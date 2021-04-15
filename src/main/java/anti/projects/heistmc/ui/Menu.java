package anti.projects.heistmc.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
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
    boolean makeSound = false;
    for (MenuListener ml : listeners) {
      makeSound = makeSound || ml.itemSelected(slot, type, name, evt.isShiftClick());
    }
    if (makeSound) {
      for (HumanEntity he : evt.getViewers()) {
        if (he instanceof Player) {
          Player p = (Player)he;
          p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
      }
    }
    evt.setCancelled(true);
  }

}
