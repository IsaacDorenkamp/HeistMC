package anti.projects.heistmc.ui;

public abstract class MenuItemListener {
  public boolean whenSelected() {
    onSelected();
    return true;
  }
  public void onSelected() {}
  public boolean whenShiftSelected() {
    onShiftSelected();
    return true;
  }
  public void onShiftSelected() {}
}
