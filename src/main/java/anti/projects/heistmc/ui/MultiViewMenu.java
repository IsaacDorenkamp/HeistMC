package anti.projects.heistmc.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class MultiViewMenu extends Menu {
  
  private int slots;
  private Inventory inv;
  private String title;
  private InventoryView view;
  
  private Player pFor;
  
  private MenuPage base;
  private List<MenuPage> pushedViews;
  
  private boolean showing = false;
  
  public MultiViewMenu(String title, int slots, Player pFor, MenuPage base) {
    this.slots = slots;
    this.title = title;
    this.base = base;
    this.pushedViews = new ArrayList<MenuPage>();
    this.pFor = pFor;
    inv = Bukkit.createInventory(null, slots);
    
    view = new InventoryView() {

      @Override
      public Inventory getTopInventory() {
        return MultiViewMenu.this.inv;
      }

      @Override
      public Inventory getBottomInventory() {
        return MultiViewMenu.this.pFor.getInventory();
      }

      @Override
      public HumanEntity getPlayer() {
        return MultiViewMenu.this.pFor;
      }

      @Override
      public InventoryType getType() {
        return InventoryType.CHEST;
      }

      @Override
      public String getTitle() {
        return MultiViewMenu.this.title;
      }
      
    };
    
    addMenuListener(new MenuListener() {

      public void itemSelected(int slot, Material icon, String name, boolean isShift) {
        if (pushedViews.size() == 0) {
          MultiViewMenu.this.base.itemSelected(slot, icon, name, isShift);
        } else {
          MenuPage current = pushedViews.get(pushedViews.size() - 1);
          current.itemSelected(slot, icon, name, isShift);
        }
      }
      
    });
  }
  
  public MultiViewMenu(String title, int slots, Player pFor) {
    this(title, slots, pFor, new MenuPage());
  }
  
  public Player getViewer() {
    return pFor;
  }
  
  public MenuPage getBaseMenu() {
    return base;
  }
  
  public void onShow() {
    base.activate(inv);
    showing = true;
  }
  
  public void onExit() {
    pushedViews.clear();
    inv.clear();
    showing = false;
  }
  
  public MenuPage getCurrent() {
    if (pushedViews.size() == 0) {
      return base;
    } else {
      return pushedViews.get(pushedViews.size() - 1);
    }
  }
  
  public boolean pushView(MenuPage next) {
    if (!showing) {
      return false;
    } else {
      MenuPage page = getCurrent();
      page.deactivate();
      pushedViews.add(next);
      next.activate(inv);
      return true;
    }
  }
  
  public boolean popView() {
    if (pushedViews.size() == 0) {
      return false;
    } else {
      getCurrent().deactivate();
      pushedViews.remove(pushedViews.size() - 1);
      if (pushedViews.size() == 0) {
        base.activate(inv);
      } else {
        MenuPage prev = pushedViews.get(pushedViews.size() - 1);
        prev.activate(inv);
      }
      return true;
    }
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
