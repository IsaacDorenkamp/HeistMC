package anti.projects.heistmc.ui;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;

public class ConfirmView extends InteractiveView {
  
  private Player viewer;
  private Inventory topView;
  private String question;
  private Consumer<Boolean> callback;
  private boolean answered = false;
  
  public ConfirmView(Player viewer, String question, Consumer<Boolean> callback) {
    topView = Bukkit.createInventory(viewer, InventoryType.HOPPER, question);
    
    topView.setContents(new ItemStack[] {
        Globals.getNamedItem(Material.BLACK_STAINED_GLASS_PANE, " "),
        Globals.getNamedItem(Material.LIME_STAINED_GLASS_PANE, "Yes"),
        Globals.getNamedItem(Material.BLACK_STAINED_GLASS_PANE, " "),
        Globals.getNamedItem(Material.RED_STAINED_GLASS_PANE, "No"),
        Globals.getNamedItem(Material.BLACK_STAINED_GLASS_PANE, " ")
    });
    
    this.viewer = viewer;
    this.question = question;
    this.callback = callback;
  }

  @Override
  public Inventory getTopInventory() {
    return topView;
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
    return InventoryType.HOPPER;
  }

  @Override
  public String getTitle() {
    return question;
  }
  
  public Consumer<Boolean> getCallback() {
    return callback;
  }
  
  public void inventoryClicked(InventoryClickEvent evt) {
    if (answered) {
      evt.setResult(Result.DENY);
      evt.setCancelled(true);
      return;
    }
    ItemStack is = evt.getClickedInventory().getItem(evt.getSlot());
    boolean close = true;
    if (Globals.isNamedItem(is, Material.LIME_STAINED_GLASS_PANE, "Yes")) {
      answered = true;
      callback.accept(true);
    } else if (Globals.isNamedItem(is, Material.RED_STAINED_GLASS_PANE, "No")) {
      answered = true;
      callback.accept(false);
    } else {
      close = false;
    }
    
    if (close) evt.getWhoClicked().closeInventory();
    
    evt.setResult(Result.DENY);
    evt.setCancelled(true);
  }
}
