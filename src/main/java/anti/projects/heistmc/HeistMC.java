package anti.projects.heistmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import anti.projects.heistmc.api.HeistPlayer;
import anti.projects.heistmc.api.InternalPermissions;
import anti.projects.heistmc.api.PlayerState;
import anti.projects.heistmc.api.PlayerStateTracker;
import anti.projects.heistmc.api.WeaponsProvider;
import anti.projects.heistmc.persist.InventoryPersist;
import anti.projects.heistmc.persist.PlayerStatePersist;
import anti.projects.heistmc.stages.BuildWorld;
import anti.projects.heistmc.stages.HeistWorld;
import anti.projects.heistmc.stages.Lobby;
import anti.projects.heistmc.ui.BuildWorldSelector;

public class HeistMC extends JavaPlugin {
  public static Scoreboard BLANK;
  
  private static HeistMC INSTANCE = null;
  
  public static HeistMC getInstance() {
    return INSTANCE;
  }
  
  public static InternalPermissions getPermissions() {
    return INSTANCE.permissions;
  }
  
  public static void runTask(final Runnable r) {
    BukkitRunnable toRun = new BukkitRunnable() {
      public void run() {
        r.run();
      }
    };
    toRun.runTask(INSTANCE);
  }
  
  public static int scheduleRepeatedTask(final Runnable r, long period) {
    return Bukkit.getScheduler().scheduleSyncRepeatingTask(INSTANCE, r, 0L, period);
  }
  
  public static int scheduleDelayedTask(final Runnable r, long delay) {
    return Bukkit.getScheduler().scheduleSyncDelayedTask(INSTANCE, r, delay);
  }
  
  public static void descheduleRepeatedTask(int taskId) {
    Bukkit.getScheduler().cancelTask(taskId);
  }
  
  Logger log;
  
  private Server server;
  private WorldManager worldMgr;
  private MapManager mapMgr;
  private FileConfiguration commands;
  private InternalPermissions permissions;
  
  private GlobalEvents evts;
  private PlayerStateTracker tracker;
  private EntityTracker entities;
  private WeaponsProvider provider;
  
  private InventoryPersist invPersist;
  private PlayerStatePersist psPersist;
  
  private BuildWorldSelector selector = new BuildWorldSelector();
  
  @Override
  public void onEnable() {
    HeistMC.INSTANCE = this;
    
    BLANK = Bukkit.getScoreboardManager().getNewScoreboard();
    
    server = getServer();
    MessageUtil.initialize(server);
    
    worldMgr = new WorldManager(server);
    
    File data = getDataFolder();
    if (!data.exists()) {
      data.mkdirs();
    }
    
    File maps = new File(data, "maps");
    if (!maps.exists()) {
      maps.mkdir();
    }
    
    try {
      mapMgr = new MapManager(maps);
    } catch (IOException ioe) {
      throw new UnsupportedOperationException("Could not initialize map manager; aborting.");
    }
    
    File configfile = new File(data, "config.yml");
    if (!configfile.exists()) {
      try {
        InputStream is = getResource("config.yml");
        FileOutputStream fos = new FileOutputStream(configfile);
        int _byte;
        while ((_byte = is.read()) != -1) {
          fos.write(_byte);
        }
        fos.close();
        is.close();
      } catch (IOException ioe) {
        log.severe("Could not copy config.yml to data folder.");
      }
    }
    
    commands = getConfig();
    
    File permFile =  new File(data, Globals.PERMISSIONS_FILE);
    try {
      if (permFile.exists()) permissions = InternalPermissions.load(permFile);
      else permissions = new InternalPermissions();
    } catch (IOException ioe) {
      log.severe("Could not read permissions file. Using blank permissions model.");
      permissions = new InternalPermissions();
    }
    
    tracker = new PlayerStateTracker();
    server.getPluginManager().registerEvents(tracker, this);
    
    entities = new EntityTracker();
    server.getPluginManager().registerEvents(entities, this);
    server.getPluginManager().registerEvents(selector, this);
    
    provider = new WeaponsProvider();
    server.getPluginManager().registerEvents(provider, this);
    
    worldMgr.onLoad(new Consumer<World>() {
      public void accept(World w) {
        provider.registerWorld(w.getName());
      }
    });
    
    worldMgr.onUnload(new Consumer<World>() {
      public void accept(World w) {
        provider.unregisterWorld(w.getName());
      }
    });
    
    log = server.getLogger();
    
    try {
      invPersist = InventoryPersist.load(new File(data, Globals.INVENTORY_PERSIST_FILE));
    } catch (IllegalArgumentException iae) {
      log.severe("Inventory persist file is corrupted; using blank inventory persistance handler.");
    } catch (FileNotFoundException fnfe) {
      log.info("No inventory persist file, using blank inventory persistance handler. This is not an error - it is expected behavior on the first startup or after a data file purge.");
    } catch (IOException ioe) {
      ioe.printStackTrace();
      log.severe("Failed to read inventory persist file; using blank inventory persistance handler.");
    } finally {
      if (invPersist == null) {
        invPersist = new InventoryPersist();
      }
    }
    
    try {
      psPersist = PlayerStatePersist.load(new File(data, Globals.PLAYER_STATE_PERSIST_FILE));
    } catch (FileNotFoundException fnfe) {
      log.info("No player state persist file, using fresh handler");
    } catch (IOException ioe) {
      log.severe("Failed to read player state file!");
    } catch (IllegalArgumentException iae) {
      log.severe("Player state file seems to be corrupted, using blank handler");
    } finally {
      if (psPersist == null) {
        psPersist = new PlayerStatePersist();
      }
    }
    
    evts = new GlobalEvents(worldMgr, invPersist, psPersist);
    server.getPluginManager().registerEvents(evts, this);
  }
  
  @Override
  public void onDisable() {
    try {
      Class.forName("anti.projects.heistmc.stages.BuildWorld");
      for (BuildWorld bw : BuildWorld.getInstances()) {
        bw.evacuate();
      }
      BuildWorld.saveAll();
    } catch(ClassNotFoundException cnfe) {
      log.warning("Build worlds not initialized; ignoring");
    }
    
    try {
      permissions.save(new File(getDataFolder(), Globals.PERMISSIONS_FILE));
    } catch (IOException ioe) {
      log.severe("FAILED TO SAVE PERMISSIONS FILE - permissions will be reset on next startup!");
    }

    // unload and delete non-persistent worlds
    worldMgr.purge();
    try {
      invPersist.save(new File(getDataFolder(), Globals.INVENTORY_PERSIST_FILE));
    } catch (IOException ioe) {
      log.severe("FAILED TO SAVE INVENTORY PERSIST FILE - players will lose their inventory data!");
    }
    
    try {
      psPersist.save(new File(getDataFolder(), Globals.PLAYER_STATE_PERSIST_FILE));
    } catch (IOException ioe) {
      log.severe("FAILED TO SAVE PLAYER STATE PERSIST FILE - players may notice unexpected health/food levels");
    }
  }
  
  public InventoryPersist getInventoryPersist() {
    return invPersist;
  }
  
  public PlayerStatePersist getPlayerStatePersist() {
    return psPersist;
  }
  
  public WorldManager getWorldManager() {
    return worldMgr;
  }
  
  public MapManager getMapManager() {
    return mapMgr;
  }
  
  public PlayerStateTracker getStateTracker() {
    return tracker;
  }
  
  public EntityTracker getEntityTracker() {
    return entities;
  }
  
  public WeaponsProvider getWeaponsProvider() {
    return provider;
  }
  
  @Override
  public boolean onCommand(CommandSender sender, Command cmdObj, String cmdLabel, String[] args) {
    String cmd = cmdObj.getName();
    
    String permission = commands.getString(String.format("commands.%s", cmd), null);
    System.out.println("PATH: " + commands.getConfigurationSection(String.format("commands", cmd)).getValues(true));
    System.out.println(commands.getConfigurationSection("commands").getValues(true));
    System.out.println("PERMISSION: " + permission);
    if (permission != null && sender instanceof Player) {
      Player pl = (Player)sender;
      if (!permissions.hasPermission(pl, permission)) {
        MessageUtil.send(pl, ChatColor.RED + "You don't have permission to do that.");
        return true;
      }
    }
    
    if (cmd.equals("dbg")) {
      if (args.length != 1 || !(sender instanceof Player)) {
        MessageUtil.send(sender, "Invalid debug command.");
        return false;
      }
      
      Player player = (Player)sender;
      
      String debugCmd = args[0];
      if (debugCmd.equals("reset")) {
        MessageUtil.send(player, "debug - sending to main world spawn.");
        player.teleport(worldMgr.getMainWorld().getSpawnLocation());
        return true;
      } else {
        MessageUtil.send(sender, "Invalid debug command.");
        return false;
      }
    } else if (cmd.equals("join")) {
      Player p = null;
      if (sender instanceof Player) {
        p = (Player)sender;
      } else {
        if (args.length == 1) {
          try {
            p = server.getPlayer(UUID.fromString(args[0]));
          } catch (IllegalArgumentException iae) {
            p = server.getPlayer(args[0]);
          }
        }
      }
      
      if (p == null) {
        MessageUtil.send(sender, "Cannot identify player.");
        return true; // ignore
      }
      
      if (!tracker.getState(p).equals(PlayerState.ONLINE)) {
        MessageUtil.send(sender, "Please finish your current activity before attempting to join a lobby.");
        return true;
      }
      
      Lobby l = Lobby.getNextLobby(this);
      if (l == null) {
        MessageUtil.send(sender, ChatColor.RED + "No lobbies are available at this time.");
        return true;
      } else {
        if (!l.putPlayer(p, ChatColor.GREEN + "Player " + ChatColor.YELLOW + p.getDisplayName() +
            ChatColor.GREEN + " has joined.")) {
          MessageUtil.send(sender, ChatColor.RED + "You can't join a lobby right now!");
        }
        return true;
      }
    } else if (cmd.equals("leave")) {
      if (!(sender instanceof Player)) {
        return false;
      } else {
        Player p = (Player)sender;
        PlayerState state = tracker.getState(p);
        if (state.equals(PlayerState.HEIST) || state.equals(PlayerState.LOBBY) || state.equals(PlayerState.BUILD)) {
          p.teleport(worldMgr.getMainWorld().getSpawnLocation());
          tracker.setState(p, PlayerState.ONLINE);
        } else {
          MessageUtil.send(p, "You are not in a lobby or heist!");
        }
        return true;
      }
    } else if (cmd.equals("build")) {
      if (!(sender instanceof Player)) {
        return false;
      } else {
        final Player p = (Player)sender;
        if (tracker.getState(p).equals(PlayerState.BUILD)) {
          MessageUtil.send(p, ChatColor.RED + "You're already building!");
          return true;
        }
        Consumer<Integer> onSlotSelect = new Consumer<Integer>() {
          public void accept(Integer slot) {
            BuildWorld bw = BuildWorld.getInstanceFor(HeistMC.this, p, slot);
            if (bw == null) {
              MessageUtil.send(p, "You don't have permission to build HeistMC maps.");
            } else {
              bw.putPlayer(p);
              MessageUtil.send(p, "Welcome to the HeistMC map builder!");
            }
          }
        };
        selector.show(p, onSlotSelect);
        return true;
      }
    } else if (cmd.equals("inv-reload")) {
      try {
        invPersist.save(new File(Globals.INVENTORY_PERSIST_FILE));
      } catch(Exception e) {
        MessageUtil.send(sender, ChatColor.RED + "Reload failed.");
        return true;
      }
      try {
        invPersist = InventoryPersist.load(new File(Globals.INVENTORY_PERSIST_FILE));
      } catch(Exception e) {
        MessageUtil.send(sender, ChatColor.RED + "Reload failed.");
        return true;
      }
      MessageUtil.send(sender, ChatColor.GREEN + "Reload succeeded.");
      return true;
    } else if (cmd.equals("inv-push")) {
      if (sender instanceof Player) {
        Player player = (Player)sender;
        invPersist.saveInventory(player, player.getWorld().getName());
        MessageUtil.send(sender, "Inventory pushed.");
      }
      return true;
    } else if (cmd.equals("inv-pop")) {
      if (sender instanceof Player) {
        Player player = (Player)sender;
        invPersist.loadInventory(player, player.getWorld().getName());
      }
      return true;
    } else if (cmd.startsWith("set-")) {
      if (args.length != 1) {
        return false;
      }
      @SuppressWarnings("deprecation")
      OfflinePlayer op = server.getOfflinePlayer(args[0]);
      String perm = "heistmc." + cmd.split("-")[1];
      if (permissions.hasPermission(op, perm)) {
        MessageUtil.send(sender, "Player already has that permission!");
      } else {
        permissions.grantPermission(op, perm);
        MessageUtil.send(sender, "Granted permission " + ChatColor.YELLOW + cmd.split("-")[1] + ChatColor.RESET + " to player "
            + op.getName());
      }
      return true;
    } else if (cmd.startsWith("revoke-")) {
      if (args.length != 1) {
        return false;
      }
      @SuppressWarnings("deprecation")
      OfflinePlayer op = server.getOfflinePlayer(args[0]);
      String perm = "heistmc." + cmd.split("-")[1];
      if (!permissions.hasPermission(op, perm)) {
        MessageUtil.send(sender, "Player doesn't have that permission!");
      } else {
        boolean revoked = permissions.revokePermission(op, perm);
        if (revoked) MessageUtil.send(sender, "Revoked permission " + ChatColor.YELLOW + cmd.split("-")[1] + ChatColor.RESET +
            " from player " + op.getName());
        else {
          MessageUtil.send(sender, ChatColor.RED + "Cannot revoke that permission! Is that player an op?");
        }
      }
      return true;
    } else if (cmd.equals("money")) {
      if (args.length != 1) return false;
      if (!(sender instanceof Player)) {
        MessageUtil.send(sender, "Only developer *players* can use this command!");
        return true;
      }
      
      double amount;
      try {
        amount = Double.parseDouble(args[0]);
      } catch (NumberFormatException nfe) {
        return false;
      }
      
      Player player = (Player)sender;
      HeistWorld hw = HeistWorld.getInstanceForPlayer(player);
      if (hw != null) {
        HeistPlayer hp = hw.getHeistPlayer(player);
        if (hp != null) {
          hp.setMoney(hp.getMoney() + amount);
        } else {
          MessageUtil.send(player, "An unknown error occurred - your HeistPlayer instance could not be found.");
        }
      } else {
        MessageUtil.send(player, "You are not in a heist; you cannot gain money.");
      }
      return true;
    } else if (sender instanceof Player) {
      Player p = (Player)sender;
      HeistWorld instanceForPlayer = HeistWorld.getInstanceForPlayer(p);
      if (BuildWorld.hasActiveInstance(p)) {
        BuildWorld instance = BuildWorld.getActiveInstance(p);
        return instance.onCommand(sender, cmdObj, cmdLabel, args);
      } else if (instanceForPlayer != null) {
        return instanceForPlayer.onCommand(p, cmdObj, cmdLabel, args);
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
}
