package anti.projects.heistmc.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import anti.projects.heistmc.Globals;
import net.md_5.bungee.api.ChatColor;

public class MenuPage implements MenuListener {
  
  private static final class MenuEntry {
    public Material type;
    public String name;
    public String lore;
    public MenuItemListener onClick;
    
    public MenuEntry(Material type, String name, String lore, MenuItemListener click) {
      this.type = type;
      this.name = name;
      this.lore = lore;
      this.onClick = click;
    }
  }
  
  private HashMap<Integer, MenuEntry> entries;
  private boolean active = false;
  protected Inventory display = null;
  public MenuPage() {
    entries = new HashMap<Integer, MenuEntry>();
  }
  
  public void addItem(int slot, Material type, String name, String lore, MenuItemListener onSelect) {
    entries.put(slot, new MenuEntry(type, name, lore, onSelect));
  }
  
  public void addItem(int slot, Material type, String name, MenuItemListener onSelect) {
    addItem(slot, type, name, null, onSelect);
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
          if (ent.lore != null) {
            String[] lines = ent.lore.split("\n");
            for (int i = 0; i < lines.length; i++) {
              lines[i] = ChatColor.RESET + lines[i];
            }
            List<String> l = Arrays.asList(lines);
            ItemMeta im = toPut.getItemMeta();
            im.setLore(l);
            toPut.setItemMeta(im);
          }
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
  
  public boolean itemSelected(int slot, Material type, String name, boolean isShift) {
    MenuEntry entry = entries.get(slot);
    if (entry != null) {
      MenuItemListener click = entry.onClick;
      if (click != null) {
        boolean makeSound;
        if (!isShift) {
          makeSound = click.whenSelected();
        } else {
          makeSound = click.whenShiftSelected();
        }
        return makeSound;
      } else {
        return false;
      }
    } else {
      onEmptySlotSelected();
      return false;
    }
  }
}
