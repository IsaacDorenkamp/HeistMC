package anti.projects.heistmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import anti.projects.heistmc.api.PlayerState;
import anti.projects.heistmc.api.PlayerStateTracker;
import anti.projects.heistmc.persist.InventoryPersist;
import anti.projects.heistmc.stages.BuildWorld;
import anti.projects.heistmc.stages.HeistWorld;
import anti.projects.heistmc.stages.Lobby;

public class HeistMC extends JavaPlugin {
  public static Scoreboard BLANK;
  
  private static HeistMC INSTANCE = null;
  
  public static HeistMC getInstance() {
    return INSTANCE;
  }
  
  public static void runTask(final Runnable r) {
    BukkitRunnable toRun = new BukkitRunnable() {
      public void run() {
        r.run();
      }
    };
    toRun.runTask(INSTANCE);
  }
  
  Logger log;
  
  private Server server;
  private WorldManager worldMgr;
  private MapManager mapMgr;
  
  private GlobalEvents evts;
  private PlayerStateTracker tracker;
  
  private InventoryPersist invPersist;
  
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
    
    tracker = new PlayerStateTracker();
    server.getPluginManager().registerEvents(tracker, this);
    
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
    
    evts = new GlobalEvents(worldMgr, invPersist);
    server.getPluginManager().registerEvents(evts, this);
  }
  
  @Override
  public void onDisable() {
    worldMgr.purge();
    try {
      invPersist.save(new File(getDataFolder(), Globals.INVENTORY_PERSIST_FILE));
    } catch (IOException ioe) {
      log.severe("FAILED TO SAVE INVENTORY PERSIST FILE - players will lose their inventory data!");
    }
    
    // unload and delete non-persistent worlds
    // it is important that this is done last
    // so that if it fails (due to a ClassNotFoundException),
    // it won't prevent the other of these from running
    BuildWorld.saveAll();
  }
  
  public InventoryPersist getInventoryPersist() {
    return invPersist;
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
  
  @Override
  public boolean onCommand(CommandSender sender, Command cmdObj, String cmdLabel, String[] args) {
    String cmd = cmdObj.getName();
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
        if (args.length == 1) p = server.getPlayer(args[0]);
      }
      
      if (p == null) {
        MessageUtil.send(sender, "Cannot identify player.");
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
        Player p = (Player)sender;
        if (tracker.getState(p).equals(PlayerState.BUILD)) {
          MessageUtil.send(p, ChatColor.RED + "You're already building!");
          return true;
        }
        BuildWorld bw = BuildWorld.getInstanceFor(this, p);
        if (bw == null) {
          MessageUtil.send(p, "You don't have permission to build HeistMC maps.");
        } else {
          bw.putPlayer(p);
          MessageUtil.send(p, "Welcome to the HeistMC map builder!");
        }
        return true;
      }
    } else if (sender instanceof Player) {
      Player p = (Player)sender;
      HeistWorld instanceForPlayer = HeistWorld.getInstanceForPlayer(p);
      if (BuildWorld.hasActiveInstance(p)) {
        BuildWorld instance = BuildWorld.getInstanceFor(this, p);
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
