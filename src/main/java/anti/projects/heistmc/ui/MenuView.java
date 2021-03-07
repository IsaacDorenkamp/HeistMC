package anti.projects.heistmc.ui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public interface MenuView {
  public void onInventoryClick(InventoryClickEvent evt);
  public InventoryView getInventoryView();
  public int getSlots();
  public void onShow();
  public void onExit();
}
