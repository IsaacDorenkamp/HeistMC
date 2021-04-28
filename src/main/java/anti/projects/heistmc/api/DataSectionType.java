package anti.projects.heistmc.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;

import anti.projects.heistmc.stages.HeistWorldData;

public enum DataSectionType {
  BREAKABLE_BLOCKS, UPGRADE_ANVILS;
  
  public void load(DataInputStream src, HeistWorldData loadInto) throws IOException {
    int amount;
    switch(this) {
    case BREAKABLE_BLOCKS:
      amount = src.readInt();
      for (int i = 0; i < amount; i++) {
        int x = src.readInt();
        int y = src.readInt();
        int z = src.readInt();
        Material m = Material.valueOf(src.readUTF());
        loadInto.addBreakableBlock(new BreakableBlock(x, y, z, m));
      }
      break;
    case UPGRADE_ANVILS:
      amount = src.readInt();
      for (int i = 0; i < amount; i++) {
        int x = src.readInt();
        int y = src.readInt();
        int z = src.readInt();
        loadInto.addUpgradeAnvil(new Location(null, x, y, z));
      }
      break;
    }
  }
  
  public void save(DataOutputStream dest, HeistWorldData saveFrom) throws IOException {
    int amount;
    switch(this) {
    case BREAKABLE_BLOCKS:
      List<BreakableBlock> blocks = saveFrom.getBreakableBlocks();
      amount = blocks.size();
      dest.writeInt(amount);
      
      for (BreakableBlock block : blocks) {
        dest.writeInt(block.getX());
        dest.writeInt(block.getY());
        dest.writeInt(block.getZ());
        dest.writeUTF(block.getType().toString());
      }
      
      break;
    case UPGRADE_ANVILS:
      List<Location> anvs = saveFrom.getUpgradeAnvils();
      amount = anvs.size();
      dest.writeInt(amount);
      for (Location anv : anvs) {
        dest.writeInt(anv.getBlockX());
        dest.writeInt(anv.getBlockY());
        dest.writeInt(anv.getBlockZ());
      }
      break;
    }
  }
}
