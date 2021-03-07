package anti.projects.heistmc.ui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.MapManager;

public class MapSelectMenu extends SingleViewMenu {
  
  private MapManager maps;
  private Consumer<String> cbk;
  
  public MapSelectMenu(MapManager mgr, Player playerFor, Consumer<String> onSelect) {
    super(Globals.STRING_SELECT_MAP, 9 * 5, playerFor);
    maps = mgr;
    cbk = onSelect;
    construct();
  }
  
  private void construct() {
    MenuPage pg = getMenu();
    List<Object> mapnames = maps.getMapNames().stream().sorted().filter(new Predicate<String>() {

      public boolean test(String t) {
        return !t.equals(Globals.ID_LOBBY);
      }
      
    }).collect(Collectors.toList());
    for (int i = 0; i < getSlots(); i++) {
      int row = i / 9;
      int col = i % 9;
      
      if (row == 0 || col == 0 || row == 4 || col == 8) {
        pg.addItem(i, Material.BLACK_STAINED_GLASS_PANE, " ", null);
      } else {
        int transRow = row - 1;
        int transCol = col - 1;
        int transIdx = transRow * 7 + transCol;
        if (mapnames.size() > transIdx) {
          final String name = mapnames.get(transIdx).toString();
          pg.addItem(i, Material.FILLED_MAP, name, new MenuItemListener() {
            public void onSelected() {
              cbk.accept(name);
            }
          });
        } else {
          pg.addItem(i, Material.MAP, "No map here, yet!", null);
        }
      }
    }
  }
}
