package anti.projects.heistmc.stages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MapManager;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.api.ChatRoom;
import anti.projects.heistmc.api.HeistPlayer;
import anti.projects.heistmc.api.HeistWorldState;
import anti.projects.heistmc.api.PlayerState;
import anti.projects.heistmc.api.PlayerStateTracker;
import anti.projects.heistmc.mission.MissionObjective;
import anti.projects.heistmc.persist.InventoryPersist;
import anti.projects.heistmc.persist.PlayerStatePersist;
import anti.projects.heistmc.ui.ConfirmView;
import anti.projects.heistmc.ui.InteractiveView;
import anti.projects.heistmc.ui.UpgradeView;

public class HeistWorld implements ChatRoom, CommandExecutor {
  private static ArrayList<HeistWorld> INSTANCES = new ArrayList<HeistWorld>();
  
  public static HeistWorld getInstanceForWorld(World w) {
    for (HeistWorld instance : INSTANCES) {
      if (w.equals(instance.world)) {
        return instance;
      }
    }
    return null;
  }
  
  public static HeistWorld getInstanceForPlayer(Player p) {
    for (HeistWorld hw : INSTANCES) {
      if (hw.hasPlayer(p)) {
        return hw;
      }
    }
    return null;
  }
  
  private World world;
  private HashMap<UUID, HeistPlayer> inHeist;
  private HeistEvents evts;
  private boolean inProgress = false;
  private WorldManager mgr;
  private MapManager mapMgr;
  private PlayerStateTracker tracker;
  private InventoryPersist persistence;
  private PlayerStatePersist ps_persistence;
  
  private HashMap<UUID, InteractiveView> views = new HashMap<>();
  
  // heist world state
  private HeistWorldData data = null;
  private HeistWorldState state = null;
  
  private HeistWorld() {
    inHeist = new HashMap<>();
    INSTANCES.add(this);
  }
  
  @Override
  protected void finalize() {
    INSTANCES.remove(this);
  }
  
  public boolean hasPlayer(Player p) {
    return inHeist.containsKey(p.getUniqueId());
  }
  
  public void showView(Player p, InteractiveView view) {
    p.openInventory(view);
    views.put(p.getUniqueId(), view);
  }
  
  public void revokeView(Player p) {
    views.remove(p.getUniqueId());
  }
  
  public InteractiveView getView(Player p) {
    return views.get(p.getUniqueId());
  }
  
  private void initialize(HeistMC p) {
    mgr = p.getWorldManager();
    mapMgr = p.getMapManager();
    tracker = p.getStateTracker();
    persistence = p.getInventoryPersist();
    ps_persistence = p.getPlayerStatePersist();
    evts = new HeistEvents(this, mgr);
    this.world = null;
    
    Server server = p.getServer();
    server.getPluginManager().registerEvents(evts, p);
  }
  
  public boolean initialize_map(String mapName) {
    File map = mapMgr.getMap(mapName);
    File heistDataSrc = new File(map, Globals.HEIST_WORLD_DATA_FILE);
    try {
      data = HeistWorldData.load(new FileInputStream(heistDataSrc));
    } catch(IOException ioe) {
      data = null;
      return false;
    }
    
    state = new HeistWorldState(data);
    if (state.getCurrentObjective() == null) {
      // world without objectives should break and be ignored
      return false;
    }
    
    boolean success;
    if (map != null) {
      try {
        this.world = mgr.copyFrom(map, false);
        success = this.world != null;
      } catch (IOException ioe) {
        ioe.printStackTrace();
        success = false;
      }
    } else {
      success = false;
    }
    
    state.getCurrentObjective().initialize(this);
    
    return success;
  }
  
  public void cleanup_map() {
    if (world == null) {
      throw new IllegalStateException("The world is not initialized!");
    }
    mgr.deleteAndRemove(this.world);
  }
  
  public boolean hasLoadedMap() {
    return world != null;
  }
  
  public boolean ready() {
    return world != null && data != null;
  }
  
  public static HeistPlayer getHeistPlayerFor(Player p) {
    for (HeistWorld hw : INSTANCES) {
      HeistPlayer hp = hw.getHeistPlayer(p);
      if (hp != null) {
        return hp;
      }
    }
    return null;
  }
  
  public HeistPlayer getHeistPlayer(Player p) {
    if (p == null) return null;
    return inHeist.get(p.getUniqueId());
  }
  
  public void checkNextObjective() {
    MissionObjective obj = state.getCurrentObjective();
    if (obj == null) return;
    if (obj.isComplete(this)) {
      obj.cleanup(this);
      boolean isFinished = state.next();
      if (isFinished) {
        finish(mgr.getMainWorld().getSpawnLocation(), true);
        return;
      } else {
        // change individual displays!
        MissionObjective newCur = state.getCurrentObjective();
        newCur.initialize(this);
        MessageUtil.sendToRoom(this, String.format("OBJECTIVE: %s", ChatColor.YELLOW + "" + ChatColor.BOLD + newCur.getDescription()));
        MessageUtil.roomTitle(this, String.format("OBJECTIVE: %s", newCur.getName()), ChatColor.YELLOW + newCur.getDescription());
      }
      checkNextObjective(); // preemptively check if next objective is already fulfilled
    }
  }
  
  public void putPlayer(Player p) {
    if (world == null) {
      throw new IllegalStateException("Heist world map not initialized!");
    }

    MissionObjective obj = state.getCurrentObjective();
    MessageUtil.send(p, String.format("OBJECTIVE: %s", ChatColor.YELLOW + "" + ChatColor.BOLD + obj.getDescription()));
    MessageUtil.title(p, String.format("OBJECTIVE: %s", obj.getName()), ChatColor.YELLOW + obj.getDescription());
    
    inProgress = true;
    HeistPlayer hp = new HeistPlayer(p);
    hp.setup();
    inHeist.put(p.getUniqueId(), hp);
    p.teleport(world.getSpawnLocation());
    tracker.setState(p, PlayerState.HEIST);
  }
  
  public void removePlayer(Player p, String message, boolean teleport) {
    HeistPlayer hp = getHeistPlayer(p);
    inHeist.remove(p.getUniqueId());
    if (hp != null) hp.cleanup();
    if (teleport) {
      // VERY IMPORTANT to teleport *after* removing the player from the heist!
      // See HeistEvents#playerTeleport to understand why
      boolean loaded = persistence.loadInventory(p, mgr.getMainWorld().getName());
      if (!loaded) {
        p.getInventory().clear();
      }
      if (ps_persistence.hasEntry(p)) {
        ps_persistence.popPlayerState(p);
      }
      p.teleport(mgr.getMainWorld().getSpawnLocation());
      tracker.setState(p, PlayerState.ONLINE);
    }
    MessageUtil.sendToRoom(this, message);
    
    if (inHeist.size() == 0) {
      cleanup_map();
      inProgress = false;
    }
  }
  
  public void evacuate(String message) {
    Object[] all = inHeist.keySet().toArray();
    for (Object _uuid : all) {
      UUID uuid = (UUID)_uuid;
      HeistPlayer hp = inHeist.get(uuid);
      removePlayer(hp.getPlayer(), message);
    }
  }
  
  public void removePlayer(Player p, String message) {
    removePlayer(p, message, true);
  }
  
  public World getWorld() {
    return world;
  }
  
  public HeistWorldData getData() {
    return data;
  }
  
  public boolean isTransferring() {
    return transferring;
  }
  
  private volatile boolean transferring;
  private Runnable afterTitle = null;
  public void finish(final Location to, boolean success, String subtitle, boolean delay) {
    // TODO - rearrange to have title first, wait time of, say, 5 seconds, then teleport.
    MessageUtil.roomTitle(this, success ? ChatColor.GREEN + "HEIST PASSED" : ChatColor.RED + "HEIST FAILED", subtitle);
    
    afterTitle = new Runnable() {
      public void run() {
        transferring = true;
        for (HeistPlayer hp : inHeist.values()) {
          hp.cleanup();
          Player p = hp.getPlayer();
          p.teleport(to);
          boolean loaded = persistence.loadInventory(p, to.getWorld().getName());
          if (!loaded) p.getInventory().clear();
          if (ps_persistence.hasEntry(p)) {
            ps_persistence.popPlayerState(p);
          }
          tracker.setState(p, PlayerState.ONLINE);
        }
        transferring = false;
        
        
        inHeist.clear();
        cleanup_map();
        inProgress = false;
        afterTitle = null;
      }
    };
    
    if (delay) {
      HeistMC.scheduleDelayedTask(afterTitle, 70);
    } else {
      afterTitle.run();
    }
  }
  
  public void finish(Location to, boolean delay) {
    finish(to, true, "", delay);
  }
  
  public void finish(Location to) {
    finish(to, false);
  }
  
  public boolean isInProgress() {
    return inProgress;
  }
  
  public static HeistWorld createInstance(HeistMC m) {
    HeistWorld world = new HeistWorld();
    world.initialize(m);
    return world;
  }

  public List<Player> getPlayers() {
    Object[] all = inHeist.keySet().toArray();
    Player[] p = new Player[all.length];
    int idx = 0;
    for (Object _uuid : all) {
      UUID uuid = (UUID)_uuid;
      p[idx++] = inHeist.get(uuid).getPlayer();
    }
    return Arrays.asList(p);
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    return true;
  }
}
