package anti.projects.heistmc;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityTracker implements Listener {
  private Map<Entity, EntityListener> tracking = new HashMap<Entity, EntityListener>();
  public EntityTracker() {}
  
  public void track(Entity e, EntityListener entl) {
    if (tracking.containsKey(e)) {
      throw new IllegalStateException("Already tracking entity " + e.toString() + "!");
    } else {
      tracking.put(e, entl);
    }
  }
  
  public void untrack(Entity e) {
    tracking.remove(e);
  }
  
  @EventHandler
  public void blockDamage(EntityDamageByBlockEvent evt) {
    Entity subject = evt.getEntity();
    EntityListener listener = tracking.get(subject);
    if (listener != null) {
      listener.blockDamage(evt);
    }
  }
  
  @EventHandler
  public void entityDamage(EntityDamageByEntityEvent evt) {
    Entity subject = evt.getEntity();
    EntityListener listener = tracking.get(subject);
    if (listener != null) {
      listener.entityDamage(evt);
    }
  }
  
  @EventHandler
  public void death(EntityDeathEvent evt) {
    Entity subject = evt.getEntity();
    EntityListener listener = tracking.get(subject);
    if (listener != null) {
      listener.death(evt);
      
      // entity is dead, so unregister listener
      tracking.remove(subject);
    }
  }
}
