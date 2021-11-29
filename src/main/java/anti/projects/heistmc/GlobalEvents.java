package anti.projects.heistmc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GlobalEvents implements Listener {
  
  private WorldManager mgr;
  public GlobalEvents(WorldManager mgr) {
    this.mgr = mgr;
  }
  
  @EventHandler
  public void playerJoin(PlayerJoinEvent evt) {
    Player p = evt.getPlayer();
  }
  
  @EventHandler
  public void playerQuit(PlayerQuitEvent evt) {
    Player p = evt.getPlayer();
    if (!mgr.willPersist(p.getWorld())) {
      p.teleport(mgr.getMainWorld().getSpawnLocation());
    }
  }
}
