package anti.projects.heistmc.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class Menu implements MenuView {
  
  private List<MenuListener> listeners;
  protected Menu() {
    listeners = new ArrayList<MenuListener>();
  }
  
  public void addMenuListener(MenuListener ml) {
    listeners.add(ml);
  }
  
  public boolean removeMenuListener(MenuListener ml) {
    return listeners.remove(ml);
  }

  public void onInventoryClick(InventoryClickEvent evt) {
    int slot = evt.getSlot();
    ItemStack selected = evt.getCurrentItem();
    Material type = selected == null ? null : selected.getType();
    String name = selected == null ? null : selected.getItemMeta().getDisplayName();
    for (MenuListener ml : listeners) {
      ml.itemSelected(slot, type, name);
    }
    evt.setCancelled(true);
  }

}
