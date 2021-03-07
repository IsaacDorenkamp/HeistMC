package anti.projects.heistmc.ui;

import java.util.List;

import org.bukkit.Material;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.mission.MissionObjective;
import anti.projects.heistmc.stages.ObjectiveSetTracker;

public class MissionObjectiveMenu extends MenuPage {
  private List<MissionObjective> ref;
  private MultiViewMenu parent;
  private ObjectiveSetTracker target;
  public MissionObjectiveMenu(List<MissionObjective> ref, MultiViewMenu parent, ObjectiveSetTracker target) {
    this.ref = ref;
    this.parent = parent;
    this.target = target;
    construct();
  }
  
  private void construct() {
    addItem(11, Material.WRITABLE_BOOK, Globals.STRING_MAKE_OBJECTIVE, new MenuItemListener() {
      public void onSelected() {
        parent.pushView(new MissionObjectiveTypeMenu(parent, target));
      }
    });
    
    addItem(13, Material.WRITTEN_BOOK, Globals.STRING_VIEW_OBJECTIVES, new MenuItemListener( ) {
      public void onSelected() {
        parent.pushView(new MissionObjectiveListMenu(parent, ref, target));
      }
    });
    
    addItem(40, Material.BOOK, Globals.STRING_BACK, new MenuItemListener() {
      public void onSelected() {
        parent.popView();
      }
    });
  }
}
