package anti.projects.heistmc.ui;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;

public class MenuPage implements MenuListener {
  
  private static final class MenuEntry {
    public Material type;
    public String name;
    public MenuItemListener onClick;
    
    public MenuEntry(Material type, String name, MenuItemListener click) {
      this.type = type;
      this.name = name;
      this.onClick = click;
    }
  }
  
  private HashMap<Integer, MenuEntry> entries;
  private boolean active = false;
  protected Inventory display = null;
  public MenuPage() {
    entries = new HashMap<Integer, MenuEntry>();
  }
  
  public void addItem(int slot, Material type, String name, MenuItemListener onSelect) {
    entries.put(slot, new MenuEntry(type, name, onSelect));
  }
  
  public void clearItems() {
    entries.clear();
  }
  
  public void activate(Inventory inv) {
    active = true;
    display = inv;
    render();
  }
  
  protected void render() {
    if (active) {
      display.clear();
      for (Integer slot : entries.keySet()) {
        MenuEntry ent = entries.get(slot);
        if (ent.type != Material.AIR) { 
          ItemStack toPut = Globals.getNamedItem(ent.type, ent.name);
          display.setItem(slot, toPut);
        } else {
          display.setItem(slot, new ItemStack(Material.AIR, 0));
        }
      }
    }
  }
  
  public void deactivate() {
    active = false;
    display = null;
  }
  
  public void onEmptySlotSelected() {}
  
  public void itemSelected(int slot, Material type, String name) {
    MenuEntry entry = entries.get(slot);
    if (entry != null) {
      MenuItemListener click = entry.onClick;
      if (click != null) {
        click.onSelected();
      }
    } else {
      onEmptySlotSelected();
    }
  }
}
