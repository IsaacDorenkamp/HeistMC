package anti.projects.heistmc.stages;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.MapManager;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.WorldManager;
import anti.projects.heistmc.ui.MapSelectMenu;
import anti.projects.heistmc.ui.Menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LobbyEvents implements Listener {
  private Lobby lobbyFor;
  private WorldManager mgr;
  private MapManager maps;
  public LobbyEvents(Lobby lobbyFor, MapManager maps, WorldManager mgr) {
    this.lobbyFor = lobbyFor;
    this.mgr = mgr;
    this.maps = maps;
  }
  
  public boolean isForLobby(PlayerEvent e) {
    return lobbyFor.hasPlayer(e.getPlayer());
  }
  
  public boolean isForLobby(BlockBreakEvent evt) {
    return lobbyFor.hasPlayer(evt.getPlayer());
  }
  
  public boolean isForLobby(BlockPlaceEvent evt) {
    return lobbyFor.hasPlayer(evt.getPlayer());
  }
  
  public boolean isForLobby(InventoryClickEvent evt) {
    HumanEntity he = evt.getWhoClicked();
    if (he instanceof Player) {
      return lobbyFor.hasPlayer((Player)he);
    } else {
      return false;
    }
  }
  
  public boolean isForLobby(EntityDamageEvent evt) {
    if (evt.getEntity() instanceof Player) {
      Player p = (Player)evt.getEntity();
      return lobbyFor.hasPlayer(p);
    } else return false;
  }
  
  public boolean isForLobby(InventoryCloseEvent evt) {
    if (evt.getPlayer() instanceof Player && lobbyFor.hasPlayer((Player)evt.getPlayer())) {
      return true;
    } else {
      return false;
    }
  }
  
  @EventHandler
  public void playerTeleport(PlayerTeleportEvent evt) {
    Player p = evt.getPlayer();
    if (isForLobby(evt)) {
      if (!evt.getTo().getWorld().equals(lobbyFor.getWorld())) {
        lobbyFor.removePlayer(p, "Player " + ChatColor.YELLOW + p.getName() + ChatColor.RESET + " left the lobby.");
      }
    } else {
      if (evt.getTo().getWorld().equals(lobbyFor.getWorld())) {
        evt.setCancelled(true); // prevent player from teleporting into a lobby
      }
    }
  }
  
  @EventHandler
  public void playerQuit(PlayerQuitEvent evt) {
    if (isForLobby(evt)) {
      Player p = evt.getPlayer();
      lobbyFor.removePlayer(p, "Player " + ChatColor.YELLOW + "" + ChatColor.BOLD + p.getDisplayName() + ChatColor.RESET
          + " disconnected.");
    }
  }
  
  @EventHandler
  public void blockBroken(BlockBreakEvent bbe) {
    if (isForLobby(bbe)) {
      bbe.setCancelled(true);
    }
  }
  
  @EventHandler
  public void blockPlaced(BlockPlaceEvent bpe) {
    if (isForLobby(bpe)) {
      bpe.setCancelled(true);
    }
  }
  
  @EventHandler
  public void inventoryClick(InventoryClickEvent evt) {
    if (isForLobby(evt)) {
      evt.setCancelled(true);
      
      HumanEntity he = evt.getWhoClicked();
      Menu m = menus.get(he);
      if (m != null) {
        m.onInventoryClick(evt);
      }
      he.setItemOnCursor(null);
    }
  }
  
  @EventHandler
  public void inventoryClosed(InventoryCloseEvent evt) {
    if (isForLobby(evt)) {
      Player p = (Player)evt.getPlayer();
      Menu m = menus.get(p);
      if (m != null && m.getInventoryView().getTopInventory().equals(evt.getInventory())) {
        menus.remove(p);
        m.onExit();
      }
    }
  }
  
  @EventHandler
  public void itemThrow(PlayerDropItemEvent evt) {
    if (isForLobby(evt)) {
      evt.setCancelled(true);
    }
  }
  
  @EventHandler
  public void useItem(PlayerInteractEvent evt) {
    if (isForLobby(evt) && (evt.getAction() == Action.RIGHT_CLICK_AIR ||
        evt.getAction() == Action.RIGHT_CLICK_BLOCK)) {
      final Player p = evt.getPlayer();
      ItemStack is = p.getInventory().getItemInMainHand();
      if (Globals.isNamedItem(is, Material.NETHER_STAR, Globals.STRING_EXIT)) {
        lobbyFor.removePlayer(p, ChatColor.YELLOW + "" + ChatColor.BOLD + p.getDisplayName() +
            ChatColor.RESET + "" + ChatColor.RED + " left the lobby.");
      } else if (Globals.isNamedItem(is, Material.ARROW, Globals.STRING_START_HEIST)) {
        lobbyFor.transfer();
      } else if (Globals.isNamedItem(is, Material.PAPER, Globals.STRING_SELECT_MAP)) {
        MapSelectMenu msm = new MapSelectMenu(maps, p, new Consumer<String>() { public void accept(String s) {
          lobbyFor.setSelectedMap(s);
          p.closeInventory();
          MessageUtil.sendToRoom(lobbyFor, "The lobby leader has selected the map '" + s + "' for the heist.");
        } });
        showMenu(p, msm);
      }
    }
  }
  
  /* menu management code */
  private Map<HumanEntity, Menu> menus = new HashMap<HumanEntity, Menu>();
  private void showMenu(Player p, Menu m) {
    menus.put(p, m);
    m.onShow();
    p.openInventory(m.getInventoryView());
  }
  
  @EventHandler
  public void onPvP(EntityDamageEvent evt) {
    if (isForLobby(evt)) {
      evt.setCancelled(true);
    }
  }
}
