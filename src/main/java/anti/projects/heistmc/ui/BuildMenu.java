package anti.projects.heistmc.ui;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.stages.BuildWorld;

public class BuildMenu extends MultiViewMenu {
  
  private Player viewer;
  private BuildWorld world;
  public BuildMenu(Player viewer, BuildWorld world) {
    super(Globals.STRING_BUILD_MENU, 54, viewer);
    this.viewer = viewer;
    this.world = world;
    constructBaseMenu();
  }
  
  public BuildWorld getWorld() {
    return world;
  }
  
  private void constructBaseMenu() {
    MenuPage base = getBaseMenu();
    base.addItem(11, Material.SCAFFOLDING, "Creative Inventory", new MenuItemListener() {
      public void onSelected() {
        pushView(new CreativeInventory(viewer, BuildMenu.this));
      }
    });
    
    base.addItem(13, Material.BOOK, "Mission Objectives", new MenuItemListener() {
      public void onSelected() {
        pushView(new MissionObjectiveMenu(world.getHeistWorldData().getObjectives(), BuildMenu.this, world.getMissionObjectiveTracker()));
      }
    });
  }

}
