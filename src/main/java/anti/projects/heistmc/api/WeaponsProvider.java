package anti.projects.heistmc.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.stages.HeistWorld;
import anti.projects.heistmc.stages.HeistWorldData;

public class WeaponsProvider implements Listener {
  
  private static class Grappler {
    public boolean doEarthquake;
    public boolean doFast;
    public Grappler(boolean doEarthquake, boolean doFast) {
      this.doEarthquake = doEarthquake;
      this.doFast = doFast;
    }
  }
  
  private static class Rocket {
    public boolean doFire;
    public boolean powerExplosion;
    public Rocket(boolean doFire, boolean powerExplosion) {
      this.doFire = doFire;
      this.powerExplosion = powerExplosion;
    }
  }
  
  private ArrayList<String> worlds;

  private List<Snowball> grenades;
  private HashMap<Arrow, Rocket> rockets;
  private HashMap<Arrow, Grappler> hooks;
  
  private Location lastDetonated = null;
  private Player lastDetonator = null;
  private long lastDetonation = 0L;
  
  private HashMap<UUID, Grappler> grappling = new HashMap<>();
  
  public WeaponsProvider() {
    worlds = new ArrayList<>();
    grenades = new ArrayList<>();
    rockets = new HashMap<>();
    hooks = new HashMap<>();
  }
  
  public void registerWorld(String world) {
    worlds.add(world);
  }
  
  public void unregisterWorld(String world) {
    worlds.remove(world);
  }
  
  @EventHandler
  public void playerTeleport(PlayerTeleportEvent evt) {
    if (grappling.getOrDefault(evt.getPlayer().getUniqueId(), null) != null) {
      grappling.remove(evt.getPlayer().getUniqueId());
    }
  }
  
  @EventHandler
  public void playerMove(PlayerMoveEvent evt) {
    if (grappling.getOrDefault(evt.getPlayer().getUniqueId(), null) != null) {
      Player player = evt.getPlayer();
      Material m = player.getLocation().getBlock().getType();
      if (m.isOccluding() || m.equals(Material.WATER)) {
        grappling.remove(evt.getPlayer().getUniqueId());
      }
    }
  }
  
  @EventHandler
  public void playerDamage(EntityDamageEvent evt) {
    Entity ent = evt.getEntity();
    if (ent instanceof Player) {
      Player player = (Player)ent;
      
      Grappler grap = grappling.getOrDefault(player.getUniqueId(), null);
      if (evt.getCause().equals(DamageCause.FALL) && grap != null) {
        if (grap.doEarthquake) {
          // apply the earthquake effect
          World pw = player.getWorld();
          pw.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
          for (Entity toDamage : pw.getEntities()) {
            if (toDamage instanceof Player) continue;
            else if (toDamage instanceof Monster) {
              Monster monster = (Monster)toDamage;
              if (monster.getLocation().distance(player.getLocation()) <= 15) {
                monster.damage(5.0);
              }
            }
          }
        }
        grappling.remove(player.getUniqueId());
        evt.setDamage(0.0);
        evt.setCancelled(true);
      }
    }
  }
  
  @EventHandler
  public void projectileLaunch(ProjectileLaunchEvent evt) {
    String wn = evt.getEntity().getWorld().getName();
    if (!worlds.contains(wn)) return;
    if (evt.getEntityType().equals(EntityType.ARROW)) {
      Arrow arr = (Arrow)evt.getEntity();
      ProjectileSource src = arr.getShooter();
      if (src instanceof Player) {
        Player player = (Player)src;
        ItemStack holding = player.getInventory().getItemInMainHand();
        if (Globals.isNamedItem(holding, Material.BOW, Globals.WEAPON_ROCKET_LAUNCHER)) {
          boolean hasIncendiary = Upgrade.INCENDIARY.itemHas(holding);
          boolean hasBiggerBoom = Upgrade.BIGGER_BOOM.itemHas(holding);
          Rocket rock = new Rocket(hasIncendiary, hasBiggerBoom);
          rockets.put(arr, rock);
        } else if (Globals.isNamedItem(holding, Material.CROSSBOW, Globals.WEAPON_GRAPPLING_HOOK)) {
          boolean hasEarthquake = Upgrade.EARTHQUAKE.itemHas(holding);
          boolean hasFast = Upgrade.FAST_GRAPPLE.itemHas(holding);
          Grappler grap = new Grappler(hasEarthquake, hasFast);
          hooks.put(arr, grap);
        }
      }
    } else if (evt.getEntityType().equals(EntityType.SNOWBALL)) {
      ProjectileSource src = ((Snowball)evt.getEntity()).getShooter();
      if (src instanceof Player) {
        Player player = (Player)src;
        ItemStack holding = player.getInventory().getItemInMainHand();
        if (Globals.isNamedItem(holding, Material.SNOWBALL, Globals.WEAPON_GRENADE)) {
          grenades.add((Snowball)evt.getEntity());
        }
      }
    }
  }
  
  private static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
    return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
  }
  
  private void emulateExplosion(Location loc, int radius) {
    World world = loc.getWorld();
    HeistWorld hw = HeistWorld.getInstanceForWorld(world);
    if (hw == null) return;
    
    HeistWorldData hwd = hw.getData();
    
    double CENTER_X = loc.getBlockX() + 0.5;
    double CENTER_Y = loc.getBlockY() + 0.5;
    double CENTER_Z = loc.getBlockZ() + 0.5;
    for (int x = loc.getBlockX() - radius; x <= loc.getBlockX() + radius; x++) {
      for (int y = loc.getBlockY() - radius; y <= loc.getBlockY() + radius; y++) {
        for (int z = loc.getBlockZ() - radius; z <= loc.getBlockZ() + radius; z++) {
          boolean withinRadius = (distance(x + 0.5, y + 0.5, z + 0.5, CENTER_X, CENTER_Y, CENTER_Z) <= radius);
          BreakableBlock bb = hwd.getBreakableBlock(x, y, z);
          if (withinRadius && bb != null) {
            Block b = world.getBlockAt(x, y, z);
            b.setType(Material.AIR);
          }
        }
      }
    }
  }
  
  @EventHandler
  public void projectileHit(ProjectileHitEvent evt) {
    Entity hit = evt.getEntity();
    if (hit instanceof Arrow) {
      Arrow arr = (Arrow)hit;
      if (rockets.containsKey(arr)) {
        Location loc = arr.getLocation();
        World w = loc.getWorld();
        if (arr.getShooter() instanceof Player) {
          lastDetonated = arr.getLocation().clone();
          lastDetonator = (Player) arr.getShooter();
          lastDetonation = System.currentTimeMillis();
        }
        
        Rocket rock = rockets.get(arr);
        
        float explosionSize = rock.powerExplosion ? 6.0f : 4.0f;
        emulateExplosion(loc, (int)explosionSize);
        w.createExplosion(loc, explosionSize, rock.doFire, false);
        arr.remove();
        rockets.remove(arr);
      } else if (hooks.containsKey(arr)) {
        if (arr.getShooter() instanceof Player) {
          Grappler grap = hooks.get(arr);
          launchTowardLocation((Player)arr.getShooter(), arr.getLocation().clone(), grap);
        }
        arr.remove();
        hooks.remove(arr);
      }
    } else if (hit instanceof Snowball) {
      if (grenades.contains(hit)) {
        Location loc = hit.getLocation();
        World w = loc.getWorld();
        Snowball snow = (Snowball)hit;
        if (snow.getShooter() instanceof Player) {
          lastDetonated = snow.getLocation().clone();
          lastDetonator = (Player) snow.getShooter();
          lastDetonation = System.currentTimeMillis();
        }
        w.createExplosion(loc, 3.0f, false, false);
        emulateExplosion(loc, 2);
        hit.remove();
        grenades.remove(hit);
      }
    }
  }
  
  private void launchTowardLocation(final Player p, Location loc, Grappler grap) {
    Location ploc = p.getLocation();
    double dx = loc.getX() - ploc.getX();
    double dy = (loc.getY() - ploc.getY()) + 1;
    double dz = loc.getZ() - ploc.getZ();
    
    boolean doFast = grap.doFast;
    
    double _vx;
    double _vz;
    double _vy;
    double _times;
    if (doFast) {
      _vx = dx / 5;
      _vz = dz / 5;
      _vy = ((dy / 5) + 0.8);
      _times = 1;
    } else {
      _vx = dx / 10;
      _vz = dz / 10;
      _vy = (dy / 10) + 1.6;
      _times = 1.5;
    }
    
    final double vx = _vx;
    final double vy = _vy;
    final double vz = _vz;
    final double multiplier = _times;
    
    grappling.put(p.getUniqueId(), grap);
    p.setVelocity(new Vector(0, vy, 0));
    Bukkit.getScheduler().scheduleSyncDelayedTask(HeistMC.getInstance(), new Runnable() {
      public void run() {
        Vector vel = new Vector(vx, vy, vz);
        Vector horiz = vel.clone();
        horiz.setY(0.0);
        horiz.normalize();
        vel.add(horiz.multiply(multiplier));
        p.setVelocity(new Vector(vx, vy, vz));
      }
    }, 2);
  }
  
  public void detonate(Location where, Player who, long when) {
    lastDetonated = where.clone();
    lastDetonator = who;
    lastDetonation = when;
  }
  
  @EventHandler
  public void entityDamage(EntityDamageByEntityEvent evt) {
    Entity ent = evt.getEntity();
    if (!worlds.contains(ent.getWorld().getName())) return;
    
    Entity damager = evt.getDamager();
    if (damager instanceof LivingEntity && ent instanceof LivingEntity) {
      LivingEntity cause = (LivingEntity)damager;
      EntityEquipment eq = cause.getEquipment();
      ItemStack inMainHand = eq.getItemInMainHand();
      if (inMainHand.getType().name().endsWith("_SWORD")) {
        if (Upgrade.CURSE_OF_DECAY.itemHas(inMainHand)) {
          LivingEntity lent = (LivingEntity)ent;
          if (!lent.hasPotionEffect(PotionEffectType.WITHER)) {
            lent.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 300, 1));
          }
        }
      }
    }
  }
  
  @EventHandler
  public void entityDeath(EntityDeathEvent evt) {
    Entity ent = evt.getEntity();
    DamageCause cause = ent.getLastDamageCause().getCause();
    if (cause.equals(DamageCause.BLOCK_EXPLOSION)) {
      if (lastDetonated == null) return;
      else {
        long now = System.currentTimeMillis();
        if (ent.getLocation().getWorld().equals(lastDetonated.getWorld()) && (now - lastDetonation) <= 50L) {
          HeistPlayer hp = HeistWorld.getHeistPlayerFor(lastDetonator);
          if (hp != null) {
            hp.addMoneyFor(evt.getEntityType());
          }
        }
      }
    }
  }
}
