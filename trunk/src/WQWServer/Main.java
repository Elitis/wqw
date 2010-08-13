package WQWServer;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

/**
 * @author XVII
 * Starts the server.
 */

public class Main {
    public static final boolean DEBUG = true;
    public static Server server;
    public static ServerGUI gui;
    public static int port = 5588;
    public static String serverIP;
    public static String serverName;
    protected static Properties serverConf = new Properties();
    protected static Properties serverCheck = new Properties();
    public static int version = 77;
    public static SQLDatabase sql;
    protected static boolean active = false;
    
    /**
     *  Write the message to the GUI.
     */
    public static void debug(String label, String msg) {
        if (DEBUG && Main.gui != null) {
            Main.gui.write(label + ": " + msg);
        }
    }

   public static String getip(Socket sock)
    {
       String s = sock.getInetAddress().toString();
        return s.substring(0,0)+s.substring(1);
    }

    private static void loadconfigs()
    {
        try
        {
        FileInputStream fin = new FileInputStream("configs/server.conf");
        serverConf.load(fin);
        fin.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        serverIP = serverConf.getProperty("Server_IP");
    }
    
    /**
     * Starts server and the GUI for messages.
     */
    public static void main(String[] args) {
        try {
            gui = new ServerGUI(server);
            gui.setTitle("WinQuest Worlds - V"+((double) version/100));
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
  
            gui.jButton1.setEnabled(false);
            gui.jButton2.setEnabled(false);
            gui.jButton3.setEnabled(false);
            gui.jButton5.setEnabled(false);
            gui.jButton6.setEnabled(false);
            gui.jButton8.setEnabled(false);
        }
        catch (Exception e) {
            debug("Main", "Exception (main)" + e.getMessage());
        }
    }

    protected static void startServer() {
        try {
            if (!active /*&& loadcheck()*/) {
                loadconfigs();

                sql = new SQLDatabase("Server");
                sql.start();

                server = new Server(port);
                server.start();
                server.lobbyinit();

                boolean stop = false;

                ResultSet is = Main.sql.doquery("SELECT version FROM wqw_settings");
                if (is.next()) {
                    int ver = is.getInt("version");
                    if (ver != version) {
                        debug("[Notice]", "Your server is outdated. Please contact XVII for the latest version.");
                        stopServer();
                        stop = true;
                    } else {
                        Main.sql.doupdate("UPDATE wqw_servers SET online=1 WHERE ip='"+serverIP+"'");
                    }
                } else {
                    debug("[Notice]", "Can't find the server settings in the db (Table wqw_settings).");
                    stopServer();
                    stop = true;
                }
                is.close();

                if (!stop) {
                    ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_servers WHERE ip='"+serverIP+"'");
                    if (rs.next()) {
                        serverName = rs.getString("name");
                        Main.sql.doupdate("UPDATE wqw_servers SET online=1, count=0 WHERE ip='"+serverIP+"'");
                    } else {
                        serverName = "Test";
                    }
                    rs.close();
                    active = true;
                    debug("[Server]", "Server started (Port " + port + ")");
                    Main.gui.setClientCount("Server Online - 0 Clients");
                    Main.gui.newTimer();

                    gui.jButton1.setEnabled(true);
                    gui.jButton2.setEnabled(true);
                    gui.jButton3.setEnabled(true);
                    //gui.jButton5.setEnabled(true);
                    gui.jButton6.setEnabled(true);
                    gui.jButton8.setEnabled(true);
                    gui.jButton7.setEnabled(false);
                }
            }
        } catch (Exception e) {
            debug("Main", "Exception (main, startServer)" + e.getMessage());
        }
    }

    protected static void stopServer() {
        try {
            if (active) {
                Main.gui.setClientCount("Server Offline");
                active = false;
                Main.sql.doupdate("UPDATE wqw_users SET curServer=''");
                Main.sql.doupdate("UPDATE wqw_servers SET online=0, count=0 WHERE name='"+Main.serverName+"'");
                server.finalize();
                server = null;
                sql = null;
                Main.gui.timer.cancel();
                gui.jButton1.setEnabled(false);
                gui.jButton2.setEnabled(false);
                gui.jButton3.setEnabled(false);
                gui.jButton5.setEnabled(false);
                gui.jButton6.setEnabled(false);
                gui.jButton8.setEnabled(false);
                gui.jButton7.setEnabled(true);
                debug("[Server]", "Server stopped");
            }
        } catch (Exception e) {
            debug("Main", "Exception (main, stopServer) " + e.getMessage());
            server = null;
            sql = null;
        }
    }
}
