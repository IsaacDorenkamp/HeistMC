package anti.projects.heistmc.persist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import anti.projects.heistmc.api.Stack;

public class InventoryPersist {
  
  private static Class<?> CraftItemMeta = null;
  private static Constructor<?> CraftItemMetaConstructor = null;
  
  // TODO - figure out why this isn't working...???
  static {
    try {
      CraftItemMeta = Class.forName("org.bukkit.craftbukkit.v1_16_R3.inventory.CraftMetaItem");
      CraftItemMetaConstructor = CraftItemMeta.getDeclaredConstructor(Map.class);
      CraftItemMetaConstructor.setAccessible(true);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
      e.printStackTrace();
    }
  }
  
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
  
  private static enum MapTypes {
    STRING, INTEGER, MAP, ENUM, SKIP, META
  }
  
  @SuppressWarnings("unchecked")
  private static void writeMap(ObjectOutputStream dos, Map<String, Object> map) throws IOException {
    dos.writeInt(map.size());
    for (String s : map.keySet()) {
      // write key
      dos.writeUTF(s);
      
      // write data type
      Object obj = map.get(s);
      if (obj instanceof String) {
        dos.writeUTF(MapTypes.STRING.toString());
        dos.writeUTF((String)obj);
      } else if (obj instanceof Integer) {
        dos.writeUTF(MapTypes.INTEGER.toString());
        dos.writeInt((Integer)obj);
      } else if (obj instanceof Map<?, ?>) {
        dos.writeUTF(MapTypes.MAP.toString());
        writeMap(dos, (Map<String, Object>)obj);
      } else if (obj.getClass().isEnum()) {
        dos.writeUTF(MapTypes.ENUM.toString());
        String enumName = obj.getClass().getName();
        dos.writeUTF(enumName);
        dos.writeUTF(obj.toString());
      } else if (obj instanceof ItemMeta) {
        dos.writeUTF(MapTypes.META.toString());
        ItemMeta im = (ItemMeta)obj;
        Map<String, Object> sub = im.serialize();
        writeMap(dos, sub);
      } else {
        System.out.println("Skipping object of type " + obj.getClass().getName());
        dos.writeUTF(MapTypes.SKIP.toString());
        continue;
      }
    }
  }
  
  private static Map<String, Object> readMap(ObjectInputStream dis) throws IOException {
    Map<String, Object> out = new HashMap<String, Object>();
    int entries = dis.readInt();
    for (int i = 0; i < entries; i++) {
      String key = dis.readUTF();
      String _type = dis.readUTF();
      MapTypes type = MapTypes.valueOf(_type);
      switch(type) {
      case STRING:
        String sval = dis.readUTF();
        out.put(key, sval);
        break;
      case INTEGER:
        Integer ival = dis.readInt();
        out.put(key, ival);
        break;
      case MAP:
        Map<String, Object> obj = readMap(dis);
        out.put(key, obj);
        break;
      case ENUM:
        String enumName = dis.readUTF();
        String valueName = dis.readUTF();
        Class<?> classFor;
        try {
          classFor = Class.forName(enumName);
        } catch (ClassNotFoundException e) {
          // brutal, but necessary
          continue;
        }
        @SuppressWarnings("unchecked") Object val = Enum.valueOf((Class<? extends Enum>)classFor, valueName);
        out.put(key, val);
        break;
      case META:
        Map<String, Object> metadata = readMap(dis);
        ItemMeta im = null;
        try {
          im = (ItemMeta)CraftItemMetaConstructor.newInstance(metadata);
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("Failed to construct metadata.");
          continue;
        }
        out.put(key, im);
        break;
      case SKIP:
        break;
      }
    }
    return out;
  }
  
  public void save(File f) throws IOException {
    ObjectOutputStream dos = new ObjectOutputStream(new FileOutputStream(f));
    
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
          Map<String, Object> stack = items.serialize();
          writeMap(dos, stack);
        }
      }
    }
    
    dos.close();
  }
  
  public void loadFrom(File f) throws IOException, IllegalArgumentException {
    InventoryPersist ip = load(f);
    this.entries = ip.entries;
  }
  
  public static InventoryPersist load(File f) throws IOException, IllegalArgumentException {
    ObjectInputStream dis = new ObjectInputStream(new FileInputStream(f));
    
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
          Map<String, Object> data = readMap(dis);
          ItemStack stack = ItemStack.deserialize(data);
          Object meta = data.get("meta");
          if (meta instanceof ItemMeta) {
            stack.setItemMeta((ItemMeta)meta);
          }
          map.put(slot, stack);
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
