package anti.projects.heistmc.stages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MapManager;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.api.ChatRoom;
import anti.projects.heistmc.api.PlayerState;
import anti.projects.heistmc.api.PlayerStateTracker;
import anti.projects.heistmc.persist.InventoryPersist;
import anti.projects.heistmc.ui.MenuView;

public class BuildWorld implements ChatRoom, CommandExecutor {
  private static HashMap<UUID, BuildWorld> INSTANCES = new HashMap<UUID, BuildWorld>();

  private List<Player> building;

  private WorldManager mgr;
  private MapManager maps;
  private PlayerStateTracker tracker;

  private HeistWorldData data = new HeistWorldData();
  private ObjectiveSetTracker mTracker = new ObjectiveSetTracker(data.getObjectives());
  private InventoryPersist persistence;

  private World world;

  private BuildEvents evts;
  private HashMap<Player, MenuView> menus = new HashMap<Player, MenuView>();

  private static Logger log = null;

  private BuildWorld(PlayerStateTracker tracker, WorldManager mgr, MapManager maps, InventoryPersist persist, Player p) {
    this.tracker = tracker;
    this.mgr = mgr;
    this.maps = maps;
    world = mgr.getOrBlank(String.format("build_" + p.getUniqueId().toString()), true);
    persistence = persist;
    building = new ArrayList<Player>();
  }

  private void initialize(HeistMC m) {
    evts = new BuildEvents(this, mgr);
    m.getServer().getPluginManager().registerEvents(evts, m);

    if (log == null) {
      log = m.getLogger();
    }

    File loc = new File(world.getWorldFolder(), Globals.HEIST_WORLD_DATA_FILE);
    if (loc.exists()) {
      FileInputStream fis;
      try {
        fis = new FileInputStream(loc);
      } catch (FileNotFoundException e) {
        /* won't happened, checked for this already */
        m.getServer().getLogger().severe("Error: Heist world data not found despite being present!");
        return;
      }
      try {
        HeistWorldData.load(fis, data);
      } catch (IOException e) {
        m.getServer().getLogger().severe("ERROR while loading Heist world data: " + e.toString());
      }

      System.out.println("LOADED HEIST WORLD DATA: " + data.toString());
    }
  }

  public static void saveAll() {
    for (BuildWorld bw : INSTANCES.values()) {
      try {
        bw.save();
      } catch (IOException ioe) {
        if (log != null)
          log.severe("FAILED to save heist data for world " + bw.getWorld().getName());
      }
    }
  }

  public void save() throws IOException {
    // save world, obviously
    world.save();

    // save heist world data
    File dir = world.getWorldFolder();

    File heistData = new File(dir, Globals.HEIST_WORLD_DATA_FILE);
    if (!heistData.exists()) {
      heistData.createNewFile();
    }
    FileOutputStream fos = new FileOutputStream(heistData);
    data.save(fos);
  }

  public HeistWorldData getHeistWorldData() {
    return data;
  }

  public ObjectiveSetTracker getMissionObjectiveTracker() {
    return mTracker;
  }

  public World getWorld() {
    return world;
  }

  public MenuView getMenu(Player p) {
    return menus.get(p);
  }

  public void showMenu(MenuView mv, Player p) {
    this.menus.put(p, mv);
    mv.onShow();
    p.openInventory(mv.getInventoryView());
  }

  public void menuRevoked(Player p) {
    this.menus.remove(p);
  }

  public void putPlayer(Player p) {
    if (building.contains(p))
      return;

    persistence.pushInventory(p);
    p.setGameMode(GameMode.SURVIVAL); // has to be in survival mode to prevent wacky stuff from happening with GUI
                                      // items :/
    p.getInventory().clear();
    p.getInventory().setItem(6, Globals.getNamedItem(Material.COMPASS, Globals.STRING_SET_SPAWN));
    p.getInventory().setItem(7, Globals.getMenuBook());
    p.getInventory().setItem(8, Globals.getLeaveStar());
    p.setAllowFlight(true);

    p.setFlying(true);

    building.add(p);
    p.teleport(world.getSpawnLocation());
    tracker.setState(p, PlayerState.BUILD);
  }

  public void removePlayer(Player p) {
    building.remove(p);
    p.getInventory().clear();
    p.setAllowFlight(false);
    p.teleport(mgr.getMainWorld().getSpawnLocation());
    tracker.setState(p, PlayerState.ONLINE);
    
    if (persistence.hasEntry(p)) {
      persistence.popInventory(p);
    }
  }

  public List<Player> getPlayers() {
    return building;
  }

  public boolean hasPlayer(Player p) {
    return building.contains(p);
  }

  public static boolean hasActiveInstance(Player p) {
    if (!p.hasPermission(Globals.PERMISSION_BUILD)) {
      return false;
    }

    BuildWorld instance = INSTANCES.get(p.getUniqueId());
    if (instance == null) {
      return false;
    } else {
      return instance.hasPlayer(p);
    }
  }

  public static BuildWorld getInstanceFor(HeistMC m, Player p) {
    if (!p.hasPermission(Globals.PERMISSION_BUILD)) {
      return null;
    }

    // creating instances for the same player will
    // cause conflict with the event listeners. rather
    // than unregistering and reregistering the listeners,
    // just keep track of the BuildWorld instances for each
    // unique player and return the already constructed
    // instance the second, third, fourth time et. al.
    UUID uid = p.getUniqueId();
    if (INSTANCES.containsKey(uid)) {
      return INSTANCES.get(uid);
    }

    BuildWorld w = new BuildWorld(m.getStateTracker(), m.getWorldManager(), m.getMapManager(), m.getInventoryPersist(), p);
    w.initialize(m);
    INSTANCES.put(uid, w);
    return w;
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // sender is guaranteed to be player, since this method is only invoked by
    // HeistMC on a player
    // who has an active build world (a build world they are actively building in)
    String cmd = command.getName();

    if (cmd.equals("export")) {
      if (args.length == 0) {
        MessageUtil.send(sender, "Please provide a name for your map.");
        return false;
      }
      String name = String.join(" ", args).trim();
      if (name.equals(Globals.ID_LOBBY)) {
        MessageUtil.send(sender,
            ChatColor.RED + String.format("'%s' is a reserved name, please export to a different name!"));
        return true;
      }
      try {
        // save world data first so filesystem world reflects all changes
        save();
        boolean result = maps.exportMap(name, world.getWorldFolder(), true);
        if (!result) {
          MessageUtil.send(sender, ChatColor.RED + "Could not copy world data.");
        } else {
          MessageUtil.send(sender, ChatColor.GREEN + "Map exported.");
        }
      } catch (IOException ioe) {
        MessageUtil.send(sender, ChatColor.RED + "Could not copy world data.");
      }
      return true;
    } else if (cmd.equals("export-lobby")) {
      try {
        System.out.println("Spawn location: " + world.getSpawnLocation().toString());
        save();
        boolean result = maps.exportMap(Globals.ID_LOBBY, world.getWorldFolder(), true);
        if (!result) {
          MessageUtil.send(sender, ChatColor.RED + "Could not copy world data.");
        } else {
          MessageUtil.send(sender, ChatColor.GREEN + "Map exported as lobby.");
        }
      } catch (IOException ioe) {
        MessageUtil.send(sender, ChatColor.RED + "Could not copy world data.");
      }
      return true;
    } else {
      return false;
    }
  }
}
