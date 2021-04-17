package anti.projects.heistmc.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import org.bukkit.Material;

import anti.projects.heistmc.stages.HeistWorldData;

public enum DataSectionType {
  BREAKABLE_BLOCKS;
  
  public void load(DataInputStream src, HeistWorldData loadInto) throws IOException {
    switch(this) {
    case BREAKABLE_BLOCKS:
      int amount = src.readInt();
      for (int i = 0; i < amount; i++) {
        int x = src.readInt();
        int y = src.readInt();
        int z = src.readInt();
        Material m = Material.valueOf(src.readUTF());
        loadInto.addBreakableBlock(new BreakableBlock(x, y, z, m));
      }
      break;
    }
  }
  
  public void save(DataOutputStream dest, HeistWorldData saveFrom) throws IOException {
    switch(this) {
    case BREAKABLE_BLOCKS:
      List<BreakableBlock> blocks = saveFrom.getBreakableBlocks();
      int amount = blocks.size();
      dest.writeInt(amount);
      
      for (BreakableBlock block : blocks) {
        dest.writeInt(block.getX());
        dest.writeInt(block.getY());
        dest.writeInt(block.getZ());
        dest.writeUTF(block.getType().toString());
      }
      
      break;
    }
  }
}
