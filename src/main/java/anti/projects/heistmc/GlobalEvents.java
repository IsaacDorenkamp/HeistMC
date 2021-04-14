package anti.projects.heistmc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import anti.projects.heistmc.persist.InventoryPersist;
import anti.projects.heistmc.persist.PlayerStatePersist;

public class GlobalEvents implements Listener {
  
  private WorldManager mgr;
  private InventoryPersist persistence;
  private PlayerStatePersist ps_persistence;
  public GlobalEvents(WorldManager mgr, InventoryPersist persistence, PlayerStatePersist ps_persistence) {
    this.mgr = mgr;
    this.persistence = persistence;
    this.ps_persistence = ps_persistence;
  }
  
  @EventHandler
  public void playerJoin(PlayerJoinEvent evt) {
    Player p = evt.getPlayer();
    if (persistence.hasEntry(p)) {
      System.out.println("POPPING INVENTORY");
      persistence.popInventory(p);
    }
    if (ps_persistence.hasEntry(p)) {
      ps_persistence.popPlayerState(p);
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
