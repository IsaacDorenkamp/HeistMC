package anti.projects.heistmc.api;

import org.bukkit.Material;

public class BreakableBlock {
  private int x;
  private int y;
  private int z;
  private Material type;
  
  public BreakableBlock(int x, int y, int z, Material type) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.type = type;
  }
  
  public int getX() {
    return x;
  }
  
  public int getY() {
    return y;
  }
  
  public int getZ() {
    return z;
  }
  
  public void setX(int x) {
    this.x = x;
  }
  
  public void setY(int y) {
    this.y = y;
  }
  
  public void setZ(int z) {
    this.z = z;
  }
  
  public void setCoords(int x, int y, int z) {
    setX(x);
    setY(y);
    setZ(z);
  }
  
  public Material getType() {
    return type;
  }
  
  public void setType(Material type) {
    this.type = type;
  }
}
