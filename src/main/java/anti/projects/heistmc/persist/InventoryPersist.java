package anti.projects.heistmc.persist;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import anti.projects.heistmc.api.Stack;

public class InventoryPersist {
  
  private static Collector<Stack<?>, ?, Integer> NOT_NULL_OR_EMPTY = Collectors.reducing(0, new Function<Stack<?>, Integer>() {

    @Override
    public Integer apply(Stack<?> t) {
      return t == null ? 0 : (t.size() > 0 ? 1 : 0);
    }
    
  }, new BinaryOperator<Integer>() {

    @Override
    public Integer apply(Integer t, Integer u) {
      return t + u;
    }
    
  });
  
  private static Collector<Object, ?, Integer> NOT_NULL = Collectors.reducing(0, new Function<Object, Integer>() {

    @Override
    public Integer apply(Object t) {
      return t == null ? 0 : 1;
    }
    
  }, new BinaryOperator<Integer>() {

    @Override
    public Integer apply(Integer t, Integer u) {
      return t + u;
    }
    
  });
  
  private Map<UUID, Stack<Map<Integer, ItemStack>>> entries = new HashMap<>();
  
  public InventoryPersist() {}
  private InventoryPersist(Map<UUID, Stack<Map<Integer, ItemStack>>> initial) {
    this.entries = initial;
  }
  
  public void pushInventory(Player p) {
    UUID uid = p.getUniqueId();
    Stack<Map<Integer, ItemStack>> inv = entries.get(uid);
    if (inv == null) {
      inv = new Stack<>();
      entries.put(uid, inv);
    }
    
    inv.push(mapify(p.getInventory()));
  }
  
  public void popInventory(Player p) {
    Stack<Map<Integer, ItemStack>> inv = entries.get(p.getUniqueId());
    if (inv == null || (inv != null && inv.size() == 0)) {
      throw new ArrayIndexOutOfBoundsException("There are no inventory entries for this player!");
    } else {
      Inventory pInv = p.getInventory();
      pInv.clear();
      Map<Integer, ItemStack> entry = inv.pop();
      for (Integer slot : entry.keySet()) {
        pInv.setItem(slot, entry.get(slot));
      }
    }
  }
  
  public int getPushedEntries(Player p) {
    Stack<Map<Integer, ItemStack>> entry = entries.get(p.getUniqueId());
    if (entry == null) {
      return 0;
    } else {
      return entry.size();
    }
  }
  
  public boolean hasEntry(Player p) {
    return getPushedEntries(p) > 0;
  }
  
  // TODO IMPORTANT - save ItemMeta!
  
  public void save(File f) throws IOException {
    DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
    
    // Format:
    //   writeInt (number of uuids)
    //   writeUTF (uuid)
    //   writeByte (number of inventory entries)
    //   for each inventory entry:
    //     writeByte (number of slot-item mappings)
    //     for each slot-item mapping:
    //       writeByte (slot)
    //       writeByte (item.getAmount())
    //       writeUTF (item.type.toString())
    
    dos.writeInt(entries.values().stream().collect(NOT_NULL_OR_EMPTY));
    
    for (UUID key : entries.keySet()) {
      Stack<Map<Integer, ItemStack>> entry = entries.get(key);
      if (entry == null || (entry != null && entry.size() == 0)) continue; // skip any empty entries
      
      dos.writeUTF(key.toString());
      dos.writeByte(entry.size());
      
      while (entry.size() > 0) {
        Map<Integer, ItemStack> inv = entry.pop();
        dos.writeByte(inv.values().stream().collect(NOT_NULL));
        for (Integer slot : inv.keySet()) {
          ItemStack items = inv.get(slot);
          if (items == null) continue; // skip any empty entries, just in case
          dos.writeByte(slot);
          dos.writeByte(items.getAmount());
          dos.writeUTF(items.getType().toString());
        }
      }
    }
    
    dos.close();
  }
  
  public static InventoryPersist load(File f) throws IOException, IllegalArgumentException {
    DataInputStream dis = new DataInputStream(new FileInputStream(f));
    
    // first, readInt for the number of UUIDs there are entries for
    int players = dis.readInt();
    
    Map<UUID, Stack<Map<Integer, ItemStack>>> all = new HashMap<>();
    for (int i = 0; i < players; i++) {
      UUID uid = UUID.fromString(dis.readUTF());
      int entries =  (int)dis.readByte();
      Stack<Map<Integer, ItemStack>> current = new Stack<>();
      all.put(uid, current);
      for (int entry = 0; entry < entries; entry++) {
        int mappings = (int)dis.readByte();
        Map<Integer, ItemStack> map = new HashMap<>();
        current.push(map);
        for (int mapping = 0; mapping < mappings; mapping++) {
          int slot = (int)dis.readByte();
          int amount = (int)dis.readByte();
          Material m; 
          try {
            m = Material.valueOf(dis.readUTF());
          } catch (IllegalArgumentException iae) {
            // we can skip this entry,
            // an invalid material will not
            // ruin the rest of the file.
            continue;
          }
          ItemStack is = new ItemStack(m, amount);
          map.put(slot, is);
        }
      }
    }
    
    dis.close();
    
    return new InventoryPersist(all);
  }
  
  private static Map<Integer, ItemStack> mapify(Inventory inv) {
    Map<Integer, ItemStack> map = new HashMap<>();
    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack at = inv.getItem(i);
      if (at != null) {
        map.put(i, at);
      }
    }
    return map;
  }
}
