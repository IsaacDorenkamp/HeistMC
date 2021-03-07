package anti.projects.heistmc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import anti.projects.heistmc.persist.InventoryPersist;

public class GlobalEvents implements Listener {
  
  private WorldManager mgr;
  private InventoryPersist persistence;
  public GlobalEvents(WorldManager mgr, InventoryPersist persistence) {
    this.mgr = mgr;
    this.persistence = persistence;
  }
  
  @EventHandler
  public void playerJoin(PlayerJoinEvent evt) {
    Player p = evt.getPlayer();
    if (persistence.hasEntry(p)) {
      persistence.popInventory(p);
    }
  }
  
  @EventHandler
  public void playerQuit(PlayerQuitEvent evt) {
    Player p = evt.getPlayer();
    if (!mgr.willPersist(p.getWorld())) {
      p.teleport(mgr.getMainWorld().getSpawnLocation());
    }
  }
}
