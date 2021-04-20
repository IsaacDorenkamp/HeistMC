package anti.projects.heistmc.persist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.PlayerInventory;

/* An improvement over the InventoryPersist
 * implementation in HeistMC - I actually make
 * use of the ConfigurationSerializable instead
 * of manually reconstructing the ItemMeta... */
public class InventoryPersist {
  private HashMap<UUID, HashMap<String, HashMap<Integer, ItemStack>>> persisted = new HashMap<>();
  
  public InventoryPersist() {}
  private InventoryPersist(HashMap<UUID, HashMap<String, HashMap<Integer, ItemStack>>> map) {
    persisted.putAll(map);
  }
  
  public void save(File f) throws IOException {
    DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
    int entries = persisted.size();
    
    dos.writeInt(entries);
    for (UUID uid : persisted.keySet()) {
      dos.writeUTF(uid.toString());
      
      HashMap<String, HashMap<Integer, ItemStack>> worlds = persisted.get(uid);
      int playerWorlds = worlds.size();
      
      dos.writeInt(playerWorlds);
      for (String s : worlds.keySet()) {
        dos.writeUTF(s);
        HashMap<Integer, ItemStack> inv = worlds.get(s);
        int stacks = inv.size();
        dos.writeInt(stacks);
        for (Integer slot : inv.keySet()) {
          dos.writeInt(slot);
          
          ItemStack stack = inv.get(slot);
          YamlConfiguration config = new YamlConfiguration();
          Map<String, Object> values = stack.serialize();
          for (String key : values.keySet()) {
            Object value = values.get(key);
            if (value instanceof Map) {
              config.createSection(key, (Map<?, ?>)value);
            } else {
              config.set(key, value);
            }
          }
          
          dos.writeUTF(config.saveToString());
        }
      }
    }
    
    dos.close();
  }
  
  public void saveInventory(Player player, String world) {
    HashMap<String, HashMap<Integer, ItemStack>> forPlayer;
    if (persisted.containsKey(player.getUniqueId())) {
      forPlayer = persisted.get(player.getUniqueId());
    } else {
      forPlayer = new HashMap<>();
      persisted.put(player.getUniqueId(), forPlayer);
    }
    
    HashMap<Integer, ItemStack> inv = new HashMap<>(); // overwrite existing entry if one exists
    PlayerInventory playerInv = player.getInventory();
    for (int slot = 0; slot < playerInv.getSize(); slot++) {
      ItemStack stack = playerInv.getItem(slot);
      if (stack != null) {
        inv.put(slot, stack.clone());
      }
    }
    
    player.getInventory().clear();
    forPlayer.put(world, inv);
  }
  
  public boolean loadInventory(Player player, String world) {
    boolean hasPersistedEntries = persisted.containsKey(player.getUniqueId());
    if (hasPersistedEntries) {
      HashMap<String, HashMap<Integer, ItemStack>> entries = persisted.get(player.getUniqueId());
      boolean hasEntriesForWorld = entries.containsKey(world);
      if (hasEntriesForWorld) {
        player.getInventory().clear();
        HashMap<Integer, ItemStack> inv = entries.get(world);
        for (Integer slot : inv.keySet()) {
          ItemStack stack = inv.get(slot); // guaranteed not to be null
          player.getInventory().setItem(slot, stack);
        }
        entries.remove(world);
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  public static InventoryPersist load(File f) throws IOException {
    HashMap<UUID, HashMap<String, HashMap<Integer, ItemStack>>> invs = new HashMap<>();
    
    DataInputStream dis = new DataInputStream(new FileInputStream(f));
    int entries = dis.readInt();
    for (int i = 0; i < entries; i++) {
      UUID playerFor = UUID.fromString(dis.readUTF());
      int worlds = dis.readInt();
      
      HashMap<String, HashMap<Integer, ItemStack>> playerInvs = new HashMap<>();
      for (int j = 0; j < worlds; j++) {
        String worldName = dis.readUTF();
        int stacks = dis.readInt();
        HashMap<Integer, ItemStack> inv = new HashMap<>();
        for (int k = 0; k < stacks; k++) {
          int slot = dis.readInt();
          String cfgString = dis.readUTF();
          
          YamlConfiguration config = new YamlConfiguration();
          try {
            config.loadFromString(cfgString);
            Map<String, Object> serialized = config.getValues(true);
            ItemStack stack = ItemStack.deserialize(serialized);
            inv.put(slot, stack);
          } catch (InvalidConfigurationException e) {
            System.err.println("ERROR - Invalid configuration entry for user with UID " + playerFor.toString()
              + " in world " + worldName);
          }
        }
        playerInvs.put(worldName, inv);
      }
      invs.put(playerFor, playerInvs);
    }
    
    dis.close();
    
    return new InventoryPersist(invs);
  }
}