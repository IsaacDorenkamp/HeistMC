package anti.projects.heistmc.ui;

import org.bukkit.Material;

public interface MenuListener {
  public boolean itemSelected(int slot, Material icon, String name, boolean isShift);
}
