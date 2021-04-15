package anti.projects.heistmc.ui;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.mission.KillObjective;
import anti.projects.heistmc.mission.MissionObjective;
import anti.projects.heistmc.stages.ObjectiveSetTracker;

public class MissionObjectiveListMenu extends MenuPage {
  
  public static final int PAGE_SIZE = 7;
  
  private MultiViewMenu parent;
  private List<MissionObjective> ref;
  private int page = 0;
  private int toUse = -1;
  
  private Consumer<MissionObjective> onDelete = null;
  private Consumer<KillObjective> onSelectKill = null;
  
  private ObjectiveSetTracker tracker;
  private MissionObjective selected = null;
  
  private MenuItemListener unuse = new MenuItemListener() { public boolean whenSelected() { use(-1); return false; } };
  
  public MissionObjectiveListMenu(MultiViewMenu parent, List<MissionObjective> ref, ObjectiveSetTracker tracker) {
    this.parent = parent;
    this.ref = ref;
    this.tracker = tracker;
    construct();
  }
  
  public void setDeleteCallback(Consumer<MissionObjective> onDelete) {
    this.onDelete = onDelete;
  }
  
  public void setKillSelectCallback(Consumer<KillObjective> onSelectKill) {
    this.onSelectKill = onSelectKill;
  }
  
  @Override
  public void onEmptySlotSelected() {
    unuse.whenSelected();
  }
  
  private void construct() {
    for (int i = 0; i < 45; i++) {
      addItem(i, Material.BLACK_STAINED_GLASS_PANE, " ", unuse);
    }

    addItem(18, Material.END_CRYSTAL, Globals.STRING_PREVIOUS_PAGE, new MenuItemListener() {
      public void onSelected() {
        setPage(page - 1);
      }
    });

    addItem(26, Material.END_CRYSTAL, Globals.STRING_NEXT_PAGE, new MenuItemListener() {
      public void onSelected() {
        setPage(page + 1);
      }
    });
    
    addItem(51, Material.WRITABLE_BOOK, Globals.STRING_EDIT, new MenuItemListener() {
      public void onSelected() {
        if (toUse != -1) {
          final int modify = toUse;
          MissionObjective obj = ref.get(toUse);
          if (obj instanceof KillObjective && onSelectKill != null) {
            onSelectKill.accept((KillObjective)obj);
          }
          tracker.startConfiguring(parent.getViewer(), ref.get(toUse), new Consumer<MissionObjective>() {
            public void accept(MissionObjective obj) {
              ref.set(modify, obj);
            }
          });
          parent.getViewer().closeInventory();
        }
      }
    });
    
    addItem(49, Material.BOOK, Globals.STRING_BACK, new MenuItemListener() {
      public void onSelected() {
        parent.popView();
      }
    });
    
    addItem(47, Material.BARRIER, Globals.STRING_DELETE, new MenuItemListener() {
      public void onSelected() {
        if (toUse != -1) {
          if (onDelete != null) {
            onDelete.accept(ref.get(toUse));
          }
          ref.remove(toUse);
          use(-1);
          boolean isGoodPage = setPage(page);
          if (!isGoodPage) {
            setPage(page - 1);
          }
        }
      }
    });
    
    setPage(0);
  }
  
  private static final int SELECTION_DISPLAY_INDEX = 13;
  private void use(int idx) {
    if (idx >= 0 && idx < ref.size()) {
      toUse = idx;
      selected = ref.get(toUse);
      if (selected instanceof OptionsMenuOwner) {
        OptionsMenu opt = ((OptionsMenuOwner) selected).getOptionsMenu(parent.getViewer());
        setOptionsMenu(opt);
      } else {
        setOptionsMenu(null);
      }
    } else if (idx == -1) {
      toUse = -1;
      selected = null;
      setOptionsMenu(null);
    }
    
    if (selected != null) {
      addItem(SELECTION_DISPLAY_INDEX, selected.getDisplayIcon(), selected.getName(), null);
    } else {
      addItem(SELECTION_DISPLAY_INDEX, Material.BLACK_STAINED_GLASS_PANE, " ", null);
    }
    
    render();
  }
  
  public boolean setPage(int page) {
    int maxPage = ref.size() == 0 ? 0 :(int)Math.floor((float)(ref.size() - 1) / PAGE_SIZE);
    if (!(page < 0 || page > maxPage)) {
      this.page = page;
      int startIdx = (page * PAGE_SIZE);
      for (int i = startIdx; i < startIdx + PAGE_SIZE; i++) {
        if (i >= ref.size()) {
          addItem(19 + (i - startIdx), Material.WHITE_STAINED_GLASS_PANE, "Empty", unuse);
          continue;
        }
        MissionObjective obj = ref.get(i);
        addItem(19 + (i - startIdx), obj.getDisplayIcon(), obj.getName(), obj.getDescription(), new UseListener(i));
      }
      render();
      return true;
    } else {
      return false;
    }
  }
  
  private void swap(int first, int second) {
    MissionObjective temp = ref.get(first);
    ref.set(first, ref.get(second));
    ref.set(second, temp);
    setPage(page); // reload the page
    use(second);
//    render();
  }
  
  private static final int OPTIONS_MENU_START_INDEX = 29;
  private void setOptionsMenu(OptionsMenu menu) {
    if (menu == null || (menu != null && menu.size() == 0)) {
      for (int i = OPTIONS_MENU_START_INDEX; i < OPTIONS_MENU_START_INDEX + 5; i++) {
        addItem(i, Material.BLACK_STAINED_GLASS_PANE, " ", unuse);
      }
    } else {
      int items = Math.min(menu.size(), 5);
      int startIdx = (OPTIONS_MENU_START_INDEX + 2) - ((menu.size() - 1) / 2);
      for (int i = OPTIONS_MENU_START_INDEX; i < OPTIONS_MENU_START_INDEX + 5; i++) {
        if (i < startIdx || i > (startIdx + items - 1)) {
          addItem(i, Material.AIR, "", null);
        } else {
          final OptionsMenu.OptionsMenuEntry ent = menu.getEntry(i - startIdx);
          addItem(i, ent.icon, ent.label, new MenuItemListener() {
            public void onSelected() {
              ent.onclick.onSelected();
              setOptionsMenu(null);
              use(-1);
            }
          });
        }
      }
    }
    
    render();
  }
  
  private final class UseListener extends MenuItemListener {
    private final int idx;
    public UseListener(int idx) {
      this.idx = idx;
    }
    public void onSelected() {
      use(idx);
    }
    
    @Override
    public void onShiftSelected() {
      if (toUse != -1) {
        swap(toUse, idx);
      }
    }
  }
}
