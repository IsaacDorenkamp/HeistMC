package anti.projects.heistmc.ui;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.mission.KillObjective;
import anti.projects.heistmc.mission.MissionObjective;
import anti.projects.heistmc.stages.BuildWorld;
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
    addItem(12, Material.WRITABLE_BOOK, Globals.STRING_MAKE_OBJECTIVE, new MenuItemListener() {
      public void onSelected() {
        parent.pushView(new MissionObjectiveTypeMenu(parent, target));
      }
    });
    
    addItem(14, Material.WRITTEN_BOOK, Globals.STRING_VIEW_OBJECTIVES, new MenuItemListener( ) {
      public void onSelected() {
        // quick way to do this, not the best form but still reliable
        final BuildWorld bw = BuildWorld.getActiveInstance(parent.getViewer());
        MissionObjectiveListMenu menu = new MissionObjectiveListMenu(parent, ref, target);
        menu.setDeleteCallback(new Consumer<MissionObjective>() {
          public void accept(MissionObjective obj) {
            if (obj instanceof KillObjective) {
              KillObjective ko = (KillObjective)obj;
              if (ko.equals(bw.getSelectedPlaceholders())) {
                bw.selectPlaceholderMobs(null);
              }
            }
          }
        });
        menu.setKillSelectCallback(new Consumer<KillObjective>() {
          public void accept(KillObjective obj) {
            bw.selectPlaceholderMobs(obj);
          }
        });
        parent.pushView(menu);
      }
    });
    
    addItem(40, Material.BOOK, Globals.STRING_BACK, new MenuItemListener() {
      public void onSelected() {
        parent.popView();
      }
    });
  }
}
