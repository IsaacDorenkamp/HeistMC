package anti.projects.heistmc.ui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public abstract class InteractiveView extends InventoryView {
  public abstract void inventoryClicked(InventoryClickEvent evt);
}
