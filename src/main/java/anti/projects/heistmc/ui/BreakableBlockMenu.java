package anti.projects.heistmc.ui;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.stages.BuildWorld;

public class BreakableBlockMenu extends MenuPage {
  private Player viewer;
  private BuildWorld world;
  public BreakableBlockMenu(Player viewer, BuildWorld world) {
    this.viewer = viewer;
    this.world = world;
    construct();
  }
  
  private void construct() {
    addItem(12, Material.WHITE_WOOL, "Show/Hide Breakable Blocks", new MenuItemListener() {
      public void onSelected() {
        boolean isShowing = world.isShowingBreakable();
        if (isShowing) {
          MessageUtil.send(viewer, "Hiding breakable blocks");
          world.hideBreakableBlocks();
        } else {
          MessageUtil.send(viewer, "Showing breakable blocks");
          world.showBreakableBlocks();
        }
      }
    });
    addItem(14, Material.WOODEN_AXE, "Configure Breakable Blocks", new MenuItemListener() {
      public void onSelected() {
        ItemStack breaker = Globals.getNamedItem(Material.WOODEN_AXE, Globals.STRING_TOGGLE_BREAKABLE);
        if (viewer.getInventory().first(breaker) == -1) {
          @SuppressWarnings("rawtypes")
          HashMap m = viewer.getInventory().addItem(breaker);
          if (m.size() > 0) {
            viewer.getInventory().setItem(0, breaker);
          }
          MessageUtil.send(viewer, "Left-click blocks to toggle their breakability");
        } else {
          MessageUtil.send(viewer, "You already have the tool to do this!");
        }
      }
    });
  }
}
