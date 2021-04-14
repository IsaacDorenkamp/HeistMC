package anti.projects.heistmc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import anti.projects.heistmc.stages.HeistWorld;
import anti.projects.heistmc.stages.Lobby;

public class WorldManager {
  
  private static boolean delete(File dir) {
    if (dir.exists()) {
      if (dir.isDirectory()) {
        for (File f : dir.listFiles()) {
          if (f.isDirectory()) {
            boolean result = delete(f);
            boolean dirResult = f.delete();
            if (!(result && dirResult)) return false;
          } else {
            if (!f.delete()) return false;
          }
        }
        return true;
      } else {
        return dir.delete();
      }
    } else {
      return false;
    }
  }
  
  public boolean willPersist(World w) {
    return !worlds.contains(w);
  }
  
  private static ArrayList<World> worlds = new ArrayList<World>();
  private static ArrayList<World> allWorlds = new ArrayList<World>();
  
  private Server server;
  private World mainWorld = null;
  
  public WorldManager(Server server) {
    this.server = server;
  }
  
  private void depopulate(World w) {
    Lobby forWorld = Lobby.getLobbyForWorld(w);
    if (forWorld != null) {
      forWorld.evacuate("Server is shutting down or reloading!");
      forWorld.deinitialize();
    }
    
    HeistWorld heistForWorld = HeistWorld.getInstanceForWorld(w);
    if (heistForWorld != null) {
      heistForWorld.evacuate("Server is shutting down or reloading!");
    }
    
    // last resort
    for (Player p : w.getPlayers()) {
      // kick all players to main world spawn
      p.teleport(getMainWorld().getSpawnLocation());
    }
  }
  
  public boolean delete(World w) {
    System.out.println("Unload success: " + Bukkit.unloadWorld(w.getName(), false));
    File f = w.getWorldFolder();
    boolean deleted = delete(f);
    boolean fdeleted = f.delete();
    return deleted && fdeleted;
  }
  
  public boolean deleteAndRemove(World w) {
    if (worlds.contains(w)) worlds.remove(w);
    if (allWorlds.contains(w)) allWorlds.remove(w);
    
    return delete(w);
  }
  
  public void purge() {
    Iterator<World> it = worlds.iterator();
    while (it.hasNext()) {
      World w = it.next();
      it.remove();
      depopulate(w);
      deleteAndRemove(w);
    }
  }
  
  public void unloadOnDisable() {
    for (World w : allWorlds) {
      Bukkit.unloadWorld(w, true);
    }
  }
  
  public World copyFrom(File source, boolean persist) throws IOException {
    String name = nextName();
    copyDir(source, Paths.get(server.getWorldContainer().getAbsolutePath(), name).toString());
    return createWorld(getBlankWorldCreator(name), persist);
  }
  
  private static String nextName() {
    return String.format("world_%s", UUID.randomUUID().toString());
  }
  
  public World blank(boolean persist) {
    return getOrBlank(nextName(), persist);
  }
  
  public World getMainWorld() {
    if (mainWorld == null) {
      mainWorld = server.getWorlds().get(0);
    }
    return mainWorld;
  }
  
  public World createWorld(WorldCreator creator, boolean persist) {
    World world = server.createWorld(creator);
    if (!persist) {
      worlds.add(world);
    }
    allWorlds.add(world);
    return world;
  }
  
  public World getOrBlank(String name, boolean persist) {
    return createWorld(getBlankWorldCreator(name), persist);
  }
  
  public static final WorldCreator getBlankWorldCreator(String name) {
    WorldCreator wc = WorldCreator.name(name);
    wc.generator(new ChunkGenerator() {
      @Override
      public ChunkData generateChunkData(World world, Random rand, int x, int z, BiomeGrid grid) {
        return createChunkData(world);
      }
    });
    return wc;
  }
  
  public static void copyDir(File srcDir, String outPath) throws IOException {
    File outDir = new File(outPath);
    if (!outDir.exists() || !outDir.isDirectory()) {
      outDir.mkdir();
    }
    for (File entry : srcDir.listFiles()) {
      if (entry.isDirectory()) {
        String newOut = Paths.get(outPath, entry.getName()).toString();
        copyDir(entry, newOut);
      } else {
        // skip session.lock as it will always be locked by Bukkit
        // when the player is in the world
        if (entry.getName().equals("session.lock") || entry.getName().equals("uid.dat")) {
          continue;
        }
        Files.copy(entry.toPath(), Paths.get(outPath, entry.getName()));
      }
    }
  }
}
