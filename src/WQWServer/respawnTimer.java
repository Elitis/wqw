package WQWServer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Respawns a monster
 * @author XVII
 */


public class respawnTimer {
    
    public static void main(final int monsterid, final int monstertype, final int roomtype, final int roomnumb) {

    final Timer timer = new Timer();

    int pingTime = 10000;
     timer.schedule(new TimerTask()
       {
        public void run() {
            try {
                Main.server.gameServer.room[roomtype][roomnumb].respawnMonsterDo(monsterid);
            } catch (Exception e) {
                Main.server.debug("Exception in respawn timer: "+e.getMessage());
                Main.server.gameServer.room[roomtype][roomnumb].respawnMonsterDo(monsterid);
            }
        }
      },pingTime);

    }
}