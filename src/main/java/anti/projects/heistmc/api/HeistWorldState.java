package anti.projects.heistmc.api;

import java.util.Iterator;

import anti.projects.heistmc.mission.MissionObjective;
import anti.projects.heistmc.stages.HeistWorldData;

public class HeistWorldState {
  private Iterator<MissionObjective> objectives;
  private MissionObjective current;
  
  public HeistWorldState(HeistWorldData initialState) {
    objectives = initialState.getObjectives().iterator();
    if (objectives.hasNext()) {
      current = objectives.next();
    } else {
      current = null;
    }
  }
  
  public MissionObjective getCurrentObjective() {
    return current;
  }
  
  public boolean next() {
    if (objectives.hasNext()) {
      current = objectives.next();
      return false;
    } else {
      current = null;
      return true; // true denotes that the heist is finished
    }
  }
}
