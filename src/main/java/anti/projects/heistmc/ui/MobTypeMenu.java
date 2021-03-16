package anti.projects.heistmc.ui;

import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class MobTypeMenu extends SingleViewMenu {
  
  private static final EntityType[] ENTITIES = new EntityType[] {
    EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER, EntityType.VINDICATOR  
  };
  private static final Material[] ENTITY_HEADS = new Material[] {
      Material.ZOMBIE_HEAD, Material.SKELETON_SKULL, Material.CREEPER_HEAD, Material.SPIDER_SPAWN_EGG, Material.IRON_AXE
  };
  
  public static String format(EntityType type) {
    String name = type.toString();
    return String.format("%c%s", name.charAt(0), name.substring(1).toLowerCase());
  }
  
  private Consumer<EntityType> cbk;
  public MobTypeMenu(Player pFor, Consumer<EntityType> cbk) {
    super("Mob Type", 9, pFor);
    this.cbk = cbk;
    construct();
  }
  
  private void construct () {
    MenuPage ui = getMenu();
    for (int i = 0; i < 9; i++) ui.addItem(i, Material.BLACK_STAINED_GLASS_PANE, " ", null);
    for (int i = 0; i < ENTITIES.length; i++) {
      final EntityType thisType = ENTITIES[i];
      ui.addItem(i + 2, ENTITY_HEADS[i], format(thisType), new MenuItemListener() {
        @Override
        public void onSelected() {
          cbk.accept(thisType);
        }
      });
    }
  }
}
