package anti.projects.heistmc.ui;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import anti.projects.heistmc.Globals;

public class BuildWorldSelector implements Listener {
  private static Material[] LABELS = new Material[] {
      Material.WHITE_STAINED_GLASS_PANE,
      Material.BLUE_STAINED_GLASS_PANE,
      Material.GREEN_STAINED_GLASS_PANE,
      Material.ORANGE_STAINED_GLASS_PANE,
      Material.RED_STAINED_GLASS_PANE
  };
  HashMap<UUID, Consumer<Integer>> showing = new HashMap<>();
  public BuildWorldSelector() {}
  
  public void show(Player p, Consumer<Integer> cbk) {
    Inventory inv = Bukkit.createInventory(p, InventoryType.HOPPER, "Select Build World");
    for (int i = 0; i < Globals.BUILD_SLOTS; i++) {
      inv.setItem(i, Globals.getNamedItem(LABELS[i], String.format("Build World %d", i + 1)));
    }
    
    p.openInventory(inv);
    showing.put(p.getUniqueId(), cbk);
  }
  
  @EventHandler
  public void inventoryClosed(InventoryCloseEvent evt) {
    HumanEntity p = evt.getPlayer();
    if (showing.containsKey(p.getUniqueId())) {
      showing.remove(p.getUniqueId());
    }
  }
  
  @EventHandler
  public void inventoryClickEvent(InventoryClickEvent evt) {
    HumanEntity ent = evt.getWhoClicked();
    if (showing.containsKey(ent.getUniqueId())) {
      Consumer<Integer> cbk = showing.get(ent.getUniqueId());
      showing.remove(ent.getUniqueId());
      ent.closeInventory();
      cbk.accept(evt.getSlot());
    }
  }
}
