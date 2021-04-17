package anti.projects.heistmc.ui;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.stages.BuildWorld;
import net.md_5.bungee.api.ChatColor;

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
    base.addItem(11, Material.COMPASS, Globals.STRING_SET_SPAWN, new MenuItemListener() {
      public void onSelected() {
        Location l = viewer.getLocation();
        world.getWorld().setSpawnLocation(l);
        MessageUtil.send(viewer, "Spawn location set to " + ChatColor.ITALIC + String.format("(%d, %d, %d)",
            l.getBlockX(), l.getBlockY(), l.getBlockZ()));
        viewer.closeInventory();
      }
    });
    base.addItem(13, Material.BOOK, "Mission Objectives", new MenuItemListener() {
      public void onSelected() {
        pushView(new MissionObjectiveMenu(world.getHeistWorldData().getObjectives(), BuildMenu.this, world.getMissionObjectiveTracker()));
      }
    });
    base.addItem(15, Material.ZOMBIE_HEAD, Globals.STRING_HIDE_PLACEHOLDERS, new MenuItemListener() {
      public void onSelected() {
        world.selectPlaceholderMobs(null);
        viewer.closeInventory();
      }
    });
    
    base.addItem(31, Material.COBBLESTONE, "Breakable Blocks", new MenuItemListener() {
      public void onSelected() {
        pushView(new BreakableBlockMenu(viewer, world));
      }
    });
  }

}
