package anti.projects.heistmc.ui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import anti.projects.heistmc.Globals;

public class WeaponsMenu extends MenuPage {
  private Player viewer;
  public WeaponsMenu(Player viewer) {
    super();
    this.viewer = viewer;
    construct();
  }
  
  private void construct() {
    addItem(11, Material.BOW, Globals.WEAPON_ROCKET_LAUNCHER, new MenuItemListener() {
      @Override
      public void onSelected() {
        viewer.getInventory().addItem(Globals.getNamedItem(Material.BOW, Globals.WEAPON_ROCKET_LAUNCHER));
      }
    });
    addItem(13, Material.SNOWBALL, Globals.WEAPON_GRENADE, new MenuItemListener() {
      @Override
      public void onSelected() {
        viewer.getInventory().addItem(Globals.getNamedItem(Material.SNOWBALL, Globals.WEAPON_GRENADE));
      }
    });
    addItem(15, Material.CROSSBOW, Globals.WEAPON_GRAPPLING_HOOK, new MenuItemListener() {
      @Override
      public void onSelected() {
        viewer.getInventory().addItem(Globals.getNamedItem(Material.CROSSBOW, Globals.WEAPON_GRAPPLING_HOOK));
      }
    });
    
    addItem(29, Material.TNT, Globals.WEAPON_C4, new MenuItemListener() {
      @Override
      public void onSelected() {
        viewer.getInventory().addItem(Globals.getNamedItem(Material.TNT, Globals.WEAPON_C4));
      }
    });
  }
}
