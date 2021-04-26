package anti.projects.heistmc.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.stages.HeistWorld;
import anti.projects.heistmc.stages.HeistWorldData;

public class WeaponsProvider implements Listener {
  private ArrayList<String> worlds;
  
  private List<Arrow> rockets;
  private List<Snowball> grenades;
  private List<Arrow> hooks;
  
  private Location lastDetonated = null;
  private Player lastDetonator = null;
  private long lastDetonation = 0L;
  
  private HashMap<UUID, Boolean> grappling = new HashMap<>();
  
  public WeaponsProvider() {
    worlds = new ArrayList<>();
    rockets = new ArrayList<>();
    grenades = new ArrayList<>();
    hooks = new ArrayList<>();
  }
  
  public void registerWorld(String world) {
    worlds.add(world);
  }
  
  public void unregisterWorld(String world) {
    worlds.remove(world);
  }
  
  @EventHandler
  public void playerTeleport(PlayerTeleportEvent evt) {
    if (grappling.getOrDefault(evt.getPlayer().getUniqueId(), false)) {
      grappling.remove(evt.getPlayer().getUniqueId());
    }
  }
  
  @EventHandler
  public void playerMove(PlayerMoveEvent evt) {
    if (grappling.getOrDefault(evt.getPlayer().getUniqueId(), false) == true) {
      Player player = evt.getPlayer();
      Material m = player.getLocation().getBlock().getType();
      if (!m.isAir()) {
        grappling.remove(evt.getPlayer().getUniqueId());
      }
    }
  }
  
  @EventHandler
  public void playerDamage(EntityDamageEvent evt) {
    Entity ent = evt.getEntity();
    if (ent instanceof Player) {
      Player player = (Player)ent;
      if (evt.getCause().equals(DamageCause.FALL) && grappling.getOrDefault(player.getUniqueId(), false)) {
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
          rockets.add(arr);
        } else if (Globals.isNamedItem(holding, Material.CROSSBOW, Globals.WEAPON_GRAPPLING_HOOK)) {
          hooks.add(arr);
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
      if (rockets.contains(arr)) {
        Location loc = arr.getLocation();
        World w = loc.getWorld();
        if (arr.getShooter() instanceof Player) {
          lastDetonated = arr.getLocation().clone();
          lastDetonator = (Player) arr.getShooter();
          lastDetonation = System.currentTimeMillis();
        }
        w.createExplosion(loc, 4.0f, false, false);
        emulateExplosion(loc, 4);
        arr.remove();
        rockets.remove(arr);
      } else if (hooks.contains(arr)) {
        if (arr.getShooter() instanceof Player) {
          launchTowardLocation((Player)arr.getShooter(), arr.getLocation().clone());
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
  
  private void launchTowardLocation(final Player p, Location loc) {
    // TODO
    Location ploc = p.getLocation();
    double dx = loc.getX() - ploc.getX();
    double dy = (loc.getY() - ploc.getY()) + 1;
    double dz = loc.getZ() - ploc.getZ();
    
    final double vx = dx / 10;
    final double vz = dz / 10;
    final double vy = (dy / 10) + 1.6; // counteract minecraft's 32 m/s^2 gravity
    
    grappling.put(p.getUniqueId(), true);
    p.setVelocity(new Vector(0, vy, 0));
    Bukkit.getScheduler().scheduleSyncDelayedTask(HeistMC.getInstance(), new Runnable() {
      public void run() {
        Vector vel = new Vector(vx, vy, vz);
        Vector horiz = vel.clone();
        horiz.setY(0.0);
        horiz.normalize();
        vel.add(horiz.multiply(1.5));
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
