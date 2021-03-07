package anti.projects.heistmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapManager {
  private File mapDir;
  private Map<String, File> maps;
  
  private File master;
  
  public MapManager(File mapDir) throws IOException {
    this.mapDir = mapDir;
    this.maps = new HashMap<String, File>();
    
    master = new File(mapDir, Globals.MAP_MASTER_FILE);
    if (master.exists()) {
      // load current map listing
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(master)));
      String line;
      while ((line = br.readLine()) != null) {
        String[] entries = line.split(":", 2);
        if (entries.length != 2) continue;
        maps.put(entries[1], new File(mapDir, entries[0]));
      }
      br.close();
    } else {
      master.createNewFile();
    }
  }
  
  public boolean hasMap(String name) {
    return maps.containsKey(name);
  }
  
  public File getMap(String name) {
    return maps.get(name);
  }
  
  public Set<String> getMapNames() {
    return maps.keySet();
  }
  
  public boolean exportMap(String name, File source, boolean override) throws IOException {
    if (maps.containsKey(name)) {
      if (override) {
        File cur = maps.get(name);
        delete(cur);
      } else {
        return false;
      }
    }
    
    if (!source.isDirectory()) {
      System.out.println(source.getAbsolutePath());
      return false;
    }
    
    String target = UUID.randomUUID().toString();
    File outDir = new File(mapDir, target);
    outDir.mkdir();
    copyDir(source, outDir.getAbsolutePath());
    maps.put(name, new File(mapDir, target));
    
    save(); // sync master file with in-program map
    // TODO - above line breaking this function??
    // files seem to copy ok
    
    return true;
  }
  
  private static void copyDir(File srcDir, String outPath) throws IOException {
    for (File entry : srcDir.listFiles()) {
      if (entry.isDirectory()) {
        String newOut = Paths.get(outPath, entry.getName()).toString();
        new File(newOut).mkdir();
        copyDir(entry, newOut);
      } else {
        // skip session.lock as it will always be locked by Bukkit
        // when the player is in the world
        if (entry.getName().equals("session.lock")) {
          continue;
        }
        Files.copy(entry.toPath(), Paths.get(outPath, entry.getName()));
      }
    }
  }
  
  private static void delete(File f) {
    if (f.isDirectory()) {
      for (File subF : f.listFiles()) {
        delete(subF);
      }
      f.delete();
    } else {
      f.delete();
    }
  }
  
  public void save() throws IOException {
    String content = maps.keySet().stream().map(new Function<String, String>() {
      public String apply(String key) {
        String dirName = maps.get(key).getName();
        return dirName + ":" + key;
      }
    }).collect(Collectors.joining("\n"));
    System.out.println("content: " + content);
    FileOutputStream fos = new FileOutputStream(new File(mapDir, Globals.MAP_MASTER_FILE));
    fos.write(content.getBytes());
    fos.close();
  }
  
  public static void main(String[] args) throws IOException {
    MapManager mm = new MapManager(new File("testing"));
    mm.exportMap("map-test", new File("src"), true);
    System.out.println(mm.hasMap("map-test"));
  }
}
