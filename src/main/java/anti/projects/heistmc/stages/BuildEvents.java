package anti.projects.heistmc.stages;

import org.bukkit.GameMode;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.mission.KillObjective;
import anti.projects.heistmc.ui.BuildMenu;
import anti.projects.heistmc.ui.MenuView;

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
  
  private boolean isForBuild(EntityDamageByEntityEvent evt) {
    System.out.println("CHECKING");
    Entity target = evt.getEntity();
    System.out.println("IS: " + world.isPlaceholderMob(target));
    return world.isPlaceholderMob(target);
  }
  
  private boolean isForBuild(InventoryOpenEvent evt) {
    return evt.getPlayer() instanceof Player && world.hasPlayer((Player)evt.getPlayer());
  }
  
  private boolean isForBuild(InventoryCloseEvent evt) {
    return evt.getPlayer() instanceof Player && world.hasPlayer((Player)evt.getPlayer());
  }
  
  private boolean isForBuild(InventoryDragEvent evt) {
    return evt.getWhoClicked() instanceof Player && world.hasPlayer((Player)evt.getWhoClicked());
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
  
  private void populateInventory(Inventory inv) {
    if (!Globals.isNamedItem(inv.getItem(7), Material.BOOK, Globals.STRING_BUILD_MENU)) {
      inv.setItem(7, Globals.getMenuBook());
    }
    if (!Globals.isNamedItem(inv.getItem(8), Material.NETHER_STAR, Globals.STRING_EXIT)) {
      inv.setItem(8, Globals.getLeaveStar());
    }
  }
  
  @EventHandler
  public void inventoryClicked(InventoryClickEvent evt) {
    if (isForBuild(evt)) {
      HumanEntity clicked = evt.getWhoClicked();
      if (evt.getWhoClicked().getInventory().equals(evt.getClickedInventory())) {
        final Inventory inv = evt.getWhoClicked().getInventory();
        populateInventory(inv);
      }
      
      if (clicked instanceof Player) {
        MenuView viewing = world.getMenu((Player)clicked);
        if (viewing != null) {
          if (evt.getClickedInventory().equals(viewing.getInventoryView().getTopInventory())) {
            evt.setCancelled(true);
            evt.setResult(Result.DENY);
            clicked.setItemOnCursor(null);
            viewing.onInventoryClick(evt);
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
  public void entityDamageByEntity(EntityDamageByEntityEvent evt) {
    if (isForBuild(evt)) {
      Entity damager = evt.getDamager();
      if (damager instanceof Player) {
        Player p = (Player)damager;
        ItemStack inMain = p.getInventory().getItemInMainHand();
        if (Globals.isNamedItem(inMain, Material.DIAMOND_SWORD, Globals.STRING_REMOVE_MOB)) {
          KillObjective selected = world.getSelectedPlaceholders();
          if (selected != null) {
            world.removeEntryForPlaceholder(evt.getEntity());
            evt.setCancelled(true);
          }
        }
      }
    }
  }
  
  @EventHandler
  public void playerDrop(PlayerDropItemEvent evt) {
    if (isForBuild(evt)) {
      ItemStack drop = evt.getItemDrop().getItemStack();
      if (Globals.isNamedItem(drop, Material.COMPASS, Globals.STRING_SET_SPAWN)
          || Globals.isNamedItem(drop, Material.BOOK, Globals.STRING_BUILD_MENU)
          || Globals.isNamedItem(drop, Material.NETHER_STAR, Globals.STRING_EXIT)
          || Globals.isNamedItem(drop, Material.ZOMBIE_HEAD, Globals.STRING_HIDE_PLACEHOLDERS)) {
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
        int slot = evt.getPlayer().getInventory().getHeldItemSlot();
        boolean useItem = true;
        if (Globals.isNamedItem(is, Material.NETHER_STAR, Globals.STRING_EXIT) && slot == 8) {
          world.removePlayer(evt.getPlayer());
          useItem = false;
        } else if (Globals.isNamedItem(is, Material.BOOK, Globals.STRING_BUILD_MENU) && slot == 7) {
          Player p = evt.getPlayer();
          BuildMenu menu = new BuildMenu(p, world);
          world.showMenu(menu, p);
          useItem = false;
        }
        evt.setUseItemInHand(useItem ? Result.ALLOW : Result.DENY);
        evt.setUseInteractedBlock(useItem ? Result.ALLOW : Result.DENY);
      }
    }
  }
  
  @EventHandler
  public void inventoryOpened(InventoryOpenEvent evt) {
    if (isForBuild(evt)) {
      // we already know evt.getPlayer() is a Player instance
      Player p = (Player)evt.getPlayer();
      boolean isFlying = p.isFlying();
      p.setGameMode(GameMode.SURVIVAL);
      p.setAllowFlight(true);
      p.setFlying(isFlying);
    }
  }
  
  @EventHandler
  public void inventoryClosed(InventoryCloseEvent evt) {
    if (isForBuild(evt)) {
      HumanEntity he = evt.getPlayer();
      he.setGameMode(GameMode.CREATIVE);
      if (he instanceof Player) {
        Player p = (Player)he;
        if (world.inventoryCallback(p)) return;
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
