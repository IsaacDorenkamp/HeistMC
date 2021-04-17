package anti.projects.heistmc.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

public class InternalPermissions {
  private HashMap<UUID, List<String>> perms;
  public InternalPermissions() {
    perms = new HashMap<>();
  }
  private InternalPermissions(HashMap<UUID, List<String>> perms) {
    this.perms = perms;
  }
  
  public boolean hasPermission(OfflinePlayer p, String perm) {
    List<String> permList = perms.get(p.getUniqueId());
    if (permList == null) {
      return p.isOp();
    } else {
      return permList.contains(perm) || p.isOp();
    }
  }
  
  public void grantPermission(OfflinePlayer p, String perm) {
    List<String> permsFor = perms.get(p.getUniqueId());
    if (permsFor == null) {
      permsFor = new ArrayList<String>();
      permsFor.add(perm);
      perms.put(p.getUniqueId(), permsFor);
    } else {
      if (permsFor.contains(perm)) return;
      permsFor.add(perm);
    }
  }
  
  public boolean revokePermission(OfflinePlayer p, String perm) {
    if (p.isOp()) return false;
    List<String> permsFor = perms.get(p.getUniqueId());
    if (permsFor == null) {
      return false;
    } else {
      return permsFor.remove(perm);
    }
  }
  
  public void save(File f) throws IOException {
    int keys = perms.size();
    DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
    dos.writeInt(keys);
    for (UUID key : perms.keySet()) {
      List<String> permList = perms.get(key);
      dos.writeUTF(key.toString());
      dos.writeInt(permList.size());
      for (String perm : permList) {
        dos.writeUTF(perm);
      }
    }
    dos.close();
  }
  
  public static InternalPermissions load(File f) throws IOException {
    HashMap<UUID, List<String>> perms = new HashMap<>();
    DataInputStream dis = new DataInputStream(new FileInputStream(f));
    
    int entries = dis.readInt();
    for (int i = 0; i < entries; i++) {
      UUID puid = UUID.fromString(dis.readUTF());
      int totalPerms = dis.readInt();
      String[] permList = new String[totalPerms];
      for (int j = 0; j < totalPerms; j++) {
        permList[j] = dis.readUTF();
      }
      perms.put(puid, new ArrayList<String>(Arrays.asList(permList)));
    }
    
    dis.close();
    
    return new InternalPermissions(perms);
  }
}
