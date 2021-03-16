package anti.projects.heistmc.mission;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import anti.projects.heistmc.stages.BuildWorld;
import anti.projects.heistmc.stages.HeistWorld;

public abstract class MissionObjective {
  protected String name;
  protected String description;
  public MissionObjective(String name, String description) {
    this.name = name;
    this.description = description;
  }
  
  public MissionObjective() {
    this("", "");
  }
  
  public String getName() {
    return name;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void initialize(HeistWorld forWorld) {}
  public void cleanup(HeistWorld forWorld) {}
  
  public abstract boolean isComplete(HeistWorld forWorld); // speaks for itself
  public boolean tryConfigAction(PlayerInteractEvent evt, BuildWorld bw) {
    return tryConfigAction(evt);
  }; // returns whether objective configuration is complete
  public abstract boolean tryConfigAction(PlayerInteractEvent evt);
  public abstract boolean onStartConfig(Player p); // returns whether initiating configuration was successful
  public abstract Material getDisplayIcon();
  
  public final void saveData(DataOutputStream out) throws IOException {
    String type = this.getClass().getName();
    out.writeUTF(type);
    save(out);
  }
  public abstract void save(DataOutputStream out) throws IOException;
  public abstract void load(DataInputStream in) throws IOException;
}
