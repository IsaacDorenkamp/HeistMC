package anti.projects.heistmc.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class SingleViewMenu extends Menu {
  
  private int slots;
  private Inventory inv;
  private String title;
  private InventoryView view;
  final private Player pFor;
  private MenuPage menu;
  
  private boolean showing = false;
  
  public SingleViewMenu(String title, int slots, final Player pFor, MenuPage base) {
    this.slots = slots;
    this.title = title;
    this.menu = base;
    this.pFor = pFor;
    inv = Bukkit.createInventory(null, slots);
    
    view = new InventoryView() {

      @Override
      public Inventory getTopInventory() {
        return SingleViewMenu.this.inv;
      }

      @Override
      public Inventory getBottomInventory() {
        return SingleViewMenu.this.pFor.getInventory();
      }

      @Override
      public HumanEntity getPlayer() {
        return SingleViewMenu.this.pFor;
      }

      @Override
      public InventoryType getType() {
        return InventoryType.CHEST;
      }

      @Override
      public String getTitle() {
        return SingleViewMenu.this.title;
      }
      
    };
    
    addMenuListener(new MenuListener() {

      public boolean itemSelected(int slot, Material icon, String name, boolean isShift) {
        pFor.playSound(pFor.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        return menu.itemSelected(slot, icon, name, isShift);
      }
      
    });
  }
  
  public SingleViewMenu(String title, int slots, Player pFor) {
    this(title, slots, pFor, new MenuPage());
  }
  
  public Player getViewer() {
    return pFor;
  }
  
  public MenuPage getMenu() {
    return menu;
  }
  
  public void onShow() {
    menu.activate(inv);
    showing = true;
  }
  
  public void onExit() {
    menu.deactivate();
    inv.clear();
    showing = false;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getTitle() {
    return title;
  }

  public InventoryView getInventoryView() {
    return view;
  }

  public int getSlots() {
    return slots;
  }

}
