package anti.projects.heistmc.api;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;

import anti.projects.heistmc.Globals;

public enum Upgrade {
  BIGGER_BOOM(ChatColor.BOLD + "Bigger Boom", Material.BOW, Globals.WEAPON_ROCKET_LAUNCHER, 250),
  INCENDIARY(ChatColor.RED + "" + ChatColor.BOLD + "Incendiary", Material.BOW, Globals.WEAPON_ROCKET_LAUNCHER, 400),
  
  EARTHQUAKE(ChatColor.BOLD + "Earthquake", Material.CROSSBOW, Globals.WEAPON_GRAPPLING_HOOK, 300),
  FAST_GRAPPLE(ChatColor.YELLOW + "Fast Grapple", Material.CROSSBOW, Globals.WEAPON_GRAPPLING_HOOK, 200),
  INSTA_GRAPPLE(ChatColor.ITALIC + "InstaGrapple", Material.CROSSBOW, Globals.WEAPON_GRAPPLING_HOOK, 200),
  
  SLASHER(ChatColor.BLUE + "" + ChatColor.ITALIC + "Slasher", new Material[] {
      Material.WOODEN_SWORD,
      Material.STONE_SWORD,
      Material.IRON_SWORD,
      Material.GOLDEN_SWORD,
      Material.DIAMOND_SWORD
  }, null, 250, createEnchantSet(new Object[][] {
    { Enchantment.DAMAGE_ALL, 3 } // sharpness III
  })),
  MOLTEN_SWORD(ChatColor.GOLD + "" + ChatColor.BOLD + "Molten Sword", new Material[] {
      Material.WOODEN_SWORD,
      Material.STONE_SWORD,
      Material.IRON_SWORD,
      Material.GOLDEN_SWORD,
      Material.DIAMOND_SWORD
  }, null, 300, createEnchantSet(new Object[][] {
    { Enchantment.FIRE_ASPECT, 1 }
  })),
  FORCEFUL(ChatColor.YELLOW + "" + ChatColor.BOLD + "Forceful", new Material[] {
      Material.WOODEN_SWORD,
      Material.STONE_SWORD,
      Material.IRON_SWORD,
      Material.GOLDEN_SWORD,
      Material.DIAMOND_SWORD
  }, null, 200, createEnchantSet(new Object[][] {
    { Enchantment.KNOCKBACK, 2 }
  })),
  CURSE_OF_DECAY(ChatColor.GRAY + "" + ChatColor.BOLD + "Curse of Decay", new Material[] {
      Material.WOODEN_SWORD,
      Material.STONE_SWORD,
      Material.IRON_SWORD,
      Material.GOLDEN_SWORD,
      Material.DIAMOND_SWORD
  }, null, 400);
  
  private static HashMap<Enchantment, Integer> createEnchantSet(Object[][] enchants) {
    HashMap<Enchantment, Integer> all = new HashMap<>();
    for (Object[] pair : enchants) {
      all.put((Enchantment)pair[0], (Integer)pair[1]);
    }
    return all;
  }
  
  private HashMap<Enchantment, Integer> enchants = new HashMap<>();
  private Material weaponTypes[];
  private String weaponName;
  private String displayName;
  private int price;
  private Upgrade(String displayName, Material[] weaponTypes, String weaponName, int price, HashMap<Enchantment, Integer> enchants) {
    this.displayName = displayName;
    this.weaponTypes= weaponTypes;
    this.weaponName = weaponName;
    this.price = price;
    if (enchants != null) {
      this.enchants.putAll(enchants);
    }
  }
  private Upgrade(String displayName, Material[] weaponTypes, String weaponName, int price) {
    this(displayName, weaponTypes, weaponName, price, null);
  }
  private Upgrade(String displayName, Material[] weaponTypes, int price) {
    this(displayName, weaponTypes, null, price);
  }
  private Upgrade(String displayName, Material weaponType, String weaponName, int price) {
    this(displayName, new Material[] { weaponType }, weaponName, price);
  }
  private Upgrade(String displayName, Material weaponType, int price) {
    this(displayName, weaponType, null, price);
  }
  
  public int getPrice() {
    return price;
  }
  
  public void apply(ItemStack is) {
    ItemMeta im = is.getItemMeta();
    List<String> lore;
    if (im.hasLore()) {
      lore = im.getLore();
      lore.add(displayName);
    } else {
      lore = Arrays.asList(new String[] { displayName });
    }
    im.setLore(lore);
    
    if (enchants.size() > 0) {
      for (Enchantment ench : enchants.keySet()) {
        int level = enchants.get(ench);
        im.addEnchant(ench, level, true);
      }
    } else {
      if (!im.hasEnchants()) {
        im.addEnchant(Enchantment.BINDING_CURSE, 1, false);
      }
    }
    
    if (!im.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
      im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    
    is.setItemMeta(im);
  }
  
  public boolean itemHas(ItemStack is) {
    ItemMeta meta = is.getItemMeta();
    if (meta.hasLore()) {
      List<String> lore = meta.getLore();
      for (String line : lore) {
        if (line.equals(displayName)) {
          return true;
        }
      }
      return false;
    } else {
      return false;
    }
  }
  
  public boolean appliesTo(ItemStack is) {
    boolean material_matches = false;
    for (Material m : weaponTypes) {
      if (m.equals(is.getType())) {
        material_matches = true;
        break;
      }
    }
    
    if (!material_matches) return false;

    boolean nameCondition;
    String itemName = is.getItemMeta().getDisplayName();
    if (weaponName == null) {
      nameCondition = true;
    } else {
      nameCondition = itemName.equals(this.weaponName) && !itemHas(is);
    }
    
    return nameCondition && !itemHas(is);
  }
  
  @Override
  public String toString() {
    return displayName;
  }
  
  public static List<Upgrade> getUpgradesFor(ItemStack is) {
    List<Upgrade> upgrades = new ArrayList<>();
    for (Upgrade up : Upgrade.values()) {
      if (up.appliesTo(is)) {
        upgrades.add(up);
      }
    }
    return upgrades;
  }
  
  public static boolean canBeUpgraded(ItemStack is) {
    return getUpgradesFor(is).size() > 0;
  }
}
