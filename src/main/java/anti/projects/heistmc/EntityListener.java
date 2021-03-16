package anti.projects.heistmc;

import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public interface EntityListener {
  public void blockDamage(EntityDamageByBlockEvent evt);
  public void entityDamage(EntityDamageByEntityEvent evt);
  public void death(EntityDeathEvent evt);
}
