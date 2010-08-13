package WQWServer;

import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Pings the client incase they refresh or something
 * @author XVII
 */


public class pingTimer {
    
    public static void main(final ServerConnection client) {

    final Timer timer = new Timer();

    int pingTime = 30000;
     timer.schedule(new TimerTask()
       {
        public void run() {
            try {
                /*if (false) {
                    client.finalize();
                    timer.cancel();
                }*/
                client.monkilled = 0;
                client.packetsend = 0;
            } catch (Exception e) {
                Main.server.debug("Exception in ping timer: "+e.getMessage());
                client.monkilled = 0;
                client.packetsend = 0;
            }
        }
      },pingTime,pingTime);

    }
}