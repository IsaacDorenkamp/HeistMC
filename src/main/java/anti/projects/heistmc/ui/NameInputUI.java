package anti.projects.heistmc.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import anti.projects.heistmc.Globals;

public class NameInputUI extends InventoryView {
  
  private Player viewer;
  private String title;
  private Inventory nametagInv;
  public NameInputUI(Player playerFor, String title) {
    viewer = playerFor;
    this.title = title;
    nametagInv = Bukkit.createInventory(viewer, InventoryType.ANVIL, title);
    
    // TODO IMPORTANT - for some reason this isn't working...?
    nametagInv.setItem(1, Globals.getNamedItem(Material.NAME_TAG, "rename..."));
    // TODO - add event listeners to prevent pulling out this name tag!
  }

  @Override
  public Inventory getTopInventory() {
    return nametagInv;
  }

  @Override
  public Inventory getBottomInventory() {
    return viewer.getInventory();
  }

  @Override
  public HumanEntity getPlayer() {
    return viewer;
  }

  @Override
  public InventoryType getType() {
    return InventoryType.ANVIL;
  }

  @Override
  public String getTitle() {
    return title;
  }

}
