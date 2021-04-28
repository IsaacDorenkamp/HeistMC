package anti.projects.heistmc.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import anti.projects.heistmc.HeistMC;
import anti.projects.heistmc.ui.SidebarDisplay;
import net.md_5.bungee.api.ChatColor;

public class HeistPlayer {
  
  private static HashMap<EntityType, Double> EARNINGS = new HashMap<>();
  static {
    EARNINGS.put(EntityType.ZOMBIE, 10.00);
    EARNINGS.put(EntityType.SKELETON, 15.00);
    EARNINGS.put(EntityType.CREEPER, 30.00);
    EARNINGS.put(EntityType.SPIDER, 5.00);
    EARNINGS.put(EntityType.VINDICATOR, 75.00);
  }
  
  private Player player;
  private UUID playerId; // keep UUID for persistence in the future
  private double money;
  
  private SidebarDisplay scoreboard;
  private Scoreboard scores;
  
  public HeistPlayer(Player player) {
    this.player = player;
    playerId = player.getUniqueId();
    money = 0.00;
    
    scores = Bukkit.getScoreboardManager().getNewScoreboard();
    scoreboard = new SidebarDisplay(scores, ChatColor.BOLD + " HEISTMC ");
    scoreboard.addLine("", true);
    scoreboard.addLine("Money", true);
    scoreboard.blit();
    updateScoreboard();
  }
  
  private void updateScoreboard() {
    scoreboard.setLine(1, String.format("Money: $%.2f", money), true);
    scoreboard.blit();
  }
  
  public Player getPlayer() {
    return player;
  }
  
  public UUID getPlayerID() {
    return playerId;
  }
  
  public double getMoney() {
    return money;
  }
  
  public void setMoney(double money) {
    this.money = money;
    updateScoreboard();
  }
  
  public void addMoneyFor(EntityType type) {
    Double earnings = EARNINGS.get(type);
    if (earnings == null) return;
    else setMoney(money + earnings);
  }
  
  public void setup() {
    scoreboard.show(player);
  }
  
  public void cleanup() {
    scoreboard.unshow(player);
  }
}
