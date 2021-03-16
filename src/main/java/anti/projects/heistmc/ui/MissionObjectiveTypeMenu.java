package anti.projects.heistmc.ui;

import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.mission.*;
import anti.projects.heistmc.stages.ObjectiveSetTracker;

public class MissionObjectiveTypeMenu extends MenuPage {
  private MultiViewMenu parent;
  private ObjectiveSetTracker target;
  public MissionObjectiveTypeMenu(MultiViewMenu parent, ObjectiveSetTracker target) {
    super();
    this.parent = parent;
    this.target = target;
    construct();
  }
  
  private void construct() {
    
    addItem(11, Material.COMPASS, Globals.STRING_LOCATION_OBJECTIVE, new MenuItemListener() {
      public void onSelected() {
        Player viewer = parent.getViewer();
        if (target.isConfiguring()) {
          MessageUtil.send(viewer, "You are already configuring an objective!");
        } else {
          target.startConfiguring(viewer, new LocationObjective("Location Objective"), new Consumer<MissionObjective>() {
            public void accept(MissionObjective product) {
              target.getObjectives().add(product);
            }
          });
          MessageUtil.send(viewer, "Now configuring an objective.");
        }
        viewer.closeInventory();
      }
    });
    
    addItem(13, Material.DEBUG_STICK, Globals.STRING_ITEM_OBJECTIVE, new MenuItemListener() {
      public void onSelected() {
        Player viewer = parent.getViewer();
        if (target.isConfiguring()) {
          MessageUtil.send(viewer, "You are already configuring an objective!");
        } else {
          target.startConfiguring(viewer, new ItemObjective("Item Objective"), new Consumer<MissionObjective>() {
            public void accept(MissionObjective product) {
              target.getObjectives().add(product);
            }
          });
          MessageUtil.send(viewer, "Now configuring an objective.");
        }
        viewer.closeInventory();
      }
    });
    
    addItem(15, Material.DIAMOND_SWORD, Globals.STRING_KILL_OBJECTIVE, new MenuItemListener() {

      @Override
      public void onSelected() {
        Player viewer = parent.getViewer();
        if (target.isConfiguring()) {
          MessageUtil.send(viewer,  "You are already configuring an objective!");
        } else {
          target.startConfiguring(viewer, new KillObjective(), new Consumer<MissionObjective>() {
            public void accept(MissionObjective product) {
              target.getObjectives().add(product);
            }
          });
        }
        viewer.closeInventory();
      }
      
    });
    
    addItem(40, Material.BOOK, Globals.STRING_BACK, new MenuItemListener() {
      public void onSelected() {
        parent.popView();
      }
    });
  }
}
