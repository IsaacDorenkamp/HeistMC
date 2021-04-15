package anti.projects.heistmc.stages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import anti.projects.heistmc.EntityListener;
import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MapManager;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.api.ChatRoom;
import anti.projects.heistmc.api.PlayerState;
import anti.projects.heistmc.api.PlayerStateTracker;
import anti.projects.heistmc.mission.KillObjective;
import anti.projects.heistmc.mission.MissionObjective;
import anti.projects.heistmc.persist.InventoryPersist;
import anti.projects.heistmc.ui.MenuView;

public class BuildWorld implements ChatRoom, CommandExecutor {
  private static HashMap<UUID, BuildWorld> INSTANCES = new HashMap<UUID, BuildWorld>();
  
  public static Collection<BuildWorld> getInstances() {
    return INSTANCES.values();
  }
  
  public static BuildWorld getInstanceForWorld(World w) {
    for (BuildWorld instance : INSTANCES.values()) {
      if (instance.world.equals(w)) {
        return instance;
      }
    }
    return null;
  }

  private List<Player> building;

  private WorldManager mgr;
  private MapManager maps;
  private PlayerStateTracker tracker;

  private HeistWorldData data = new HeistWorldData();
  private ObjectiveSetTracker mTracker = new ObjectiveSetTracker(data.getObjectives(), this);
  private InventoryPersist persistence;

  private World world;

  private BuildEvents evts;
  private HashMap<Player, MenuView> menus = new HashMap<Player, MenuView>();
  
  private KillObjective selected = null;
  private HashMap<LivingEntity, KillObjective.Entry> placeholderMobs = new HashMap<>();
  private HashMap<Player, GameMode> onEnter = new HashMap<Player, GameMode>();

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
    }
  }

  public static void saveAll() {
    for (BuildWorld bw : INSTANCES.values()) {
      try {
        bw.save();
        bw.removePlaceholderMobs();
      } catch (IOException ioe) {
        if (log != null)
          log.severe("FAILED to save heist data for world " + bw.getWorld().getName());
      }
    }
  }

  public void save() throws IOException {
    KillObjective wasShowing = selected;
    // get rid of placeholder mobs
    removePlaceholderMobs();
    
    // get rid of any other living entities
    for (Entity e : world.getEntities()) {
      if (e instanceof LivingEntity && !(e instanceof Player)) {
        e.remove();
      }
    }
    
    // save world, obviously
    world.save();
    
    selectPlaceholderMobs(wasShowing);

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
    onEnter.put(p, p.getGameMode());
    
    p.setGameMode(GameMode.CREATIVE);
    
    p.getInventory().clear();
    p.getInventory().setItem(7, Globals.getMenuBook());
    p.getInventory().setItem(8, Globals.getLeaveStar());

    building.add(p);
    p.teleport(world.getSpawnLocation());
    tracker.setState(p, PlayerState.BUILD);
  }
  
  public void removePlayer(Player p) {
    building.remove(p);
    GameMode gm = onEnter.get(p);
    p.setGameMode(gm == null ? GameMode.SURVIVAL : gm);
    p.getInventory().clear();
    p.teleport(mgr.getMainWorld().getSpawnLocation());
    tracker.setState(p, PlayerState.ONLINE);
    
    if (persistence.hasEntry(p)) {
      persistence.popInventory(p);
    }
  }
  
  public void evacuate() {
    while (building.size() > 0) {
      removePlayer(building.get(0));
    }
  }
  
  public KillObjective getSelectedPlaceholders() {
    return selected;
  }
  
  public void removeEntryForPlaceholder(Entity ent) {
    if (selected != null) {
      selected.remove(placeholderMobs.get(ent));
    }
    placeholderMobs.remove(ent);
    ent.remove();
  }
  
  public boolean isPlaceholderMob(Entity ent) {
    return placeholderMobs.containsKey(ent);
  }
  
  public void addPlaceholderMob(KillObjective objFor, KillObjective.Entry source) {
    Location at = new Location(world, source.x + 0.5, source.y, source.z + 0.5);
    final LivingEntity actual = (LivingEntity)at.getWorld().spawnEntity(at, source.type);
    
    if (actual instanceof Zombie) {
      Zombie z = (Zombie)actual;
      if (z.isBaby()) z.setBaby(false);
    }
    
    actual.getEquipment().clear();
    source.equip(actual);
    
    // mostly for spider jockeys
    if (actual.getPassengers().size() > 0) {
      for (Entity e : actual.getPassengers()) {
        actual.removePassenger(e);
      }
    }
    
    actual.setGravity(false);
    if (source.name != null) {
      actual.setCustomName(source.name);
      actual.setCustomNameVisible(true);
    } else {
      actual.setCustomNameVisible(false);
    }
    actual.setInvulnerable(true);
    actual.setAI(false);
    placeholderMobs.put(actual, source);
    HeistMC.getInstance().getEntityTracker().track(actual, new EntityListener() {

      @Override
      public void blockDamage(EntityDamageByBlockEvent evt) {}

      @Override
      public void entityDamage(EntityDamageByEntityEvent evt) {}

      @Override
      public void death(EntityDeathEvent evt) {
        placeholderMobs.remove(actual);
      }
      
    });
  }
  
  private void removePlaceholderMobs() {
    for (LivingEntity ent : placeholderMobs.keySet()) {
      ent.remove();
    }
    placeholderMobs.clear();
  }
  
  public void selectPlaceholderMobs(KillObjective obj) {
    if (obj == null) {
      removePlaceholderMobs();
      selected = null;
      return;
    } else if (!obj.equals(selected)) {
      removePlaceholderMobs();
      selected = obj;
      for (KillObjective.Entry entry : obj.getEntities()) {
        addPlaceholderMob(obj, entry);
      }
    }
  }
  
  private HashMap<UUID, Consumer<Inventory>> invCbk = new HashMap<>();
  private HashMap<UUID, Inventory> showing = new HashMap<>();
  public void showInventory(Player p, Inventory inv, Consumer<Inventory> cbk) {
    if (hasPlayer(p)) {
      p.openInventory(inv);
      showing.put(p.getUniqueId(), inv);
      invCbk.put(p.getUniqueId(), cbk);
    }
  }
  
  public boolean inventoryCallback(Player p) {
    UUID puid = p.getUniqueId();
    if (invCbk.containsKey(puid) && showing.containsKey(puid)) {
      invCbk.get(puid).accept(showing.get(puid));
      invCbk.remove(puid);
      showing.remove(puid);
      return true;
    } else {
      return false;
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
    w.getWorld().setSpawnFlags(false, false);
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
