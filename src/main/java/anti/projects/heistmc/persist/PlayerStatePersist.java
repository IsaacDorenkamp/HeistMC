package anti.projects.heistmc.persist;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;

public class PlayerStatePersist {
  public static class PlayerState {
    public double health;
    public int food;
    
    public PlayerState(double health, int food) {
      this.health = health;
      this.food = food;
    }
  }
  
  private HashMap<UUID, PlayerState> entries = new HashMap<UUID, PlayerState>();
  
  public PlayerStatePersist() {}
  private PlayerStatePersist(HashMap<UUID, PlayerState> entries) {
    this.entries = entries;
  }
  
  public void setPlayerState(Player p) {
    PlayerState state = new PlayerState(p.getHealth(), p.getFoodLevel());
    entries.put(p.getUniqueId(), state);
  }
  
  public boolean hasEntry(Player p) {
    return entries.containsKey(p.getUniqueId());
  }
  
  public void popPlayerState(Player p) {
    PlayerState state = entries.get(p.getUniqueId());
    p.setHealth(state.health);
    p.setFoodLevel(state.food);
  }
  
  public void save(File f) throws IOException {
    DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
    dos.writeInt(entries.size());
    for (UUID key : entries.keySet()) {
      dos.writeUTF(key.toString());
      PlayerState state = entries.get(key);
      dos.writeDouble(state.health);
      dos.writeInt(state.food);
    }
    dos.close();
  }
  
  public static PlayerStatePersist load(File f) throws IOException, IllegalArgumentException {
    DataInputStream dis = new DataInputStream(new FileInputStream(f));
    int entries = dis.readInt();
    HashMap<UUID, PlayerState> data = new HashMap<UUID, PlayerState>();
    for (int i = 0; i < entries; i++) {
      String key = dis.readUTF();
      double health = dis.readDouble();
      int food = dis.readInt();
      data.put(UUID.fromString(key), new PlayerState(health, food));
    }
    return new PlayerStatePersist(data);
  }
}
