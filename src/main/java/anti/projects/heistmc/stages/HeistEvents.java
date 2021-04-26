package anti.projects.heistmc.stages;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.TNT;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.api.BreakableBlock;
import anti.projects.heistmc.api.HeistPlayer;
import anti.projects.heistmc.ui.ConfirmView;

public class HeistEvents implements Listener {
  private static final Random rng = new Random();
  
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
      
      Entity ent = evt.getEntity();
      Player whoDid = tnt.get(ent.getUniqueId());
      if (whoDid != null) {
        tnt.remove(ent.getUniqueId());
        HeistMC.getInstance().getWeaponsProvider().detonate(evt.getLocation(), whoDid, System.currentTimeMillis());
      }
      
      evt.setCancelled(true);
      world.getWorld().createExplosion(ent.getLocation(), 4.0f, false, false);
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
  public void hangingBreak(HangingBreakByEntityEvent evt) {
    if (isForHeist(evt)) {
      evt.setCancelled(true);
    }
  }
  
  @EventHandler
  public void itemFrame(EntityDamageByEntityEvent evt) {
    if (isForHeist(evt)) {
      Entity interacted = evt.getEntity();
      if (interacted instanceof ItemFrame && evt.getDamager() instanceof Player) {
        final Player player = (Player)evt.getDamager();
        ItemFrame iframe = (ItemFrame)interacted;
        final ItemStack is = iframe.getItem();
        if (is == null) return;
        ItemMeta im = is.getItemMeta();
        System.out.println("HERE");
        if (im.hasLore()) {
          List<String> lore = im.getLore();
          if (lore.size() > 0) {
            String priceLine = lore.get(0);
            
            final String finalName;
            if (lore.size() >= 2) {
              finalName = lore.get(1);
            } else {
              finalName = im.getDisplayName();
            }
            
            final HeistPlayer hp = world.getHeistPlayer(player);
            if (hp == null) {
              evt.setCancelled(true);
              return;
            }
              
            
            if (priceLine.matches(".*Price:\\s\\$[0-9]+(\\.[0-9]{2})?$")) {
              final double price = Double.parseDouble(priceLine.split("\\$")[1]);
              
              if (hp.getMoney() < price) {
                MessageUtil.send(player, ChatColor.RED + "" + ChatColor.BOLD + "You can't afford that!");
                evt.setCancelled(true);
                return;
              }
              
              String title = String.format("Cost: $%.2f. Purchase?", price);
              ConfirmView cv = new ConfirmView(player, title, new Consumer<Boolean>() {
                public void accept(Boolean b) {
                  if (b.equals(true)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1f, 1f);
                    ItemStack item = is.clone();
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(finalName);
                    meta.setLore(null);
                    item.setItemMeta(meta);
                    // TODO - owner meta?
                    player.getInventory().addItem(item);
                    
                    if (hp.getMoney() >= price) {
                      hp.setMoney(hp.getMoney() - price);
                    } else {
                      MessageUtil.send(player, ChatColor.RED + "" + ChatColor.BOLD + "You can't afford that!");
                    }
                  }
                }
              });
              world.showConfirmView(player, cv);
              MessageUtil.send(player, String.format("That costs " + ChatColor.GREEN + "$%.2f", price));
              evt.setCancelled(true);
            }
          }
        }
      }
    }
  }
  
  @EventHandler
  public void noBreakHanging(HangingBreakEvent evt) {
    if (isForHeist(evt)) {
      if (evt.getCause().equals(RemoveCause.EXPLOSION)) {
        evt.setCancelled(true);
      }
    }
  }
  
  private boolean isForHeist(HangingBreakEvent evt) {
    return evt.getEntity().getWorld().equals(world.getWorld());
  }

  @EventHandler
  public void blockBreak(BlockBreakEvent evt) {
    if (isForHeist(evt)) {
      Location loc = evt.getBlock().getLocation();
      BreakableBlock bb = world.getData().getBreakableBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      if (bb == null) evt.setCancelled(true);
    }
  }
  
  private HashMap<UUID, Player> tnt = new HashMap<>();
  
  @EventHandler
  public void blockPlace(BlockPlaceEvent evt) {
    if (isForHeist(evt)) {
      if (evt.getBlock().getType().equals(Material.TNT)) {
        evt.getBlock().setType(Material.AIR);
        Entity ent = world.getWorld().spawnEntity(evt.getBlock().getLocation(), EntityType.PRIMED_TNT);
        tnt.put(ent.getUniqueId(), evt.getPlayer());
      } else {
        evt.setCancelled(true);
      }
    }
  }
  
  private void checkObjective() {
    world.checkNextObjective();
  }
  
  @EventHandler
  public void entityDamage(EntityDamageEvent evt) {
    if (isForHeist(evt)) {
      
      if (evt.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
        evt.setCancelled(true);
        return;
      }
      
      if (evt.getEntity() instanceof Player) {
        // TODO - allow multiple deaths
        final Player p = (Player)evt.getEntity();
        if (p.getHealth() - evt.getDamage() <= 0) {
          evt.setCancelled(true);
          Bukkit.getScheduler().scheduleSyncDelayedTask(HeistMC.getInstance(), new Runnable() {
            public void run() {
              world.finish(mgr.getMainWorld().getSpawnLocation(), false, String.format("%s died.", p.getDisplayName()), false);
            }
          });
        }
      } else if (evt.getEntity() instanceof Hanging) {
        if (evt.getEntity() instanceof ItemFrame) {
          if (((ItemFrame)evt.getEntity()).getItem() == null) {
            evt.setCancelled(true);
          }
        } else {
          evt.setCancelled(true); // prevent item frames, paintings etc from blowing up
        }
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
      
      Entity ent = evt.getEntity();
      if (ent instanceof LivingEntity && !(ent instanceof Player)) {
        HeistPlayer hp = world.getHeistPlayer(evt.getEntity().getKiller());
        if (hp != null) {
          hp.addMoneyFor(ent.getType());
        }
      }
      
      evt.setDroppedExp(0);
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
  
  @EventHandler
  public void inventoryClick(InventoryClickEvent evt) {
    if (isForHeist(evt)) {
      Player p = (Player)evt.getWhoClicked();
      ConfirmView cv = world.getConfirmView(p);
      if (cv != null) {
        ItemStack is = evt.getClickedInventory().getItem(evt.getSlot());
        boolean close = true;
        if (Globals.isNamedItem(is, Material.LIME_STAINED_GLASS_PANE, "Yes")) {
          cv.getCallback().accept(true);
        } else if (Globals.isNamedItem(is, Material.RED_STAINED_GLASS_PANE, "No")) {
          cv.getCallback().accept(false);
        } else {
          close = false;
        }
        
        if (close) p.closeInventory();
        
        evt.setResult(Result.DENY);
        evt.setCancelled(true);
      }
    }
  }
  
  @EventHandler
  public void inventoryClose(InventoryCloseEvent evt) {
    if (isForHeist(evt)) {
      if (world.getConfirmView((Player)evt.getPlayer()) != null) {
        world.revokeConfirmView((Player)evt.getPlayer());
      }
    }
  }

  private boolean isForHeist(InventoryClickEvent evt) {
    if (evt.getWhoClicked() instanceof Player) {
      Player player = (Player)evt.getWhoClicked();
      return world.hasPlayer(player);
    } else return false;
  }
  
  private boolean isForHeist(InventoryCloseEvent evt) {
    return evt.getPlayer() instanceof Player ? world.hasPlayer((Player)evt.getPlayer()) : false;
  }
}
