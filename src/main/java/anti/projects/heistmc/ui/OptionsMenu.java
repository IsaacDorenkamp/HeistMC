package anti.projects.heistmc.ui;

import java.util.ArrayList;

import org.bukkit.Material;

// basically the Minecraft version of context menus.
public class OptionsMenu {
  public static final class OptionsMenuEntry {
    public Material icon;
    public String label;
    public MenuItemListener onclick;
    
    public OptionsMenuEntry(Material icon, String label, MenuItemListener onclick) {
      this.icon = icon;
      this.label = label;
      this.onclick = onclick;
    }
  }
  
  private ArrayList<OptionsMenuEntry> options = new ArrayList<OptionsMenuEntry>();
  
  public OptionsMenu() {}
  
  public void addEntry(OptionsMenuEntry entry) {
    options.add(entry);
  }
  public void addEntry(Material icon, String label, MenuItemListener onclick) {
    addEntry(new OptionsMenuEntry(icon, label, onclick));
  }
  
  public int size() {
    return options.size();
  }
  
  public OptionsMenuEntry getEntry(int index) {
    return options.get(index);
  }
}
