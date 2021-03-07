package anti.projects.heistmc;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import anti.projects.heistmc.api.ChatRoom;

public class MessageUtil {
  private static Server server = null;
  private static Logger logger = null;
  private static String prefix = ChatColor.GREEN + String.format("[%s] ", Globals.PLUGIN_NAME) + ChatColor.RESET;
  public static void initialize(Server s) {
    server = s;
    logger = server.getLogger();
  }
  
  public static void reset() {
    server = null;
  }
  
  public static String createMessage(String content) {
    return prefix + content;
  }
  
  public static void broadcast(String message) {
    String send = createMessage(message);
    logger.info(send);
    server.broadcastMessage(send);
  }
  
  public static void send(CommandSender recipient, String message) {
    String msg = createMessage(message);
    logger.info(msg);
    recipient.sendMessage(msg);
  }
  
  public static void sendToRoom(ChatRoom cr, String message) {
    String msg = createMessage(message);
    logger.info(msg);
    for (Player p : cr.getPlayers()) {
      p.sendMessage(msg);
    }
  }
  
  public static void title(Player p, String title, String subtitle) {
    p.sendTitle(title, subtitle, -1, -1, -1);
  }
  
  public static void roomTitle(ChatRoom cr, String title, String subtitle) {
    for (Player p : cr.getPlayers()) {
      p.sendTitle(title, subtitle, -1, -1, -1);
    }
  }
  
  public static void roomTitle(ChatRoom cr, String title) {
    roomTitle(cr, title, "");
  }
}
