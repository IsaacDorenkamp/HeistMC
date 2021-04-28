package anti.projects.heistmc.mission;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.stages.HeistWorld;
import anti.projects.heistmc.ui.MenuItemListener;
import anti.projects.heistmc.ui.OptionsMenu;
import anti.projects.heistmc.ui.OptionsMenuOwner;
import net.md_5.bungee.api.ChatColor;

import net.wesjd.anvilgui.AnvilGUI;
import net.wesjd.anvilgui.AnvilGUI.Response;

public class ItemObjective extends MissionObjective implements OptionsMenuOwner {

  private Material type = null;
  private String displayName = null;
  public ItemObjective(String name) {
    super(name, "");
  }
  
  public ItemObjective() {
    this("");
  }
  
  public void setMaterial(Material type) {
    this.type = type;
  }
  
  public void setDisplayName(String disp) {
    this.displayName = disp;
  }

  @Override
  public boolean isComplete(HeistWorld forWorld) {
    List<Player> all = forWorld.getPlayers();
    ItemStack checkFor = Globals.getNamedItem(type, displayName);
    for (Player player : all) {
      for (ItemStack is : player.getInventory().getContents()) {
        if (is == null) continue;
        if (Globals.isNamedItem(is, type, displayName)) {
          return true;
        }
      }
    }
    return false;
  }
  
  private ItemStack nametag = null;
  private ItemStack finish = null;

  @Override
  public boolean tryConfigAction(PlayerInteractEvent evt) {
    ItemStack is = evt.getItem();
    if (is == null) {
      return false;
    } else {
      if (Globals.isNamedItem(is, Material.NAME_TAG, Globals.STRING_NAME_ITEM)) {
        AnvilGUI.Builder builder = new AnvilGUI.Builder();
        builder.plugin(HeistMC.getInstance());
        builder.itemLeft(Globals.getNamedItem(Material.NAME_TAG, "rename...")).onComplete(new BiFunction<Player, String, AnvilGUI.Response>() {
          @Override
          public Response apply(Player t, String u) {
            ItemObjective.this.displayName = u;
            MessageUtil.send(t, "Setting item name to " + ChatColor.BOLD + u);
            return AnvilGUI.Response.close();
          }
        });
        builder.open(evt.getPlayer());
        return false;
      } else if(Globals.isNamedItem(is, Material.WRITTEN_BOOK, Globals.STRING_FINISH)) {
        if (type == null) {
          MessageUtil.send(evt.getPlayer(), ChatColor.RED + "" + ChatColor.BOLD + "Please select an item type by right-clicking with an item in your hand.");
          return false;
        }
        if (displayName == null) {
          MessageUtil.send(evt.getPlayer(), ChatColor.RED + "" + ChatColor.BOLD + "Please set a name for the item by right-clicking with the 'Name Item' nametag in your inventory.");
          return false;
        }
        Inventory inv = evt.getPlayer().getInventory();
        if (nametag != null) inv.remove(nametag);
        if (finish != null) inv.remove(finish);
        
        evt.getPlayer().playSound(evt.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, Globals.UI_SOUND_VOLUME, 1f);
        MessageUtil.send(evt.getPlayer(), "Configured item objective for item type " + ChatColor.YELLOW +
            Globals.getMaterialName(type) + ChatColor.RESET + " with name " + ChatColor.BOLD + displayName);
        cfgmeta();
        
        return true;
      } else {
        type = is.getType();
        MessageUtil.send(evt.getPlayer(), "Setting item type to " + Globals.getMaterialName(type));
        return false;
      }
    }
  }
  
  private void cfgmeta() {
    this.name = "OBTAIN";
    this.description = "Find and pick up a " + Globals.getMaterialName(type) + " called " + ChatColor.BOLD + displayName;
  }

  @Override
  public boolean onStartConfig(Player p) {
    nametag = Globals.getNamedItem(Material.NAME_TAG, Globals.STRING_NAME_ITEM);
    finish = Globals.getNamedItem(Material.WRITTEN_BOOK, Globals.STRING_FINISH);
    Inventory inv = p.getInventory();
    inv.setItem(0, nametag);
    inv.setItem(1, finish);
    MessageUtil.send(p, "TIP: Use the nametag to rename the item and right click with an item in hand to set the item type.");
    return true;
  }

  @Override
  public Material getDisplayIcon() {
    return Material.DEBUG_STICK;
  }
  
  @Override
  public void save(DataOutputStream dos) throws IOException {
    // write item material name (Material.toString value), item display name
    dos.writeUTF(type.toString());
    dos.writeUTF(displayName);
  }

  @Override
  public void load(DataInputStream dis) throws IOException {
    String _materialStr = dis.readUTF();
    String _displayName = dis.readUTF();
    Material _m = Material.valueOf(_materialStr);
    
    type = _m;
    displayName = _displayName;
    
    cfgmeta();
  }
  
  @Override
  public String toString() {
    return String.format("Obtain %s", ChatColor.YELLOW + displayName);
  }

  public OptionsMenu getOptionsMenu(final Player playerFor) {
    OptionsMenu menu = new OptionsMenu();
    menu.addEntry(Material.IRON_PICKAXE, "Create Item for this Objective", new MenuItemListener() {
      public void onSelected() {
        ItemStack created = Globals.getNamedItem(type, displayName);
        playerFor.setItemOnCursor(created);
      }
    });
    return menu;
  }
}
