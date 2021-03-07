package anti.projects.heistmc.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import anti.projects.heistmc.HeistMC;

public class SidebarDisplay {
  private Scoreboard display;
  private String title;
  private List<String> lines;
  private boolean dirty = false;
  public SidebarDisplay(Scoreboard display, String title) {
    this.display = display;
    this.title = title;
    this.lines = new ArrayList<String>();
  }
  
  private void rerender() {
    Objective obj;
    if (display.getObjective(DisplaySlot.SIDEBAR) == null) {
      obj = display.registerNewObjective("lobby", "lobby", title);
      obj.setDisplaySlot(DisplaySlot.SIDEBAR);
    } else {
      obj = display.getObjective(DisplaySlot.SIDEBAR);
      for (String line : display.getEntries()) {
        System.out.println("clearing entry for line " + line);
        display.resetScores(line);
      }
    }
    for (int i = 0; i < lines.size(); i++) {
      String text = lines.get(i);
      obj.getScore(text).setScore(i + 1);
    }
  }
  
  private void onUpdate(boolean isBatch) {
    if (!isBatch) {
      rerender();
    } else {
      dirty = true;
    }
  }
  
  public int addLine(String text, boolean isBatch) {
    lines.add(0, text);
    onUpdate(isBatch);
    return lines.size() - 1;
  }
  
  public void setLine(int idx, String text, boolean isBatch) {
    lines.set(lines.size() - 1 - idx, text);
    onUpdate(isBatch);
  }
  
  public void blit() {
    if (dirty) {
      rerender();
      dirty = false;
    } else {
      throw new IllegalStateException("Can only blit when display is dirty.");
    }
  }
  
  public void show(Player p) {
    p.setScoreboard(display);
  }
  
  public void unshow(Player p) {
    p.setScoreboard(HeistMC.BLANK);
  }
}
