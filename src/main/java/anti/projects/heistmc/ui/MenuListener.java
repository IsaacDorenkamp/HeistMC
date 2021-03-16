package anti.projects.heistmc.ui;

import org.bukkit.Material;

public interface MenuListener {
  public void itemSelected(int slot, Material icon, String name, boolean isShift);
}
