package WQWServer;

import java.util.*;
import java.net.*;

/**
 * @author XVII
 * The server handles the client connections and allocates them a thread each.
 */
public class Server extends Thread {

    protected int port;
    protected ServerSocket serverSocket;
    protected boolean listening;
    protected Vector<ServerConnection> clientConnections;
    
    public static GameServer gameServer = new GameServer();

    /**
     * Creates a new instance of RelayServer.
     */
    public Server(int serverPort) {
        this.port = serverPort;
        this.listening = false;
        this.clientConnections = new Vector<ServerConnection>();
    }
    
    /**
     * Gets the server's port.
     */
    public int getPort() {
        return this.port;
    }
    
    /**
     * Gets the server's listening status.
     */
    public boolean getListening() {
        return this.listening;
    }

    public int getClientCount() {
        return this.clientConnections.size();
    }
    
    /**
     * Roots a debug message to the main application.
     */
    protected void debug(String msg) {
        Main.debug("Server (" + this.port + ")", msg);
    }

    protected void lobbyinit() {
        gameServer.init();
    }

    /**
     * Removes a client from the server (it's expected that the client closes its own connection).
     */
    public boolean remove(SocketAddress remoteAddress) {
        try {
             for (int i = 0; i < this.clientConnections.size(); i++) {
                ServerConnection client = this.clientConnections.get(i);
                if (client.getRemoteAddress().equals(remoteAddress)) {
                    this.clientConnections.remove(i);
                    return true;
                }
            }
        }
        catch (Exception e) {
            debug("Exception (remove): " + e.getMessage());
        }
        return false;
    }

    /**
     * Relays PvP to the other user
     */
    public void setPvP(SocketAddress remoteAddress, int turn, int userid, int weaponlevel2) {
        try {
             for (int i = 0; i < this.clientConnections.size(); i++) {
                ServerConnection client = this.clientConnections.get(i);
                if (client.getRemoteAddress().equals(remoteAddress)) {
                    client.playerAttack("aa", turn+1, "p", userid, weaponlevel2);
                }
            }
        }
        catch (Exception e) {
            debug("Exception (set pvp): " + e.getMessage());
        }
    }
    
    /**
     * Relays PvP to the other user
     */
    public void startPvP(SocketAddress remoteAddress, SocketAddress remoteAddressOther) {
        try {
            int userid2 = 0;
            int weaponlevel2 = 0;
            for (int i = 0; i < this.clientConnections.size(); i++) {
                ServerConnection client = this.clientConnections.get(i);
                if (client.getRemoteAddress().equals(remoteAddressOther)) {
                    userid2 = client.accountid;
                    weaponlevel2 = client.weaponlevel;
                    break;
                }
            }
            for (int i = 0; i < this.clientConnections.size(); i++) {
                ServerConnection client = this.clientConnections.get(i);
                if (client.getRemoteAddress().equals(remoteAddress)) {
                    client.playerAttack("aa", 0, "p", userid2, weaponlevel2);
                }
            }
        }
        catch (Exception e) {
            debug("Exception (set pvp): " + e.getMessage());
        }
    }

    
    /**
     * Waits for clients' connections and handles them to a new RelayServerConnection.
     */
    public void run() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            this.listening = true;

            while (this.listening) {
                Socket socket = this.serverSocket.accept();
                if (!gameServer.checkInUse(socket)) {
                    debug("Client connected: " + socket.getRemoteSocketAddress());
                    ServerConnection socketConnection = new ServerConnection(socket,this,gameServer);
                    clientConnections.add(socketConnection);
                }
            }
        }
        catch (Exception e) {
            if (!e.getMessage().equals("socket closed")) {
                debug("Exception (run): " + e.getMessage());
            }
        }
    }
    
    /**
     * Closes the server's socket.
     */
    @Override protected void finalize() {
        try {
            this.listening = false;
            for (int i = 0; i < this.clientConnections.size(); i++) {
                ServerConnection client = this.clientConnections.get(i);
                if (client != null) {
                    client.finalize();
                    client = null;
                }
            }
            gameServer.finalize();
            this.serverSocket.close();
        }
        catch (Exception e) {
            debug("Exception (finalize): " + e.getMessage());
        }
    }
}
