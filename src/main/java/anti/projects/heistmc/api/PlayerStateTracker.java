package anti.projects.heistmc.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerStateTracker implements Listener {
  private Map<UUID, PlayerState> states;
  
  public PlayerStateTracker() {
    states = new HashMap<UUID, PlayerState>();
  }
  
  public void setState(Player p, PlayerState state) {
    states.put(p.getUniqueId(), state);
  }
  
  public PlayerState getState(Player p) {
    PlayerState state = states.get(p.getUniqueId());
    if (state == null) {
      return PlayerState.OFFLINE;
    } else return state;
  }
  
  @EventHandler
  public void onQuit(PlayerQuitEvent evt) {
    states.remove(evt.getPlayer().getUniqueId());
  }
  
  @EventHandler
  public void onJoin(PlayerJoinEvent evt) {
    states.put(evt.getPlayer().getUniqueId(), PlayerState.ONLINE); // players will not spawn in lobbies or heist worlds
  }
}
