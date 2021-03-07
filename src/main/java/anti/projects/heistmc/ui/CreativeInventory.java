package anti.projects.heistmc.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;

public class CreativeInventory extends MenuPage {
  
  public static final Material[] REDSTONE = new Material[] {
      Material.DISPENSER, Material.NOTE_BLOCK, Material.PISTON, Material.STICKY_PISTON, Material.TNT,
      Material.LEVER, Material.STONE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
      Material.DARK_OAK_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE,
      Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE,
      Material.STONE_PRESSURE_PLATE, Material.REDSTONE_TORCH // TODO - finish
  };
  
  public static final int SIZE = 54;
  public static final int CAPACITY = SIZE - 9;
  
  private static final List<Material> ALL_BLOCKS = new ArrayList<Material>();
  static {
    for (Material m : Material.values()) {
      if (m.isBlock() && !m.isAir()) {
        ALL_BLOCKS.add(m);
      }
    }
  }
  private static final int MAX_PAGE = 14; // Magic, cuz calculating wasn't really working
  
  private int page = 0;
  private HumanEntity viewer;
  public CreativeInventory(HumanEntity viewer, final MultiViewMenu mvm) {
    this.viewer = viewer;
    
    addItem(CAPACITY, Material.END_CRYSTAL, Globals.STRING_PREVIOUS_PAGE, new MenuItemListener() {
      public void onSelected() {
        setPage(page - 1);
      }
    });
    
    addItem(SIZE - 1, Material.END_CRYSTAL, Globals.STRING_NEXT_PAGE, new MenuItemListener() {
      public void onSelected() {
        setPage(page + 1);
      }
    });
    
    if (mvm != null) {
      addItem(SIZE - 5, Material.BOOK, Globals.STRING_BACK, new MenuItemListener() {
        public void onSelected() {
          mvm.popView();
        }
      });
    }
    
    setPage(0);
  }
  
  public CreativeInventory(HumanEntity viewer) {
    this(viewer, null);
  }
  
  public boolean setPage(int page) {
    if (page > MAX_PAGE || page < 0) {
      return false;
    } else {
      this.page = page;
      
      for (int i = 0; i < CAPACITY; i++) {
        final Material m = ALL_BLOCKS.get(page * CAPACITY + i);
        addItem(i, m, Globals.getMaterialName(m), new MenuItemListener() {
          public void onSelected() {
            viewer.setItemOnCursor(new ItemStack(m, 1));
          }
        });
      }
      render();
      
      return true;
    }
  }
  
  public int getPage() {
    return page;
  }
  
}
