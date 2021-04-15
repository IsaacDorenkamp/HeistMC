package anti.projects.heistmc;

import java.util.HashMap;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Globals {
  public static final int MAX_LOBBIES = 1;
  public static final int MAX_PLAYERS = 4;
  
  public static final String PLUGIN_NAME = "HeistMC";
  
  public static final String PERMISSION_BUILD = "heistmc.create";
  
  public static final String STRING_EXIT = "Exit";
  public static final String STRING_START_HEIST = "Start Heist";
  public static final String STRING_BUILD_MENU = "Build Menu";
  
  public static final String STRING_PREVIOUS_PAGE = "Previous Page";
  public static final String STRING_NEXT_PAGE = "Next Page";
  
  public static final String STRING_BACK = "Back";
  
  public static final String STRING_MAKE_OBJECTIVE  = "Add New Mission Objective";
  public static final String STRING_VIEW_OBJECTIVES = "View Mission Objectives";
  
  public static final String STRING_LOCATION_OBJECTIVE = "Location Objective";
  public static final String STRING_ITEM_OBJECTIVE = "Obtain Item Objective";
  public static final String STRING_KILL_OBJECTIVE = "Kill Objective";
  
  public static final String STRING_SET_LOCATION_NAME = "Set Location Name (Optional)";
  public static final String STRING_DESTINATION = "Set Destination";
  public static final String STRING_EDIT = "Edit Objective";
  public static final String STRING_DELETE = "Delete Objective";
  public static final String STRING_NAME_ITEM = "Name Item";
  public static final String STRING_FINISH = "Finish Configuring";
  
  public static final String STRING_REDSTONE = "Redstone";
  public static final String STRING_CREATE_BLOCK = "Spawn Block Under You";
  
  public static final String STRING_SELECT_MAP = "Select Map";
  public static final String STRING_SET_SPAWN = "Set World Spawn";
  
  public static final String STRING_SELECT_TYPE = "Select Enemy Type";
  public static final String STRING_PLACE_ONE = "Spawn One";
  public static final String STRING_CONFIGURE_EQUIPMENT = "Configure Equipment";
  public static final String STRING_HIDE_PLACEHOLDERS = "Hide Placeholder Mobs";
  public static final String STRING_REMOVE_MOB = "Remove Enemy";
  
  public static final String ID_LOBBY = "lobby";
  
  public static final String HEIST_WORLD_DATA_FILE = "heist.dat";
  public static final String MAP_MASTER_FILE = "maps.txt";
  public static final String INVENTORY_PERSIST_FILE = "inventory_persist.dat";
  public static final String PLAYER_STATE_PERSIST_FILE = "player_state_persist.dat";
  
  public static ItemStack getNamedItem(Material type, String name) {
    ItemStack stack = new ItemStack(type, 1);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(name);
    stack.setItemMeta(meta);
    return stack;
  }
  
  public static boolean isNamedItem(ItemStack test, Material type, String name) {
    return test != null && test.getType().equals(type) && test.getItemMeta().getDisplayName().equals(name)
        && test.getAmount() == 1;
  }
  
  public static ItemStack getLeaveStar() {
    return getNamedItem(Material.NETHER_STAR, STRING_EXIT);
  }
  
  public static ItemStack getStartArrow() {
    return getNamedItem(Material.ARROW, STRING_START_HEIST);
  }
  
  public static ItemStack getMenuBook() {
    return getNamedItem(Material.BOOK, STRING_BUILD_MENU);
  }
  
  public static Integer min(Set<Integer> set) {
    Integer min = null;
    for (Integer i : set) {
      if (min == null || i < min) {
        min = i;
      }
    }
    return min;
  }
  
  private static HashMap<Material, String> nameCache = new HashMap<Material, String>();
  public static String getMaterialName(Material m) {
    String name = nameCache.get(m);
    if (name == null) {
      String ret = _format(m.toString());
      nameCache.put(m, ret);
      return ret;
    } else {
      return name;
    }
  }
  
  private static String _format(String s) {
    String[] parts = s.split("_");
    String[] newParts = new String[parts.length];
    int idx = 0;
    for (String part : parts) {
      newParts[idx++] = (part.substring(0, 1).toUpperCase()) + (part.substring(1).toLowerCase());
    }
    return String.join(" ", newParts);
  }
}
