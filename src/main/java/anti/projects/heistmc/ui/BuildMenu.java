package anti.projects.heistmc.ui;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import anti.projects.heistmc.Globals;
import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.MessageUtil;
import anti.projects.heistmc.stages.BuildWorld;
import net.md_5.bungee.api.ChatColor;
import net.wesjd.anvilgui.AnvilGUI;
import net.wesjd.anvilgui.AnvilGUI.Response;
import net.wesjd.anvilgui.AnvilGUI.Slot;

public class BuildMenu extends MultiViewMenu {
  
  private Player viewer;
  private BuildWorld world;
  public BuildMenu(Player viewer, BuildWorld world) {
    super(Globals.STRING_BUILD_MENU, 54, viewer);
    this.viewer = viewer;
    this.world = world;
    constructBaseMenu();
  }
  
  public BuildWorld getWorld() {
    return world;
  }
  
  private void constructBaseMenu() {
    MenuPage base = getBaseMenu();
    base.addItem(12, Material.COMPASS, Globals.STRING_SET_SPAWN, new MenuItemListener() {
      public void onSelected() {
        Location l = viewer.getLocation();
        world.getWorld().setSpawnLocation(l);
        MessageUtil.send(viewer, "Spawn location set to " + ChatColor.ITALIC + String.format("(%d, %d, %d)",
            l.getBlockX(), l.getBlockY(), l.getBlockZ()));
        viewer.closeInventory();
      }
    });
    base.addItem(13, Material.BOOK, "Mission Objectives", new MenuItemListener() {
      public void onSelected() {
        pushView(new MissionObjectiveMenu(world.getHeistWorldData().getObjectives(), BuildMenu.this, world.getMissionObjectiveTracker()));
      }
    });
    base.addItem(14, Material.ZOMBIE_HEAD, Globals.STRING_HIDE_PLACEHOLDERS, new MenuItemListener() {
      public void onSelected() {
        world.selectPlaceholderMobs(null);
        viewer.closeInventory();
      }
    });
    
    base.addItem(21, Material.COBBLESTONE, "Breakable Blocks", new MenuItemListener() {
      public void onSelected() {
        pushView(new BreakableBlockMenu(viewer, world));
      }
    });
    
    base.addItem(22, Material.BOW, "Weapons", new MenuItemListener() {
      public void onSelected() {
        pushView(new WeaponsMenu(viewer));
      }
    });
    base.addItem(23, Material.GOLD_INGOT, "Assign Prices", new MenuItemListener() {
      public void onSelected() {
        final AnvilGUI.Builder builder = new AnvilGUI.Builder();
        builder.plugin(HeistMC.getInstance());
        builder
          .title("Assign Price")
          .onLeftInputClick(new Consumer<Player>() {
            public void accept(Player player) {
              ItemStack is = player.getItemOnCursor();
              if (is != null && !is.getType().isAir()) {
                ItemStack copy = is.clone();
                ItemMeta meta = copy.getItemMeta();
                String name;
                if (meta.hasDisplayName()) {
                  name = meta.getDisplayName();
                } else {
                  name = Globals.getMaterialName(copy.getType());
                }
                meta.setLore(Arrays.asList(new String[] { name }));
                meta.setDisplayName("0");
                copy.setItemMeta(meta);
                player.getOpenInventory().setItem(Slot.INPUT_LEFT, copy);
                player.setItemOnCursor(new ItemStack(Material.AIR, 0));
              }
            }
          })
          .onComplete(new BiFunction<Player, String, AnvilGUI.Response>() {

            @Override
            public Response apply(Player t, String u) {
              boolean isPrice = u.matches("^\\$?[0-9]+(\\.[0-9]{2})?$");
              if (isPrice) {
                 if (u.charAt(0) == '$') {
                   u = u.substring(1);
                 }
                 Double price = Double.parseDouble(u);
                 ItemStack holding = t.getOpenInventory().getItem(Slot.INPUT_LEFT);
                 if (holding != null && !holding.getType().equals(Material.AIR)) {
                   ItemStack newstack = holding.clone();
                   ItemMeta meta = newstack.getItemMeta();
                   String dispName = meta.getLore().get(0);
                   meta.setDisplayName(String.format("%s ($%.2f)", dispName, price));
                   meta.setLore(Arrays.asList(new String[] { String.format(ChatColor.GREEN + "" + ChatColor.BOLD +
                       "Price: $%.2f", price), dispName }));
                   newstack.setItemMeta(meta);
                   t.getInventory().addItem(newstack);
                   t.getOpenInventory().setItem(Slot.INPUT_LEFT, new ItemStack(Material.AIR, 0));
                 }
                 return Response.close();
              } else {
                return Response.text("Invalid price.");
              }
            }
          
        });
        builder.open(viewer);
      }
    });
    base.addItem(30, Material.ANVIL, Globals.STRING_PLACE_UPGRADE_ANVIL, new MenuItemListener() {
      public void onSelected() {
        ItemStack upgradeAnvil = Globals.getNamedItem(Material.ANVIL, Globals.STRING_UPGRADE_ANVIL);
        PlayerInventory inv = viewer.getInventory();
        int held = inv.getHeldItemSlot();
        
        int slot = inv.first(upgradeAnvil);
        if (slot < 7 && slot >= 0) {
          held = slot;
          inv.setHeldItemSlot(held);
        } else {
          if (slot != -1) {
            inv.setItem(slot, null);
          }
          for (int i = 6; i >= 0; i--) {
            if (inv.getItem(i) == null) {
              held = i;
              break;
            }
          }
          if (held >= 7) {
            held = 6;
          }
          inv.setHeldItemSlot(held);
          inv.setItemInMainHand(upgradeAnvil);
        }
        viewer.closeInventory();
      }
    });
  }

}
