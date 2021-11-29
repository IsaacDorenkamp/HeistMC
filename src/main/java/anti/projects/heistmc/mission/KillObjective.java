package anti.projects.heistmc.mission;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.EntityListener;
import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.stages.BuildWorld;
import anti.projects.heistmc.stages.HeistWorld;
import anti.projects.heistmc.ui.MenuItemListener;
import anti.projects.heistmc.ui.MobTypeMenu;
import anti.projects.heistmc.ui.OptionsMenu;
import anti.projects.heistmc.ui.OptionsMenu.OptionsMenuEntry;
import anti.projects.heistmc.ui.OptionsMenuOwner;
import net.md_5.bungee.api.ChatColor;

// TODO - Future
// 1. Configure equipment (armor, weapon). Fortunately, I have already implemented the I/O infrastructure for this.
// 2. Ability to remove entities without having to delete and recreate objectives
// 3. EXCLUDE placeholder entities from saves. Perhaps make their visibility toggle-able?
// 4. Rename individual entities
public class KillObjective extends MissionObjective implements OptionsMenuOwner {
  
  public KillObjective() {
    super("", "");
  }
  
  public static final class Entry {
    public int x;
    public int y;
    public int z;
    public String name;
    public EntityType type;
    public ItemStack[] equipment;
    
    public Entry(int x, int y, int z, String name, EntityType type) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.name = name;
      this.type = type;
      this.equipment = new ItemStack[0]; // TODO
    }
    
    public void equip(LivingEntity ent) {
      for (ItemStack is : equipment) {
        if (is == null) continue;
        Material type = is.getType();
        switch(type) {
        case LEATHER_HELMET:
        case IRON_HELMET:
        case GOLDEN_HELMET:
        case DIAMOND_HELMET:
        case CHAINMAIL_HELMET:
          ent.getEquipment().setHelmet(is);
          break;
        case LEATHER_CHESTPLATE:
        case IRON_CHESTPLATE:
        case GOLDEN_CHESTPLATE:
        case DIAMOND_CHESTPLATE:
        case CHAINMAIL_CHESTPLATE:
          ent.getEquipment().setChestplate(is);
          break;
        case LEATHER_LEGGINGS:
        case IRON_LEGGINGS:
        case GOLDEN_LEGGINGS:
        case DIAMOND_LEGGINGS:
        case CHAINMAIL_LEGGINGS:
          ent.getEquipment().setLeggings(is);
          break;
        case LEATHER_BOOTS:
        case IRON_BOOTS:
        case GOLDEN_BOOTS:
        case DIAMOND_BOOTS:
        case CHAINMAIL_BOOTS:
          ent.getEquipment().setBoots(is);
          break;
        default:
          ent.getEquipment().setItemInMainHand(is);
          break;
        }
      }
    }
  }
  
  boolean initted = false;
  private ArrayList<Entry> entities = new ArrayList<Entry>();
  
  // for use in HeistWorld
  private ArrayList<Entity> spawned = new ArrayList<Entity>();
  
  @Override
  public void initialize(HeistWorld forWorld) {
    World w = forWorld.getWorld();
    for (Entry ent : entities) {
      Entity e = forWorld.getWorld().spawnEntity(new Location(w, ent.x + 0.5, ent.y, ent.z + 0.5), ent.type);
      if (e instanceof LivingEntity) {
        LivingEntity le = (LivingEntity) e;
        if (ent.name != null) {
          le.setCustomName(ent.name);
          le.setCustomNameVisible(true);
        } else {
          le.setCustomNameVisible(false);
        }
        ent.equip(le);
      }
      spawned.add(e);
    }
    initted = true;
  }
  
  public List<Entry> getEntities() {
    return new ArrayList<Entry>(entities);
  }

  @Override
  public boolean isComplete(HeistWorld forWorld) {
    if (!initted) return false;
    for (Entity e : spawned) {
      if (!e.isDead()) return false;
    }
    return true;
  }
  
  @Override
  public boolean tryConfigAction(PlayerInteractEvent evt) { return false; }

  private EntityType toSpawn = EntityType.ZOMBIE;
  
  @Override
  public boolean tryConfigAction(PlayerInteractEvent evt, final BuildWorld bw) {
    ItemStack is = evt.getItem();
    if (is == null) return false;
    
    final Player p = evt.getPlayer();
    
    if (Globals.isNamedItem(is, Material.CREEPER_HEAD, Globals.STRING_SELECT_TYPE)) {
      // show type map
      MobTypeMenu selectType = new MobTypeMenu(p, new Consumer<EntityType>() {
        public void accept(EntityType type) {
          MessageUtil.send(p, "Setting mob type to " + ChatColor.YELLOW + MobTypeMenu.format(type));
          toSpawn = type;
          for (Entry ent : entities) {
            ent.type = type;
            bw.addPlaceholderMob(KillObjective.this, ent);
          }
          bw.selectPlaceholderMobs(KillObjective.this);
          p.closeInventory();
        }
      });
      bw.showMenu(selectType, p);
      return false;
    } else if (Globals.isNamedItem(is, Material.ZOMBIE_SPAWN_EGG, Globals.STRING_PLACE_ONE)) {
      Location spawnLoc = evt.getClickedBlock().getLocation().add(0, 1, 0);
      final Entry ent = new Entry(spawnLoc.getBlockX(), spawnLoc.getBlockY(), spawnLoc.getBlockZ(), null, toSpawn);
      if (entities.size() > 0) {
        ent.equipment = entities.get(0).equipment;
      }
      entities.add(ent);
      
      // Note: This is justified since only LivingEntity types will be available for spawn.
      bw.addPlaceholderMob(this, ent);
    } else if (Globals.isNamedItem(is, Material.WRITTEN_BOOK, Globals.STRING_FINISH)) {
      if (_cfg != null) {
        p.getInventory().removeItem(_cfg);
      }
      
      p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, Globals.UI_SOUND_VOLUME, 1f);
      MessageUtil.send(p, "Configured Kill Objective");
      
      cfgmeta();
      
      bw.selectPlaceholderMobs(null);
      
      return true;
    } else if (Globals.isNamedItem(is, Material.ANVIL, Globals.STRING_CONFIGURE_EQUIPMENT)) {
      Inventory inv = Bukkit.createInventory(p, InventoryType.HOPPER);
      // for now, all equipment is uniform (and may be forever)
      if (entities.size() > 0) {
        if (entities.get(0).equipment != null) {
          inv.setContents(entities.get(0).equipment);
        }
      }
      bw.showInventory(p, inv, new Consumer<Inventory>() {
        @Override
        public void accept(Inventory t) {
          for (Entry e : entities) {
            e.equipment = copyItemStacks(t.getContents());
          }
          bw.selectPlaceholderMobs(null);
          bw.selectPlaceholderMobs(KillObjective.this);
        }
        
      });
    }
    
    return false;
  }
  
  private static ItemStack[] copyItemStacks(ItemStack[] src) {
    ItemStack[] out = new ItemStack[src.length];
    for (int i = 0; i < src.length; i++) {
      out[i] = src[i] == null ? null : src[i].clone();
    }
    return out;
  }
  
  public void remove(KillObjective.Entry ent) {
    entities.remove(ent);
  }
  
  private void cfgmeta() {
    if (entities.size() == 0) return;
    this.name = "KILL";
    this.description = "Find and kill the " + ChatColor.RED + "" + ChatColor.BOLD + MobTypeMenu.format(entities.get(0).type) + (entities.size() > 1 ? "s" : "") ;
  }
  
  private ItemStack[] _cfg = null;

  @Override
  public boolean onStartConfig(Player p) {
    ItemStack selectType = Globals.getNamedItem(Material.CREEPER_HEAD, Globals.STRING_SELECT_TYPE);
    ItemStack spawn = Globals.getNamedItem(Material.ZOMBIE_SPAWN_EGG, Globals.STRING_PLACE_ONE);
    ItemStack cfg_eq = Globals.getNamedItem(Material.ANVIL, Globals.STRING_CONFIGURE_EQUIPMENT);
    ItemStack finish = Globals.getNamedItem(Material.WRITTEN_BOOK, Globals.STRING_FINISH);
    ItemStack remove = Globals.getNamedItem(Material.DIAMOND_SWORD, Globals.STRING_REMOVE_MOB);
    _cfg = new ItemStack[] { selectType, spawn, cfg_eq, finish, remove };
    Inventory inv = p.getInventory();
    inv.setItem(0, selectType);
    inv.setItem(1, spawn);
    inv.setItem(2, cfg_eq);
    inv.setItem(3, finish);
    inv.setItem(4, remove);
    return true;
  }

  @Override
  public Material getDisplayIcon() {
    return Material.DIAMOND_SWORD;
  }

  @Override
  public void save(DataOutputStream out) throws IOException {
    // 1. Integer for number of entities
    // 2. For each entity:
    // 3.   Name of EntityType (writeUTF)
    // 4.   Display name (writeUTF)
    // 5.   x pos (writeInt)
    // 6.   y pos (writeInt)
    // 5.   z pos (writeInt)
    // 6.   number of equipment items (writeInt)
    // 7.   Material enum values (as many as written in step 6)
    out.writeInt(entities.size());
    out.writeUTF("MATERIAL"); // will change to "ITEM" once we write items instead of just material types
    for (Entry ent : entities) {
      out.writeUTF(ent.type.toString());
      out.writeUTF(ent.name != null ? ent.name : "");
      out.writeInt(ent.x);
      out.writeInt(ent.y);
      out.writeInt(ent.z);
      
      // TODO XXX - write serialized ItemStack (as in InventoryPersist), not just the material!
      if (ent.equipment != null) {
        int items = 0;
        ArrayList<Material> materials = new ArrayList<>();
        for (ItemStack is : ent.equipment) {
          if (is == null) continue;
          items++;
          materials.add(is.getType());
        }
        out.writeInt(items);
        for (Material m : materials) {
          out.writeUTF(m.toString());
        }
      } else {
        out.writeInt(0);
      }
    }
  }

  @Override
  public void load(DataInputStream in) throws IOException {
    int amount = in.readInt();
    in.readUTF(); // Right now, the "item format type" is ignored, it will always be material for now
    for (int i = 0; i < amount; i++) {
      EntityType type = EntityType.valueOf(in.readUTF());
      String name = in.readUTF();
      if (name.isEmpty()) name = null;
      int x = in.readInt();
      int y = in.readInt();
      int z = in.readInt();
      int items = in.readInt();
      ItemStack[] is = new ItemStack[items];
      for (int j = 0; j < items; j++) {
        is[j] = new ItemStack(Material.valueOf(in.readUTF()), 1);
      }
      
      Entry ent = new Entry(x, y, z, name, type);
      ent.equipment = is;
      
      entities.add(ent);
    }
    
    cfgmeta();
  }

  @Override
  public OptionsMenu getOptionsMenu(final Player playerFor) {
    OptionsMenu menu = new OptionsMenu();
    menu.addEntry(Material.LEVER, "Show mobs for this objective", new MenuItemListener() {
      @Override
      public void onSelected() {
        BuildWorld bw = BuildWorld.getActiveInstance(playerFor);
        if (bw != null) {
          bw.selectPlaceholderMobs(KillObjective.this);
        }
      }
    });
    return menu;
  }

}
