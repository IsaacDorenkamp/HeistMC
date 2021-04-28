package anti.projects.heistmc.ui;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.api.HeistPlayer;
import anti.projects.heistmc.api.Upgrade;
import anti.projects.heistmc.stages.HeistWorld;
import net.md_5.bungee.api.ChatColor;

public class UpgradeView extends InteractiveView {
  
  private static final int ITEM_SLOT = 10;
  
  private static final int UPGRADES_START = 12;
  private static final int UPGRADE_SLOTS = 5;
  
  private Player viewer;
  private Inventory upgradeview;
  private ItemStack viewing = null;
  private int weaponSlot = -1;
  public UpgradeView(Player viewer) {
    this.upgradeview = Bukkit.createInventory(viewer, 27, Globals.STRING_UPGRADES);
    this.viewer = viewer;
    setSubject(null);
  }
  
  public void setSubject(ItemStack is) {
    if (is == null) {
      viewing = null;
      weaponSlot = -1;
    } else {
      List<Upgrade> upgrades = Upgrade.getUpgradesFor(is);
      if (upgrades.size() == 0) {
        MessageUtil.send(viewer, "That item can't be upgraded!");
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.75f, 1f);
      } else {
        viewing = is;
      }
    }
    List<Upgrade> upgrades = viewing == null ? Arrays.asList(new Upgrade[] {}) : Upgrade.getUpgradesFor(viewing);
    for (int i = 0; i < upgradeview.getSize(); i++) {
      int upgradeRelPos = i - UPGRADES_START;
      if (i == ITEM_SLOT) {
        upgradeview.setItem(i, viewing);
      } else if (upgradeRelPos >= 0 && upgradeRelPos < UPGRADE_SLOTS) {
        Upgrade upgrade = null;
        if (upgradeRelPos < upgrades.size()) {
          upgrade = upgrades.get(upgradeRelPos);
        }
        if (upgrade == null) {
          upgradeview.setItem(i, Globals.getNamedItem(Material.BLACK_STAINED_GLASS_PANE, ""));
        } else {
          ItemStack upg = Globals.getNamedItem(Material.ENCHANTED_BOOK, String.format("%s ($%d)", upgrade.toString()
              + ChatColor.RESET, upgrade.getPrice()));
          upgradeview.setItem(i, upg);
        }
      } else {
        upgradeview.setItem(i, Globals.getNamedItem(Material.BLACK_STAINED_GLASS_PANE, ""));
      }
    }
  }

  @Override
  public Inventory getTopInventory() {
    return upgradeview;
  }

  @Override
  public Inventory getBottomInventory() {
    return viewer.getInventory();
  }

  @Override
  public HumanEntity getPlayer() {
    return viewer;
  }

  @Override
  public InventoryType getType() {
    return InventoryType.CHEST;
  }

  @Override
  public String getTitle() {
    return Globals.STRING_UPGRADES;
  }

  @Override
  public void inventoryClicked(InventoryClickEvent evt) {
    int rawSlot = evt.getRawSlot();
    int slot = evt.getSlot();
    boolean isTop = rawSlot < upgradeview.getSize();
    if (isTop) {
      if (viewing != null) {
        List<Upgrade> upgrades = Upgrade.getUpgradesFor(viewing);
        int upgradeRelSlot = slot - UPGRADES_START;
        if (upgradeRelSlot >= 0 && upgradeRelSlot < UPGRADE_SLOTS && upgradeRelSlot < upgrades.size()) {
          Upgrade upgrade = upgrades.get(upgradeRelSlot);
          
          HeistWorld inWorld = HeistWorld.getInstanceForPlayer(viewer);
          if (inWorld != null) {
            HeistPlayer hp = inWorld.getHeistPlayer(viewer);
            double money = hp.getMoney();
            if (money >= upgrade.getPrice()) {
              hp.setMoney(money - upgrade.getPrice());
              
              ItemStack is = viewer.getInventory().getItem(weaponSlot);
              upgrade.apply(is);
              
              Sound toPlay;
              if (Upgrade.canBeUpgraded(is)) {
                setSubject(is);
                toPlay = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
              } else {
                setSubject(null);
                toPlay = Sound.ENTITY_PLAYER_LEVELUP;
              }
              
              viewer.playSound(viewer.getLocation(), toPlay, Globals.UI_SOUND_VOLUME, 1f);
            } else {
              MessageUtil.send(viewer, ChatColor.RED + "" + ChatColor.BOLD + "You can't afford that!");
              viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_LOCKED, Globals.UI_SOUND_VOLUME, 1f);
            }
          }
        }
      }
    } else {
      ItemStack is = viewer.getInventory().getItem(slot);
      weaponSlot = slot;
      setSubject(is);
    }
    evt.setResult(Result.DENY);
    evt.setCancelled(true);
  }

}
