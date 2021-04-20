package anti.projects.heistmc.stages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MapManager;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.api.ChatRoom;
import anti.projects.heistmc.api.PlayerState;
import anti.projects.heistmc.api.PlayerStateTracker;
import anti.projects.heistmc.persist.InventoryPersist;
import anti.projects.heistmc.persist.PlayerStatePersist;
import anti.projects.heistmc.ui.SidebarDisplay;

public class Lobby implements ChatRoom {
  
  private static Lobby[] lobbies = new Lobby[Globals.MAX_LOBBIES];
  private volatile boolean transferring = false;
  
  public static Lobby getLobbyForWorld(World w) {
    for (Lobby lobby : lobbies) {
      if (w.equals(lobby.getWorld())) return lobby;
    }
    return null;
  }
  
  public static void createAllLobbies(HeistMC p) {
    for (int i = 0; i < lobbies.length; i++) {
      lobbies[i] = Lobby.createInstance(p);
    }
  }
  
  public static Lobby getNextLobby(HeistMC p) {
    Lobby mostFull = null;
    int lobbiesAllocated = 0;
    for (Lobby l : lobbies) {
      if (l == null) continue;
      lobbiesAllocated++;
      if (l.isAccepting()) {
        if (mostFull == null || l.getPlayersIn() > mostFull.getPlayersIn()) {
          mostFull = l;
        }
      }
    }
    
    if (mostFull == null && lobbiesAllocated == Globals.MAX_LOBBIES) {
      // all lobbies full
      return null;
    } else if (mostFull == null) {
      // there's an un-allocated lobby!
      Lobby l = null;
      for (int i = 0; i < lobbies.length; i++) {
        if (lobbies[i] == null) {
          l = Lobby.createInstance(p);
          lobbies[i] = l;
          break;
        }
      }
      return l;
    } else {
      return mostFull;
    }
  }
  
  private ArrayList<Player> inLobby;
  private LobbyEvents evts;
  private World lobbyWorld = null;
  private HeistWorld target = null;
  private String mapId = null;
  
  private WorldManager mgr;
  private PlayerStateTracker tracker;
  private InventoryPersist persistence;
  private PlayerStatePersist ps_persistence;
  
  private SidebarDisplay display;
  
  private Lobby() {
    inLobby = new ArrayList<Player>();
    
    ScoreboardManager mgr = Bukkit.getScoreboardManager();
    Scoreboard scores = mgr.getNewScoreboard();
    
    display = new SidebarDisplay(scores, ChatColor.BOLD + "  HEISTMC LOBBY  ");
    
    display.addLine("", true);
    display.addLine(String.format("Players: %d/%d", 0, Globals.MAX_PLAYERS), true);
    display.addLine("Map: " + ChatColor.GRAY + "None", true);
    display.blit();
  }
  
  public List<Player> getPlayers() {
    return new ArrayList<Player>(inLobby);
  }
  
  private void initialize(HeistMC p) {
    Server s = p.getServer();
    MapManager maps = p.getMapManager();
    evts = new LobbyEvents(this, maps, p.getWorldManager());
    s.getPluginManager().registerEvents(evts, p);
    
    boolean generateDefault = true;
    if (maps.hasMap(Globals.ID_LOBBY)) {
      try {
        lobbyWorld = p.getWorldManager().copyFrom(maps.getMap(Globals.ID_LOBBY), false);
        generateDefault = false;
      } catch (IOException ioe) {}
    }
    
    if (generateDefault) {
      lobbyWorld = p.getWorldManager().blank(false);
      
      for (int x = -5; x < 5; x++) {
        for (int z = -5; z < 5; z++) {
          Block b = lobbyWorld.getBlockAt(x, 50, z);
          b.setType(Material.POLISHED_ANDESITE);
          if (x == -5 || z == -5 || x == 4 || z == 4) {
            Block above = lobbyWorld.getBlockAt(x, 51, z);
            above.setType(Material.OAK_FENCE);
          }
        }
      }
      
      lobbyWorld.setSpawnLocation(0, 51, 0);
    }
    
    mgr = p.getWorldManager();
    tracker = p.getStateTracker();
    persistence = p.getInventoryPersist();
    ps_persistence = p.getPlayerStatePersist();
    target = HeistWorld.createInstance(p);
  }
  
  public void deinitialize() {
    this.lobbyWorld = null;
  }
  
  public String getSelectedMap() {
    return mapId;
  }
  
  public void setSelectedMap(String map) {
    mapId = map;
    String displayName = mapId != null ? ChatColor.YELLOW + mapId : ChatColor.GRAY + "None";
    display.setLine(2, "Map: " + displayName, false);
  }
  
  public void transfer() {
    if (transferring) return;
    
    transferring = true;
    
    if (mapId == null) {
      transferring = false;
      MessageUtil.send(inLobby.get(0), ChatColor.RED + "Please select a map first.");
      return;
    }
    
    boolean success = target.initialize_map(mapId);
    if (!success) {
      MessageUtil.sendToRoom(this, ChatColor.RED + "There was an error initializing the map!");
      return;
    }
    
    boolean goToTarget = true;
    if (!target.ready()) {
      MessageUtil.sendToRoom(this, ChatColor.RED + "Failed to load heist world data; sending you back to spawn.");
      if (target.hasLoadedMap()) target.cleanup_map();
      goToTarget = false;
    }
    
    for (Player p : new ArrayList<Player>(inLobby)) {
      removePlayer(p, null, false);
      if (goToTarget) target.putPlayer(p);
      else {
        if (ps_persistence.hasEntry(p)) {
          ps_persistence.popPlayerState(p);
        }
        p.teleport(mgr.getMainWorld().getSpawnLocation());
        persistence.loadInventory(p, p.getWorld().getName());
      }
    }
    
    display.setLine(2, "Map: " + ChatColor.GRAY + "None", false);
    mapId = null;
    transferring = false;
  }
  
  public World getWorld() {
    return lobbyWorld;
  }
  
  public boolean hasPlayer(Player p) {
    return inLobby.contains(p);
  }
  
  public int getPlayersIn() {
    return inLobby.size();
  }
  
  public boolean putPlayer(Player p, String message) {
    return putPlayer(p, message, true);
  }
  
  public boolean putPlayer(Player p, String message, boolean teleportIn) {
    PlayerState state = tracker.getState(p);
    if (state.equals(PlayerState.HEIST) || state.equals((PlayerState.LOBBY)) || inLobby.size() >= Globals.MAX_PLAYERS) {
      return false;
    }
    
    boolean addItems = inLobby.size() == 0;
    
    inLobby.add(p); // IMPORTANT to add player *BEFORE* teleporting! See LobbyEvents#playerTeleport to understand why
    if (teleportIn) {
      p.setVelocity(new Vector(0, 0, 0));
      p.teleport(lobbyWorld.getSpawnLocation());
    }
    p.setGameMode(GameMode.SURVIVAL);
    tracker.setState(p, PlayerState.LOBBY);
    
    if (teleportIn) {
      if (mgr.hasWorld(p.getWorld().getName())) persistence.saveInventory(p, p.getWorld().getName());
      ps_persistence.setPlayerState(p);
    }
    
    p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
    p.setFoodLevel(20);
    p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0));
    
    p.getInventory().clear();
    p.getInventory().setItem(8, Globals.getLeaveStar());
    
    // first player in lobby has control over starting the heist
    if (addItems) {
      p.getInventory().setItem(7, Globals.getStartArrow());
      p.getInventory().setItem(6, Globals.getNamedItem(Material.PAPER, Globals.STRING_SELECT_MAP));
    }
    
    display.show(p);
    display.setLine(1, String.format("Players: %d/%d", inLobby.size(), Globals.MAX_PLAYERS), false);
    
    MessageUtil.sendToRoom(this, message);
    return true;
  }
  
  public void evacuate(String message) {
    while (inLobby.size() > 0) {
      Player p = inLobby.get(0);
      removePlayer(p, message);
    }
  }
  
  public void removePlayer(Player p, String message, boolean teleport) {
    display.unshow(p);
    
    boolean reassign_controls = false;
    if (p.getInventory().first(Globals.getNamedItem(Material.ARROW, Globals.STRING_START_HEIST)) >= 0) {
      // give start arrow and map selector to next most recently joined player
      setSelectedMap(null);
      reassign_controls = true;
    }
    
    inLobby.remove(p);
    display.setLine(1, String.format("Players: %d/%d", inLobby.size(), Globals.MAX_PLAYERS), false);
    
    if (inLobby.size() > 0 && reassign_controls) {
      PlayerInventory next = inLobby.get(0).getInventory();
      next.setItem(6, Globals.getNamedItem(Material.PAPER, Globals.STRING_SELECT_MAP));
      next.setItem(7, Globals.getNamedItem(Material.ARROW, Globals.STRING_START_HEIST));
    }
    
    if (teleport) {
      p.getInventory().clear();
      p.removePotionEffect(PotionEffectType.SATURATION);
      
      p.teleport(mgr.getMainWorld().getSpawnLocation());
      persistence.loadInventory(p, p.getWorld().getName());
      if (ps_persistence.hasEntry(p)) {
        ps_persistence.popPlayerState(p);
      }
      
      tracker.setState(p, PlayerState.ONLINE);
    }
    if (message != null) MessageUtil.sendToRoom(this, message);
  }
  
  public void removePlayer(Player p, String message) {
    removePlayer(p, message, true);
  }
  
  public static Lobby createInstance(HeistMC p) {
    Lobby lob = new Lobby();
    lob.initialize(p);
    return lob;
  }
  
  public boolean isAccepting() {
    return inLobby.size() < Globals.MAX_PLAYERS && !transferring && !target.isInProgress();
  }
}
