package anti.projects.heistmc.stages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
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
import anti.projects.heistmc.api.HeistWorldState;
import anti.projects.heistmc.api.PlayerState;
import anti.projects.heistmc.api.PlayerStateTracker;
import anti.projects.heistmc.mission.MissionObjective;
import anti.projects.heistmc.persist.InventoryPersist;
import anti.projects.heistmc.persist.PlayerStatePersist;

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
  private List<Player> inHeist;
  private HeistEvents evts;
  private boolean inProgress = false;
  private WorldManager mgr;
  private MapManager mapMgr;
  private PlayerStateTracker tracker;
  private InventoryPersist persistence;
  private PlayerStatePersist ps_persistence;
  
  // heist world state
  private HeistWorldData data = null;
  private HeistWorldState state = null;
  
  private HeistWorld() {
    inHeist = new ArrayList<Player>();
    INSTANCES.add(this);
  }
  
  @Override
  protected void finalize() {
    INSTANCES.remove(this);
  }
  
  public boolean hasPlayer(Player p) {
    return inHeist.contains(p);
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
    
    state.getCurrentObjective().initialize(this);
    
    if (map != null) {
      try {
        this.world = mgr.copyFrom(map, false);
      } catch (IOException ioe) {
        ioe.printStackTrace();
        return false;
      }
      
      return this.world != null;
    } else {
      return false;
    }
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
  
  
  public void checkNextObjective() {
    MissionObjective obj = state.getCurrentObjective();
    if (obj.isComplete(this)) {
      obj.cleanup(this);
      boolean isFinished = state.next();
      if (isFinished) {
        finish(mgr.getMainWorld().getSpawnLocation());
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
    inHeist.add(p);
    p.teleport(world.getSpawnLocation());
    tracker.setState(p, PlayerState.HEIST);
  }
  
  public void removePlayer(Player p, String message, boolean teleport) {
    inHeist.remove(p);
    if (teleport) {
      // VERY IMPORTANT to teleport *after* removing the player from the heist!
      // See HeistEvents#playerTeleport to understand why
      if (persistence.hasEntry(p)) {
        persistence.popInventory(p);
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
    while (inHeist.size() > 0) {
      removePlayer(inHeist.get(0), message);
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
  public void finish(Location to, boolean success, String subtitle) {
    // TODO - rearrange to have title first, wait time of, say, 5 seconds, then teleport.
    transferring = true;
    for (Player p : inHeist) {
      p.teleport(to);
      if (persistence.hasEntry(p)) {
        persistence.popInventory(p);
      }
      if (ps_persistence.hasEntry(p)) {
        ps_persistence.popPlayerState(p);
      }
      tracker.setState(p, PlayerState.ONLINE);
    }
    transferring = false;
    
    
    MessageUtil.roomTitle(this, success ? ChatColor.GREEN + "HEIST PASSED" : ChatColor.RED + "HEIST FAILED", subtitle);
    inHeist.clear();
    cleanup_map();
    inProgress = false;
  }
  
  public void finish(Location to) {
    finish(to, true, "");
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
    return inHeist;
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    return true;
  }
}
