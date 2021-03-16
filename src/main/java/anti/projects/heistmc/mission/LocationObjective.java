package anti.projects.heistmc.mission;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.stages.HeistWorld;

public class LocationObjective extends MissionObjective {
  private int x;
  private int y;
  private int z;
  private int radius;
  public LocationObjective(String name, int x, int y, int z, int radius) {
    super(name, String.format("Go to (%d, %d, %d)", x, y, z));
    this.x = x;
    this.y = y;
    this.z = z;
    this.radius = radius;
  }
  
  public LocationObjective(String name) {
    this(name, 0, 0, 0, 2);
  }
  
  public LocationObjective() {
    this("");
  }
  
  private int particles = -1;
  
  private static double calcoffset(Random r) {
    return 2 * (r.nextDouble() - 0.5);
  }
  
  @Override
  public void initialize(HeistWorld worldFor) {
    final World w = worldFor.getWorld();
    final Location loc = new Location(w, x + 0.5, y + 1, z + 0.5);
    final Random offset = new Random();
    particles = HeistMC.scheduleRepeatedTask(new Runnable() {
      public void run() {
        Location spawn = loc.clone();
        spawn.add(calcoffset(offset), calcoffset(offset), calcoffset(offset));
        w.spawnParticle(Particle.REDSTONE, spawn, 5, new Particle.DustOptions(Color.ORANGE, 1f));
      }
    }, 5L);
  }
  
  @Override
  public void cleanup(HeistWorld worldFor) {
    if (particles != -1) {
      HeistMC.descheduleRepeatedTask(particles);
    }
  }
  
  public boolean isComplete(HeistWorld worldFor) {
    boolean inArea = true;
    double centerX = x + 0.5;
    double centerZ = z + 0.5;
    double centerY = y - 0.5;
    for (Player p : worldFor.getPlayers()) {
      Location ploc = p.getLocation();
      double distance = Math.sqrt(Math.pow(centerX - ploc.getX(), 2) + Math.pow(centerY - ploc.getY(), 2)
        + Math.pow(centerZ - ploc.getZ(), 2));
      if (distance > radius) {
        inArea = false;
        break;
      }
    }
    return inArea;
  }

  @Override
  public boolean tryConfigAction(PlayerInteractEvent evt) {
    Player p = evt.getPlayer();
    ItemStack holding = evt.getItem();
    if (Globals.isNamedItem(holding, Material.COMPASS, Globals.STRING_DESTINATION)) {
      Location loc = p.getLocation().getBlock().getLocation(); // snap location to block
      this.x = (int)loc.getX();
      this.y = (int)loc.getY();
      this.z = (int)loc.getZ();
      p.getInventory().remove(holding);
      cfgmeta();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onStartConfig(Player p) {
    ItemStack compass = Globals.getNamedItem(Material.COMPASS, Globals.STRING_DESTINATION);
    boolean success = p.getInventory().addItem(compass).size() == 0;
    if (success) {
      int index = p.getInventory().first(compass);
      if (index >= 0 && index <= 8) p.getInventory().setHeldItemSlot(index);
      MessageUtil.send(p, "Right click while holding the 'Set Destination' stick to set the objective's destination.");
    } else {
      MessageUtil.send(p, ChatColor.RED + "Your inventory is full, so could not start configuring the objective. Please free up an inventory slot.");
    }
    return success;
  }
  
  @Override
  public Material getDisplayIcon() {
    return Material.COMPASS;
  }
  
  @Override
  public String toString() {
    return String.format("Location: (%d, %d, %d)", x, y, z);
  }
  
  private void cfgmeta() {
    this.name = "GO";
    this.description = String.format("Go to the coordinates (%d, %d, %d)", x, y, z);
  }
  
  @Override
  public void save(DataOutputStream dos) throws IOException {
    // 4 bytes for x, 4 bytes for y, 4 bytes for z, 4 bytes for radius
    dos.writeInt(x);
    dos.writeInt(y);
    dos.writeInt(z);
    dos.writeInt(radius);
  }
  
  @Override
  public void load(DataInputStream dis) throws IOException {
    int _x = dis.readInt();
    int _y = dis.readInt();
    int _z = dis.readInt();
    int _r = dis.readInt();
    
    this.x = _x;
    this.y = _y;
    this.z = _z;
    this.radius = _r;
    
    cfgmeta();
    
    this.description = String.format("Go to (%d, %d, %d)", x, y, z);
  }
}
