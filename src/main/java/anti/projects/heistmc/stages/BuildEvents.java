package anti.projects.heistmc.stages;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.ui.BuildMenu;
import anti.projects.heistmc.ui.MenuView;
import net.md_5.bungee.api.ChatColor;

public class BuildEvents implements Listener {
  private BuildWorld world;
  private WorldManager mgr;
  public BuildEvents(BuildWorld world, WorldManager mgr) {
    this.world = world;
    this.mgr = mgr;
  }
  
  private boolean isForBuild(PlayerUnleashEntityEvent evt) {
    return world.hasPlayer(evt.getPlayer());
  }
  
  private boolean isForBuild(PlayerEvent evt) {
    return world.hasPlayer(evt.getPlayer());
  }
  
  private boolean isForBuild(BlockPlaceEvent evt) {
    return world.hasPlayer(evt.getPlayer());
  }
  
  private boolean isForBuild(BlockBreakEvent evt) {
    return world.hasPlayer(evt.getPlayer());
  }
  
  private boolean isForBuild(InventoryClickEvent evt) {
    HumanEntity he = evt.getWhoClicked();
    if (he instanceof Player) {
      return world.hasPlayer((Player)he);
    } else {
      return false;
    }
  }
  
  private boolean isForBuild(EntityDamageEvent evt) {
    Entity target = evt.getEntity();
    if (target instanceof Player) {
      return world.hasPlayer((Player)target);
    } else {
      return false;
    }
  }
  
  private boolean isForBuild(InventoryCloseEvent evt) {
    return evt.getPlayer() instanceof Player && world.hasPlayer((Player)evt.getPlayer());
  }
  
  private boolean isForBuild(InventoryDragEvent evt) {
    return evt.getWhoClicked() instanceof Player && world.hasPlayer((Player)evt.getWhoClicked());
  }
  
  private boolean isForBuild(BlockDamageEvent evt) {
    return evt.getBlock().getWorld().equals(world.getWorld());
  }
  
  private boolean isForBuild(EntityEvent evt) {
    return (evt.getEntity().getWorld().equals(world.getWorld()));
  }
  
  @EventHandler
  public void playerQuit(PlayerQuitEvent evt) {
    if (isForBuild(evt)) {
      Player p = evt.getPlayer();
      p.teleport(mgr.getMainWorld().getSpawnLocation());
    }
  }
  
  @EventHandler
  public void blockPlace(BlockPlaceEvent evt) {
    if (isForBuild(evt)) {
      if(!evt.getPlayer().hasPermission(Globals.PERMISSION_BUILD)) {
        evt.setCancelled(true);
      }
    }
  }
  
  @EventHandler
  public void blockBreak(BlockBreakEvent evt) {
    if (isForBuild(evt)) {
      if (!evt.getPlayer().hasPermission(Globals.PERMISSION_BUILD)) {
        evt.setCancelled(true);
      }
    }
  }

  private static final long BREAK_THRESHOLD = 250L;
  private HashMap<Player, Long> lastBreak = new HashMap<Player, Long>();
  @EventHandler
  public void blockDamage(BlockDamageEvent evt) {
    if (isForBuild(evt)) {
      long actionTime = System.currentTimeMillis();
      Long pLastBreak = lastBreak.get(evt.getPlayer());
      if (pLastBreak == null || (pLastBreak != null && (actionTime - pLastBreak) >= BREAK_THRESHOLD)) {
        evt.setInstaBreak(true);
        lastBreak.put(evt.getPlayer(), actionTime);
      } else {
        evt.setCancelled(true);
      }
    }
  }
  
  @EventHandler
  public void entitySpawn(PlayerUnleashEntityEvent evt) {
    if (isForBuild(evt)) {
      if (!evt.getPlayer().hasPermission(Globals.PERMISSION_BUILD)) {
        evt.setCancelled(true);
      }
    }
  }
  
  @EventHandler
  public void playerTeleport(PlayerTeleportEvent evt) {
    if (isForBuild(evt)) {
      if (!evt.getTo().getWorld().equals(world.getWorld())) {
        world.removePlayer(evt.getPlayer());
      }
    } else {
      if (evt.getTo().getWorld().equals(world.getWorld())) {
        if (!world.hasPlayer(evt.getPlayer())) world.putPlayer(evt.getPlayer());
      }
    }
  }
  
  @EventHandler
  public void inventoryClicked(InventoryClickEvent evt) {
    if (isForBuild(evt)) {
      HumanEntity clicked = evt.getWhoClicked();
      if (evt.getWhoClicked().getInventory().equals(evt.getClickedInventory())) {
        ItemStack cur = evt.getCurrentItem();
        if (Globals.isNamedItem(cur, Material.NETHER_STAR, Globals.STRING_EXIT)
            || Globals.isNamedItem(cur, Material.BOOK, Globals.STRING_BUILD_MENU)
            || Globals.isNamedItem(cur, Material.COMPASS, Globals.STRING_SET_SPAWN)
            || Globals.isNamedItem(cur, Material.ZOMBIE_HEAD, Globals.STRING_TOGGLE_PLACEHOLDERS)) {
          evt.setResult(Result.DENY);
          evt.setCancelled(true);
          clicked.setItemOnCursor(null);
        }
      }
      
      if (clicked instanceof Player) {
        MenuView viewing = world.getMenu((Player)clicked);
        if (viewing != null) {
          if (evt.getClickedInventory().equals(viewing.getInventoryView().getTopInventory())) {
            evt.setCancelled(true);
            evt.setResult(Result.DENY);
            clicked.setItemOnCursor(null);
            viewing.onInventoryClick(evt);
          } else if (evt.getClickedInventory().equals(clicked.getInventory())) {
            if (evt.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
              evt.setCancelled(true);
              evt.setResult(Result.DENY);
            }
          }
        }
      }
    }
  }
  
  @EventHandler
  public void entityCombust(EntityCombustEvent evt) {
    // stop build entities from being on fire
    if (isForBuild(evt)) {
      evt.setCancelled(true);
    }
  }
  
  @EventHandler
  public void inventoryDragged(InventoryDragEvent evt) {
    if (isForBuild(evt)) {
      HumanEntity clicked = evt.getWhoClicked();
      if (clicked instanceof Player) {
        Player p = (Player)clicked;
        MenuView viewing = world.getMenu(p);
        if (viewing != null) {
          Integer min = Globals.min(evt.getRawSlots());
          if (min != null && min < viewing.getSlots()) {
            evt.setResult(Result.DENY);
          }
        }
      }
    }
  }
  
  @EventHandler
  public void entityDamage(EntityDamageEvent evt) {
    if (isForBuild(evt)) {
      evt.setCancelled(true);
    }
  }
  
  @EventHandler
  public void playerDrop(PlayerDropItemEvent evt) {
    if (isForBuild(evt)) {
      ItemStack drop = evt.getItemDrop().getItemStack();
      if (Globals.isNamedItem(drop, Material.COMPASS, Globals.STRING_SET_SPAWN)
          || Globals.isNamedItem(drop, Material.BOOK, Globals.STRING_BUILD_MENU)
          || Globals.isNamedItem(drop, Material.NETHER_STAR, Globals.STRING_EXIT)
          || Globals.isNamedItem(drop, Material.ZOMBIE_HEAD, Globals.STRING_TOGGLE_PLACEHOLDERS)) {
        evt.setCancelled(true);
      }
    }
  }
  
  @EventHandler
  public void useItem(PlayerInteractEvent evt) {
    if (isForBuild(evt)) {
      
      if (world.getMissionObjectiveTracker().isConfiguring()) {
        boolean consume = world.getMissionObjectiveTracker().onInteract(evt);
        if (consume) {
          evt.setCancelled(true);
          return;
        }
      }
      
      
      if (evt.getAction() == Action.RIGHT_CLICK_AIR || evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
        ItemStack is = evt.getItem();
        boolean useItem = true;
        if (Globals.isNamedItem(is, Material.NETHER_STAR, Globals.STRING_EXIT)) {
          world.removePlayer(evt.getPlayer());
          useItem = false;
        } else if (Globals.isNamedItem(is, Material.BOOK, Globals.STRING_BUILD_MENU)) {
          Player p = evt.getPlayer();
          BuildMenu menu = new BuildMenu(p, world);
          world.showMenu(menu, p);
          useItem = false;
        } else if (Globals.isNamedItem(is, Material.COMPASS, Globals.STRING_SET_SPAWN)) {
          Location l = evt.getPlayer().getLocation().getBlock().getLocation(); // snap location to block
          world.getWorld().setSpawnLocation(l);
          MessageUtil.send(evt.getPlayer(), "Spawn location set to " + ChatColor.ITALIC + String.format("(%d, %d, %d)",
              l.getBlockX(), l.getBlockY(), l.getBlockZ()));
          useItem = false;
        } else if (Globals.isNamedItem(is, Material.ZOMBIE_HEAD, Globals.STRING_TOGGLE_PLACEHOLDERS)) {
          world.togglePlaceholderMobs();
          useItem = false;
        } else if (is != null) {
          // TODO - this fix sucks lol. do something else
          if (is.getType().isBlock() && evt.getClickedBlock() != null) {
            is.setAmount(is.getAmount() + 1);
            evt.getPlayer().getInventory().setItemInMainHand(is);
          }
        }
        evt.setUseItemInHand(useItem ? Result.ALLOW : Result.DENY);
        evt.setUseInteractedBlock(useItem ? Result.ALLOW : Result.DENY);
      }
    }
  }
  
  @EventHandler
  public void inventoryClosed(InventoryCloseEvent evt) {
    if (isForBuild(evt)) {
      HumanEntity he = evt.getPlayer();
      if (he instanceof Player) {
        Player p = (Player)he;
        if (world.getMenu(p) != null) {
          if (world.getMenu(p).getInventoryView().getTopInventory().equals(evt.getInventory())) world.menuRevoked(p);
        }
      }
    }
  }
  
  @EventHandler
  public void belowZero(PlayerMoveEvent evt) {
    if (isForBuild(evt)) {
      Location l = evt.getTo();
      if (l.getY() < -5) {
        evt.getPlayer().teleport(l.add(0, 25, 0));
      }
    }
  }
}
