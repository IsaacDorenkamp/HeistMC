package anti.projects.heistmc.stages;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import anti.projects.heistmc.mission.MissionObjective;

public class ObjectiveSetTracker {
  private boolean configuring = false;
  private Player actor = null;
  private MissionObjective constructing = null;
  private Consumer<MissionObjective> callback = null;
  
  private List<MissionObjective> objectives;
  private BuildWorld forWorld;
  
  public ObjectiveSetTracker(List<MissionObjective> tracking, BuildWorld forWorld) {
    this.objectives = tracking;
    this.forWorld = forWorld;
  }
  
  public List<MissionObjective> getObjectives() {
    return objectives;
  }
  
  public boolean isConfiguring() {
    return configuring;
  }
  
  public boolean startConfiguring(Player p, MissionObjective constructing, Consumer<MissionObjective> onFinish) {
    boolean success = constructing.onStartConfig(p);
    if (!success) {
      return false;
    }
    configuring = true;
    actor = p;
    this.constructing = constructing;
    callback = onFinish;
    return true;
  }
  
  public void stopConfiguring() {
    callback.accept(constructing);
    configuring = false;
    actor = null;
    constructing = null;
    callback = null;
  }
  
  public boolean onInteract(PlayerInteractEvent evt) {
    if (configuring) {
      if (evt.getPlayer().equals(actor)) {
        boolean isFinished = constructing.tryConfigAction(evt, forWorld);
        if (isFinished) {
          stopConfiguring();
        }
        return true;
      }
      return false;
    } else {
      return false;
    }
  }
}
