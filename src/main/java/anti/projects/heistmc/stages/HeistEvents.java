package anti.projects.heistmc.stages;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.TNT;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.InventoryHolder;

import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.api.BreakableBlock;

public class HeistEvents implements Listener {
  private HeistWorld world;
  private WorldManager mgr;
  
  public boolean isForHeist(PlayerEvent evt) {
    return world.hasPlayer(evt.getPlayer());
  }
  
  public boolean isForHeist(BlockBreakEvent evt) {
    return world.hasPlayer(evt.getPlayer());
  }
  
  public boolean isForHeist(BlockPlaceEvent evt) {
    return world.hasPlayer(evt.getPlayer());
  }
  
  public boolean isForHeist(InventoryPickupItemEvent evt) {
    InventoryHolder holder = evt.getInventory().getHolder();
    if (holder instanceof Player) {
      return world.hasPlayer((Player)holder);
    } else {
      return false;
    }
  }
  
  public boolean isForHeist(EntityEvent evt) {
    return evt.getEntity().getWorld().equals(world.getWorld());
  }
  
  public HeistEvents(HeistWorld world, WorldManager mgr) {
    this.world = world;
    this.mgr = mgr;
  }
  
  @EventHandler
  public void entityExplode(EntityExplodeEvent evt) {
    if (isForHeist(evt)) {
      evt.setCancelled(true);
      world.getWorld().playSound(evt.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
      world.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, evt.getLocation(), 5, null);
      HeistWorldData data = world.getData();
      Iterator<Block> it = evt.blockList().iterator();
      while (it.hasNext()) {
        Block b = it.next();
        Location loc = b.getLocation();
        if (data.getBreakableBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) != null) {
          b.setType(Material.AIR);
        }
      }
    }
  }
  
  @EventHandler
  public void blockBreak(BlockBreakEvent evt) {
    if (isForHeist(evt)) {
      Location loc = evt.getBlock().getLocation();
      BreakableBlock bb = world.getData().getBreakableBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      if (bb == null) evt.setCancelled(true);
    }
  }
  
  @EventHandler
  public void blockPlace(BlockPlaceEvent evt) {
    if (isForHeist(evt)) {
      if (evt.getBlock().getType().equals(Material.TNT)) {
        evt.getBlock().setType(Material.AIR);
        world.getWorld().spawnEntity(evt.getBlock().getLocation(), EntityType.PRIMED_TNT);
      } else {
        evt.setCancelled(true);
      }
    }
  }
  
  private void checkObjective() {
    world.checkNextObjective();
  }
  
  @EventHandler
  public void playerDamage(EntityDamageEvent evt) {
    if (isForHeist(evt) && evt.getEntity() instanceof Player) {
      // TODO - allow multiple deaths
      final Player p = (Player)evt.getEntity();
      if (p.getHealth() - evt.getDamage() <= 0) {
        evt.setCancelled(true);
        Bukkit.getScheduler().scheduleSyncDelayedTask(HeistMC.getInstance(), new Runnable() {
          public void run() {
            world.finish(mgr.getMainWorld().getSpawnLocation(), false, String.format("%s died.", p.getDisplayName()));
          }
        });
      }
    }
  }
  
  @EventHandler
  public void playerMove(PlayerMoveEvent evt) {
    if (isForHeist(evt)) {
      checkObjective();
    }
  }
  
  @EventHandler
  public void playerInventoryChange(InventoryPickupItemEvent evt) {
    if (isForHeist(evt)) {
      checkObjective();
    }
  }
  
  @EventHandler
  public void entityDeath(EntityDeathEvent evt) {
    if (isForHeist(evt)) {
      evt.getDrops().clear();
      checkObjective();
    }
  }
  
  @EventHandler
  public void entityCombust(EntityCombustEvent evt) {
    if (isForHeist(evt)) {
      evt.setCancelled(true);
    }
  }
  
  @EventHandler
  public void playerQuit(PlayerQuitEvent evt) {
    if (isForHeist(evt)) {
      Player p = evt.getPlayer();
      world.removePlayer(p, ChatColor.YELLOW + "" + ChatColor.BOLD + p.getDisplayName() + ChatColor.RESET + ""
          + ChatColor.RED + " has disconnected.");
    }
  }
  
  @EventHandler
  public void playerTeleport(PlayerTeleportEvent evt) {
    Player p = evt.getPlayer();
    if (isForHeist(evt)) {
      if (!evt.getTo().getWorld().equals(world.getWorld()) && world.isInProgress() && !world.isTransferring()) {
        world.removePlayer(p, ChatColor.YELLOW + "" + ChatColor.BOLD + p.getDisplayName() + ChatColor.RESET + ""
            + ChatColor.RED + " has left the heist.");
      }
    } else {
      if (evt.getTo().getWorld().equals(world.getWorld())) {
        MessageUtil.send(p, ChatColor.RED + "You may not teleport into a heist.");
        evt.setCancelled(true);
      }
    }
  }
}
