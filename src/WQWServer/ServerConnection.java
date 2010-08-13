package WQWServer;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

/**
 * @author XVII
 * ServerConnection - Processes packets from a single client.
 */
public class ServerConnection extends Thread {
    protected Socket socket;
    protected BufferedReader socketIn;
    protected PrintWriter socketOut;
    protected Server server;
    protected GameServer gameServer;
    protected Functions functions;
    protected String account;
    protected int accountid;
    protected int userid;
    protected String ip;
    protected int port;
    protected boolean hasFinalized;
    protected Room playerRoom;
    protected int playerSlot;
    protected int partyID;
    protected int monfighting;
    protected int playerlevel;
    protected int weaponlevel;
    protected boolean fighting;
    protected int monkilled;
    protected int packetsend;
    protected String[] friends = new String[20];
    pvpTimer pvpTime = new pvpTimer();
    pingTimer ping = new pingTimer();
    Random generator = new Random();
    
    /**
     * Creates a new instance of RelayServerConnection.
     */
    public ServerConnection(Socket socket, Server server, GameServer _gameServer) {
        this.socket = socket;
        this.server = server;
        this.ip = Main.getip(socket);
        this.port = socket.getPort();
        this.gameServer = _gameServer;
        this.start();
    }
    
    /**
     * Roots a debug message to the main application.
     */

    protected void debug(String msg) {
        Main.debug("ServerConnection (" + this.socket.getRemoteSocketAddress() + ")", msg);
    }

    public SocketAddress getRemoteAddress() {
        return this.socket.getRemoteSocketAddress();
    }

    protected String getcmd(String packet)
    {
        if (packet.startsWith("<")) {
            int endArrow = packet.indexOf(">");
            int endSlash = packet.indexOf("/>");
            if (endSlash < 0) {
                endSlash = endArrow + 1;
            }
            if (endSlash < endArrow) {
                return packet.substring(1,endSlash);
            } else {
                return packet.substring(1,endArrow);
            }
        } else if (packet.startsWith("%")) {
            String packet_handled[] = packet.split("%");
            return packet_handled[3];
        }
        return "Error";
    }

    protected void parseCMD(String cmd, String packet)
    {
        try{
            this.packetsend += 1;
            if (this.packetsend > 300) {
                gameServer.banPlayer(this.account,"the server for packet spamming");
            }
            Packet recvPack = new Packet();
            recvPack.setPacket(packet);
            if (cmd.equals("addFriend")) {
                /* Accept friend request */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                addFriend(this.account, packet_handled[2], true);
            } else if (cmd.equals("afk")) {
                /* Away from keyboard */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                setAFK(Boolean.parseBoolean(packet_handled[2]));
            } else if (cmd.equals("bankFromInv")) {
                /* Take from bank */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                bankFromInv(Integer.parseInt(packet_handled[2]),Integer.parseInt(packet_handled[3]));
            } else if (cmd.equals("bankToInv")) {
                /* Take from bank */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                bankToInv(Integer.parseInt(packet_handled[2]),Integer.parseInt(packet_handled[3]));
            } else if (cmd.equals("buyBagSlots")) {
                /* Buy bag slots */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                buyBagSlots(Integer.parseInt(packet_handled[2]));
            } else if (cmd.equals("buyBankSlots")) {
                /* Buy bank slots */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                buyBankSlots(Integer.parseInt(packet_handled[2]));
            } else if (cmd.equals("buyHouseSlots")) {
                /* Buy house slots */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                buyHouseSlots(Integer.parseInt(packet_handled[2]));
            } else if (cmd.equals("buyItem")) {
                /* Buy item */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                buyItem(Integer.parseInt(packet_handled[2]),Integer.parseInt(packet_handled[3]));
            } else if (cmd.equals("cc")) {
                /* Canned chat */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                cannedChat(packet_handled[2]);
            } else if (cmd.equals("cmd")) {
                /* Switch on the cmd command */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                String cmdSwitch = packet_handled[2];
                if (cmdSwitch.equals("tfer")) {
                    /* Joining a new room */
                    if (packet_handled[4].indexOf("-") > 0) {
                        String room[] = packet_handled[4].split("-");
                        joinRoom(room[0], Integer.parseInt(room[1]), "Enter", "Spawn");
                    } else {
                        if (packet_handled.length > 6) {
                            joinRoom(packet_handled[4], -1, packet_handled[5], packet_handled[6]);
                        } else {
                            joinRoom(packet_handled[4], -1, "Enter", "Spawn");
                        }
                    }
                } else if (cmdSwitch.equals("goto")) {
                    Packet sendPack = new Packet();

                    if(gameServer.getPlayerID(packet_handled[4]) != -1){
                        int room[] = gameServer.getPlayerRoom(packet_handled[4]);
                        String roomname = gameServer.getRoomName(room[0]);

                        if(roomname.equals("null") == false){
                            joinRoom(roomname, room[1], "Enter", "Spawn");
                        }
                    } else {
                        sendPack.addString("%xt%warning%-1%User '"+packet_handled[4]+"' could not be found.%");
                        send(sendPack, true);
                    }
                } else if (cmdSwitch.equals("pvp")) {
                    /* Attack a player */
                    startPvP(packet_handled[3]);
                } else {
                    debug("Parse unknown packet (cmd): "+cmd);
                }
            }  else if (cmd.equals("declineFriend")) {
                /* Decline friend request */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                requestFriend(packet_handled[2]);
            } else if (cmd.equals("deleteFriend")) {
                /* Delete friend */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                deleteFriend(this.account, packet_handled[3], true);
            } else if (cmd.equals("emotea")) {
                /* Emotes */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                emoteChat(packet_handled[2]);
            } else if (cmd.equals("equipItem")) {
                /* Equipping */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                equipItem(Integer.parseInt(packet_handled[2]), Integer.parseInt(packet_handled[3]));
            } else if (cmd.equals("firstJoin")) {
                sendLobby();
                loadSkills();
            } else if (cmd.equals("gar")) {
                /* Player attack */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                playerAttack(packet_handled[3], Integer.parseInt(packet_handled[4]), packet_handled[5], Integer.parseInt(packet_handled[6]), 0);
            } else if (cmd.equals("getQuests")) {
                /* Get Quests */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                String quests = "%";
                for (int i = 2; i < packet_handled.length-1; i++) {
                    quests += packet_handled[i] + "%";
                }
                getQuests(quests);
            } else if (cmd.equals("gp")) {
                /* Switch on the gp command */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                String gpSwitch = packet_handled[2];
                if (gpSwitch.equals("pi")) {
                    /* Inviting to a party */
                    partyInvite(packet_handled[3]);
                } else if (gpSwitch.equals("pd")) {
                    /* Declining an invitation */
                    partyDecline(Integer.parseInt(packet_handled[3]));
                } else if (gpSwitch.equals("pa")) {
                    /* Accepting an invitation */
                    partyAccept(Integer.parseInt(packet_handled[3]));
                } else if (gpSwitch.equals("pl")) {
                    /* Leaving a party */
                    partyLeave();
                } else if (gpSwitch.equals("pk")) {
                    /* Kick a player from the party */
                    partyKick(packet_handled[3]);
                } else if (gpSwitch.equals("pp")) {
                    /* Promote a player to leader of the party */
                    partyPromote(packet_handled[3]);
                } else if (gpSwitch.equals("ps")) {
                    /* Summon a player */
                    partySummon(packet_handled[3]);
                } else if (gpSwitch.equals("psd")) {
                    /* Decline a summon */
                    partySummonDecline(packet_handled[3]);
                } else if (gpSwitch.equals("psd")) {
                    /* Accept a summon */
                    partySummonAccept();
                } else {
                    debug("Parse unknown packet (gp): "+gpSwitch);
                }
            } else if (cmd.equals("hi")) {
                /* Attack disconnect timer */
                playerTimerAttack();
            } else if (cmd.equals("loadBank")) {
                /* Load Bank */
                loadBank();
            } else if (cmd.equals("loadFriendsList")) {
                /* Load Friends List */
                loadFriends();
            } else if (cmd.equals("loadHouseInventory")) {
                /* Load House Inventory */
                loadHouseInventory();
            } else if (cmd.equals("loadShop")) {
                /* Load Shop */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                loadShop(Integer.parseInt(packet_handled[2]));
            } else if (cmd.equals("msg t='sys'")) {
                /* Switch on the system command */
                recvPack.removeHeader();
                String sysSwitch = recvPack.getXMLSingle("body action");
                if (sysSwitch.equals("verChk")) {
                    /* Sends the version */
                    sendVersion();
                } else if (sysSwitch.equals("login")) {
                    /* Logs the player in */
                    doLogin(recvPack.getCDATA(recvPack.getXML("nick")), recvPack.getCDATA(recvPack.getXML("pword")), true);
                } else {
                    debug("Parse unknown packet (sys): "+cmd);
                }
            } else if (cmd.equals("message")) {
                /* Send chat data */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                if (packet_handled[2].startsWith("!")) {
                    userCommand(packet_handled[2].substring(1,packet_handled[2].length()));
                } else {
                    userChat(Integer.parseInt(packet_handled[1]),packet_handled[2],packet_handled[3]);
                }
            } else if (cmd.equals("moveToCell")) {
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                moveToCell(packet_handled[2],packet_handled[3]);
            } else if (cmd.equals("mv")) {
                /* Send move data */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                userMove(Integer.parseInt(packet_handled[2]),Integer.parseInt(packet_handled[3]),Integer.parseInt(packet_handled[4]), false);
            } else if (cmd.equals("policy-file-request")) {
                /* Send the policy */
                sendPolicy();
            } else if (cmd.equals("resPlayerTimed")) {
                /* Respawn player */
                respawnPlayer();
            } else if (cmd.equals("restRequest")) {
                /* Rest player */
                restPlayer();
            } else if (cmd.equals("retrieveInventory")) {
                /* Get the inventory details */
                loadBigInventory();
            } else if (cmd.equals("retrieveUserData")) {
                /* Send user data */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                retrieveUserData(Integer.parseInt(packet_handled[2]), true);
            } else if (cmd.equals("retrieveUserDatas")){
                recvPack.removeHeader();
                retrieveUserDatas(recvPack.getPacket());
            } else if (cmd.equals("requestFriend")) {
                /* Add friend request */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                requestFriend(packet_handled[2]);
            } else if (cmd.equals("sellItem")) {
                /* Sell item */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                sellItem(Integer.parseInt(packet_handled[2]),Integer.parseInt(packet_handled[4]));
            } else if (cmd.equals("unequipItem")) {
                /* Unequipping */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                unequipItem(Integer.parseInt(packet_handled[2]), Integer.parseInt(packet_handled[3]));
            } else if (cmd.equals("whisper")) {
                /* Whisper */
                recvPack.removeHeader();
                String packet_handled[] = recvPack.getPacket().split("%");
                whisperChat(packet_handled[2], packet_handled[3]);
            } else if (cmd.equals("Error")) {
                debug("Error in reading packet: "+packet);
            } else {
                debug("Parse Unknown Packet: "+cmd);
            }
        } catch(Exception e){
            debug("Error in parseCMD: "+e.getMessage()+", Cause: "+e.getCause()+", Packet: "+packet);
        }
    }

    /*
     * START COMMAND RESPONSES
     * The following section contains responses for the messages sent to the server by the client
     * Above and below this section are the core functions
     */

    protected void addFriend(String thischar, String otherchar, boolean repeat)
    {
        try {
            Packet sendPack = new Packet();
            String[] account2 = new String[1];
            account2[0] = ""+gameServer.userID[gameServer.getPlayerID(thischar)];
            String friends[];

            ResultSet rs2 = Main.sql.doquery("SELECT COUNT(*) AS rowcount FROM wqw_friends WHERE userid="+account2[0]);

            ResultSetMetaData rsMetaData = rs2.getMetaData();

            int numberOfColumns = rsMetaData.getColumnCount();

            if (numberOfColumns == 0){
                Main.sql.doupdate("INSERT INTO wqw_friends (userid) VALUES ("+this.userid+")");
            }
            rs2.close();

            if (numberOfColumns < 10) {
                ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_friends WHERE userid="+account2[0]);
                if (rs.next()) {
                    if (!rs.getString("friendid").equals("")) {
                        friends = rs.getString("friendid").split(",");
                        Main.sql.doupdate("UPDATE wqw_friends SET friendid=CONCAT(friendid, "+"',', "+gameServer.userID[gameServer.getPlayerID(otherchar)]+") WHERE userid="+account2[0]);
                        sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"addFriend\",\"friend\":");
                        rs.close();
                        ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+gameServer.userID[gameServer.getPlayerID(otherchar)]);
                        if (is.next()) {
                            sendPack.addString("{\"iLvl\":\""+is.getInt("level")+"\",\"ID\":\""+is.getInt("id")+"\",\"sName\":\""+is.getString("username")+"\",\"sServer\":\""+is.getString("curServer")+"\"}");
                        }
                        sendPack.addString("}}}");
                        gameServer.writePlayerPacket(thischar, sendPack, true);
                        sendPack.clean();
                        sendPack.addString("%xt%server%-1%"+otherchar+" added to your friends list.%");
                        gameServer.writePlayerPacket(thischar, sendPack, true);
                        is.close();
                    } else {
                        friends = rs.getString("friendid").split(",");
                        Main.sql.doupdate("UPDATE wqw_friends SET friendid=CONCAT(friendid, "+gameServer.userID[gameServer.getPlayerID(otherchar)]+") WHERE userid="+account2[0]);
                        sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"addFriend\",\"friend\":");
                        rs.close();
                        ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+gameServer.userID[gameServer.getPlayerID(otherchar)]);
                        if (is.next()) {
                            sendPack.addString("{\"iLvl\":\""+is.getInt("level")+"\",\"ID\":\""+is.getInt("id")+"\",\"sName\":\""+is.getString("username")+"\",\"sServer\":\""+is.getString("curServer")+"\"}");
                        }
                        sendPack.addString("}}}");
                        gameServer.writePlayerPacket(thischar, sendPack, true);
                        sendPack.clean();
                        sendPack.addString("%xt%server%-1%"+otherchar+" added to your friends list.%");
                        gameServer.writePlayerPacket(thischar, sendPack, true);
                        is.close();
                    }
                }
            }
            if (repeat) {
                addFriend(otherchar, thischar, false);
            }
            rs2.close();
        } catch (Exception e) {
            debug("Exception in add friend: "+e.getMessage());
        }
    }

    protected void addGold(int gold)
    {
        Main.sql.doupdate("UPDATE wqw_users SET gold=gold+"+gold+" WHERE id="+this.userid);
    }
    
    protected int getXpToLevel(int playerlevel)
    {
        if (playerlevel < 100) {
            return (250*((playerlevel)/2)*((playerlevel/2)*(playerlevel+1)/playerlevel)*(playerlevel/2)+100);
        }
        return 2000000000;
    }

    protected String addRewards(int xpreward, int goldreward, int cpreward, int monstertype, String type, int monsterid)
    {
        Packet sendPack2 = new Packet();
        try {
            Main.sql.doupdate("UPDATE wqw_users SET gold=gold+"+goldreward+", xp=xp+"+xpreward+" WHERE id="+userid);
            Main.sql.doupdate("UPDATE wqw_items SET classXP=classXP+"+cpreward+" WHERE userid="+userid+" AND equipped=1 AND sES='ar'");
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+this.userid);
            if (rs.next()) {
                int gold = rs.getInt("gold");
                int xp = rs.getInt("xp");
                sendPack2.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"id\":\""+monsterid+"\",\"iCP\":"+this.getClassPoints(this.userid)+",\"cmd\":\"addGoldExp\",\"intGold\":"+gold+",\"intExp\":"+xp+",\"typ\":\""+type+"\"}}}");
                if (xp >= getXpToLevel(this.playerlevel)) {
                    Main.sql.doupdate("UPDATE wqw_users SET level=level+1, xp=0 WHERE id="+userid);
                    this.playerlevel += 1;
                    Packet pack = new Packet();
                    pack.addString("%xt%uotls%-1%"+this.account+"%intLevel:"+this.playerlevel+"%");
                    gameServer.writeMapPacket(this.account, pack, true, false);
                    pack.clean();
                    gameServer.hp[this.accountid] = 700+((this.playerlevel+1)*20);
                    gameServer.mp[this.accountid] = 19+this.playerlevel;
                    pack.addString("%xt%uotls%-1%"+this.account+"%intHP:"+gameServer.hp[this.accountid]+",intMP:"+gameServer.mp[this.accountid]+",intHPMax:"+gameServer.hpmax[this.accountid]+",intMPMax:"+gameServer.mpmax[this.accountid]+"%");
                    gameServer.writeMapPacket(this.account, pack, true, false);
                    pack.clean();
                    pack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"levelUp\",\"intExpToLevel\":\""+getXpToLevel(this.playerlevel)+"\",\"intLevel\":\""+this.playerlevel+"\"}}}");
                    send(pack, true);
                    pack.clean();
                    pack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"intExpToLevel\":\""+getXpToLevel(this.playerlevel)+"\",\"iCP\":"+getClassPoints(this.userid)+",\"cmd\":\"levelUp\",\"intGold\":"+gold+",\"intLevel\":\""+this.playerlevel+"\",\"intExp\":0,\"typ\":\"m\"}}}");
                    send(pack, true);
                }
            }
            rs.close();
            ResultSet es = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+this.userid+" AND equipped=1 AND sES='ar'");
            if (es.next()) {
                int cp = es.getInt("classXP");
                if (cp >= (1800*es.getInt("iLvl")*(es.getInt("iLvl")/2))) {
                    //Took out resetting classXP because I'm not sure if it resets to 0
                    Main.sql.doupdate("UPDATE wqw_items SET iLvl=iLvl+1 WHERE userid="+userid+" AND equipped=1 AND sES='ar'");
                }
            }
            es.close();
        } catch (Exception e) {
            debug("Exception in add rewards: "+e.getMessage());
        }
        return sendPack2.getPacket();
    }

    protected String addMonsterRewards(int monstertype, String type, int monsterid)
    {
        try {
            Random r = new Random();
            int randint = r.nextInt(20);
            int goldreward = 0;
            int xpreward = 0;
            int cpreward = 0;
            if (type.equals("p")) {
                goldreward = (gameServer.level[monsterid]*2)*gameServer.goldrate + randint*2;
                xpreward = (gameServer.level[monsterid]*4)*gameServer.xprate + randint*4;
                cpreward = xpreward/2 + randint;
            } else {
                ResultSet is = Main.sql.doquery("SELECT * FROM wqw_monsters WHERE MonID="+monstertype);
                if (is.next()) {
                    goldreward = is.getInt("intGold")*gameServer.goldrate;
                    xpreward = is.getInt("intExp")*gameServer.xprate;
                    cpreward = xpreward/2 + randint;
                }
            }
            return addRewards(xpreward, goldreward, cpreward, monstertype, type, monsterid);
        } catch (Exception e) {
            debug("Exception in add monster rewards: "+e.getMessage());
        }
        return "";
    }

    protected void bankFromInv(int itemid, int adjustid)
    {
        try {
            Packet sendPack = new Packet();
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+this.userid+" AND bBank=1");
            int i = 0;
            while (rs.next()) {
                i++;
            }
            rs.close();
            ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+this.userid);
            if (is.next()) {
                if (is.getInt("slotBank") <= i) {
                    sendPack.addString("%xt%warning%-1%You have the maximum items you can in your bank.%");
                } else {
                    Main.sql.doupdate("UPDATE wqw_items SET bBank=1 WHERE userid="+this.userid+" AND bBank=0 AND id="+adjustid);
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"ItemID\":"+itemid+",\"cmd\":\"bankFromInv\"}}}");
                }
            }
            is.close();
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in bank from invent: "+e.getMessage());
        }
    }

    protected void bankToInv(int itemid, int adjustid)
    {
        try {
            Packet sendPack = new Packet();
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+this.userid+" AND bBank=0");
            int i = 0;
            while (rs.next()) {
                i++;
            }
            rs.close();
            ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+this.userid);
            if (is.next()) {
                if (is.getInt("slotBag") <= i) {
                    sendPack.addString("%xt%warning%-1%You have the maximum items you can in your inventory.%");
                } else {
                    Main.sql.doupdate("UPDATE wqw_items SET bBank=0 WHERE userid="+this.userid+" AND bBank=1 AND id="+adjustid);
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"ItemID\":"+itemid+",\"cmd\":\"bankToInv\"}}}");
                }
            }
            is.close();
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in bank to invent: "+e.getMessage());
        }
    }

    protected void buyBankSlots(int amount)
    {
        try {
            Packet sendPack = new Packet();
            boolean doContinue = true;
            int coins = 0;
            int curSlots = 0;
            ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+this.userid);
            if (is.next()) {
                coins = is.getInt("coins");
                curSlots = is.getInt("slotBank");
            }
            if (curSlots > 39) {
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyBankSlots\",\"bitSuccess\":0,\"strMessage\":\"You have the maximum bank slots avaliable.\",\"iSlots\":0}}}");
                doContinue = false;
            }
            if (doContinue) {
                if (coins < (200*amount)) {
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyBankSlots\",\"bitSuccess\":0,\"strMessage\":\"You do not have enough coins to buy that many slots.\",\"iSlots\":0}}}");
                } else {
                    Main.sql.doupdate("UPDATE wqw_users SET coins=coins-"+(200*amount)+", slotBank=slotBank+"+amount+" WHERE id="+this.userid);
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyBankSlots\",\"bitSuccess\":1,\"iSlots\":"+amount+"}}}");
                }
            }
            is.close();
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in buy bank slots: "+e.getMessage());
        }
    }

    protected void buyBagSlots(int amount)
    {
        try {
            Packet sendPack = new Packet();
            boolean doContinue = true;
            int coins = 0;
            int curSlots = 0;
            ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+this.userid);
            if (is.next()) {
                coins = is.getInt("coins");
                curSlots = is.getInt("slotBag");
            }
            if (curSlots > 39) {
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyBagSlots\",\"bitSuccess\":0,\"strMessage\":\"You have the maximum bag slots avaliable.\",\"iSlots\":0}}}");
                doContinue = false;
            }
            if (doContinue) {
                if (coins < (200*amount)) {
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyBagSlots\",\"bitSuccess\":0,\"strMessage\":\"You do not have enough coins to buy that many slots.\",\"iSlots\":0}}}");
                } else {
                    Main.sql.doupdate("UPDATE wqw_users SET coins=coins-"+(200*amount)+", slotBag=slotBag+"+amount+" WHERE id="+this.userid);
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyBagSlots\",\"bitSuccess\":1,\"iSlots\":"+amount+"}}}");
                }
            }
            is.close();
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in buy bank slots: "+e.getMessage());
        }
    }

    protected void buyHouseSlots(int amount)
    {
        try {
            Packet sendPack = new Packet();
            boolean doContinue = true;
            int coins = 0;
            int curSlots = 0;
            ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+this.userid);
            if (is.next()) {
                coins = is.getInt("coins");
                curSlots = is.getInt("slotHouse");
            }
            if (curSlots > 39) {
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyHouseSlots\",\"bitSuccess\":0,\"strMessage\":\"You have the maximum house slots avaliable.\",\"iSlots\":0}}}");
                doContinue = false;
            }
            if (doContinue) {
                if (coins < (200*amount)) {
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyHouseSlots\",\"bitSuccess\":0,\"strMessage\":\"You do not have enough coins to buy that many slots.\",\"iSlots\":0}}}");
                } else {
                    Main.sql.doupdate("UPDATE wqw_users SET coins=coins-"+(200*amount)+", slotHouse=slotHouse+"+amount+" WHERE id="+this.userid);
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyHouseSlots\",\"bitSuccess\":1,\"iSlots\":"+amount+"}}}");
                }
            }
            is.close();
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in buy bank slots: "+e.getMessage());
        }
    }

    protected void buyItem(int itemid, int shopid)
    {
        try {
            Packet sendPack = new Packet();
            boolean doContinue = true;
            int gold = 0;
            int buyprice = 1;
            int adjustid = 0;
            int level = 1;
            String sES = "";
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemID="+itemid);
            if (rs.next()) {
                buyprice = rs.getInt("iCost");
                level = rs.getInt("iLvl");
                sES = rs.getString("sES");
            } else {
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyItem\",\"bitSuccess\":0,\"strMessage\":\"Item Does Not Exist\",\"CharItemID\":-1}}}");
                doContinue = false;
            }
            if (doContinue) {
                rs.close();
                ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+this.userid);
                if (is.next()) {
                    gold = is.getInt("gold");
                }
                is.close();
                if ((gold - buyprice) >= 0) {
                    Main.sql.doupdate("INSERT INTO wqw_items (itemid, userid, sES, iLvl) VALUES ("+itemid+", "+this.userid+", '"+sES+"', '"+level+"')");
                    Main.sql.doupdate("UPDATE wqw_users SET gold="+(gold-buyprice)+" WHERE id="+this.userid);
                } else {
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyItem\",\"bitSuccess\":0,\"strMessage\":\"Not Enough Gold\",\"CharItemID\":-1}}}");
                    doContinue = false;
                }
            }
            if (doContinue) {
                ResultSet es = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+this.userid+" AND itemid="+itemid+" AND sES='"+sES+"'");
                if (es.next()) {
                    adjustid = es.getInt("id");
                }
                es.close();
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"buyItem\",\"bitSuccess\":1,\"CharItemID\":");
                sendPack.addInt(adjustid);
                sendPack.addString("}}}");
            }
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in buy item: "+e.getMessage());
        }
    }

    protected void cannedChat(String chat)
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%cc%-1%");
        sendPack.addString(chat);
        sendPack.addString("%"+this.account+"%");
        gameServer.writeMapPacket(this.account,sendPack,true,false);
        debug("Sent canned chat: "+this.account+", "+chat);
    }

    protected void declineFriend(String otherchar)
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%server%-1%You declined the friend request.%");
        send(sendPack, true);
        sendPack.clean();
        sendPack.addString("%xt%server%-1%"+this.account+" declined your friend request.%");
        gameServer.writePlayerPacket(otherchar, sendPack, true);
    }

    protected void deleteFriend(String thischar, String otherchar, boolean repeat)
    {
        try {
            Packet sendPack = new Packet();
            String account2 = ""+gameServer.userID[gameServer.getPlayerID(thischar)];
            String account3 = ""+gameServer.userID[gameServer.getPlayerID(otherchar)];
            String[] temp;
            String newFriend = "";
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_friends WHERE userid="+account2);
            if (rs.next()) {
                temp = rs.getString("friendid").split(",");
                if (temp[temp.length-1].equals(account3)) {
                    newFriend = rs.getString("friendid").replace(account3,"");
                } else {
                    newFriend = rs.getString("friendid").replace(account3+",","");
                }
                Main.sql.doupdate("UPDATE wqw_friends SET friendid='"+newFriend+"' WHERE userid="+account2);
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"deleteFriend\",\"ID\":"+account3+"}}}");
                gameServer.writePlayerPacket(thischar, sendPack, true);
            }
            if (repeat) {
                deleteFriend(otherchar, thischar, false);
            }
            rs.close();
        } catch (Exception e) {
            debug("Exception in delete friend: "+e.getMessage());
        }
    }

    protected void doLogin(String user, String pass, boolean repeat)
    {
        String[] us = user.split("~");
        user = us[1];

        Packet sendPack = new Packet();

        sendPack.addString("%xt%server%-1%Accepting party invites.%");
        send(sendPack,true);
        sendPack.clean();

        sendPack.addString("%xt%server%-1%Accepting goto requests.%");
        send(sendPack,true);
        sendPack.clean();

        sendPack.addString("%xt%server%-1%Accepting Friend requests.%");
        send(sendPack,true);
        sendPack.clean();

        sendPack.addString("%xt%server%-1%Accepting PMs.%");
        send(sendPack,true);
        sendPack.clean();

        sendPack.addString("%xt%server%-1%Ability ToolTips will always show on mouseover.%");
        send(sendPack,true);
        sendPack.clean();

        sendPack.addString("%xt%loginResponse%-1%");
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_users WHERE username='"+user+"' AND password='"+pass+"' AND banned=0 LIMIT 1");
            if (rs.next()) {
                this.userid = rs.getInt("id");
                int result = gameServer.adduser(user, socketOut, socket, rs.getInt("level"));
                if (result != -1) {
                    String message = getMessage();
                    while (message.equals("")) {
                        message = getMessage();
                    }
                    String news = getNews();
                    while (news.equals("")) {
                        news = getNews();
                    }
                    sendPack.addString("true%"+result+"%"+user+"%"+message+"%"+pass+"%"+news+"%");
                    gameServer.userID[result] = this.userid;
                    debug("1: "+gameServer.userID[result]+", "+this.userid);
                    this.accountid = result;
                    this.account = user;
                    this.playerRoom = gameServer.room[gameServer.getPlayerRoom(this.account)[0]][gameServer.getPlayerRoom(this.account)[1]];
                    this.playerSlot = this.playerRoom.getPlayerSlot(this.account);
                    Main.sql.doupdate("UPDATE wqw_users SET curServer='"+Main.serverName+"' WHERE username='"+this.account+"'");
                    ping.main(this);
                } else {
                    sendPack.addString("0%-1%%This server is full, please select another..%");
                }
            } else {
                sendPack.addString("0%-1%%User Data could not be retrieved. Please contact the WinQuest Worlds staff to resolve the issue.%");
            }
            rs.close();
            send(sendPack,true);
            debug("Sent login: "+user+", "+pass);
        } catch (Exception e) {
            debug("Exception in do login: "+e.getMessage());
            if (repeat) {
                doLogin(user, pass, false);
            } else {
                this.finalize();
            }
        }
    }

    protected void emoteChat(String chat)
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%emotea%-1%");
        sendPack.addString(chat);
        sendPack.addString("%"+this.accountid+"%");
        gameServer.writeMapPacket(this.account,sendPack,true,false);
        debug("Sent canned chat: "+this.account+", "+chat);
    }

    protected void equipItem(int itemid, int adjustid)
    {
        try {
            Packet sendPack = new Packet();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"uid\":");
            sendPack.addInt(this.accountid);
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemID="+itemid);
            if (rs.next()) {
                sendPack.addString(",\"ItemID\":\""+itemid+"\",\"strES\":\""+rs.getString("sES")+"\",\"cmd\":\"equipItem\",\"sFile\":\""+rs.getString("sFile")+"\",\"sLink\":\""+rs.getString("sLink")+"\"}}}");
            }
            String type = rs.getString("sES");
            rs.close();

            if (type.equals("Weapon")) {
                ResultSet is = Main.sql.doquery("SELECT * FROM wqw_items WHERE id="+adjustid);
                if (is.next()) {
                    this.weaponlevel = is.getInt("iLvl");
                }
                is.close();
            }
            Main.sql.doupdate("UPDATE wqw_items SET equipped=0 WHERE userid="+this.userid+" AND equipped=1 AND sES='"+type+"'");
            Main.sql.doupdate("UPDATE wqw_items SET equipped=1 WHERE userid="+this.userid+" AND id="+adjustid);
            gameServer.writeMapPacket(this.account, sendPack, true, false);
            sendPack.clean();
            
        } catch (Exception e) {
            debug("Exception in equip item: "+e.getMessage()+", itemid: "+itemid+", adjustid: "+adjustid);
        }
    }

    protected String getClassName(int id)
    {
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+id+" AND equipped=1 AND sES='ar'");
            if (rs.next()) {
                return rs.getString("className");
            }
            rs.close();
        } catch (Exception e) {
            debug("Exception in get class points: "+e.getMessage());
        }
        return "Error";
    }

    protected int getClassPoints(int id)
    {
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+id+" AND equipped=1 AND sES='ar'");
            if (rs.next()) {
                return rs.getInt("classXP");
            }
            rs.close();
        } catch (Exception e) {
            debug("Exception in get class points: "+e.getMessage());
        }
        return -1;
    }

    protected String getEquipment(int id)
    {
        Packet equipPack = new Packet();
        String[] classShort = new String[6];
        classShort[0] = "ar";
        classShort[1] = "ba";
        classShort[2] = "Weapon";
        classShort[3] = "co";
        classShort[4] = "he";
        classShort[5] = "pe";
        try {
            int i = 0;
            while (i < 6) {
                ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+id+" AND equipped=1 AND sES='"+classShort[i]+"'");
                if (rs.next()) {
                    if (i == 0) {
                        equipPack.addString("\""+classShort[i]+"\":{\"ItemID\":\"");
                    } else {
                        equipPack.addString(",\""+classShort[i]+"\":{\"ItemID\":\"");
                    }
                    int itemid = rs.getInt("itemid");
                    if (id == this.accountid && classShort[i].equals("Weapon")) {
                        this.weaponlevel = rs.getInt("iLvl");
                    }
                    equipPack.addInt(itemid);
                    ResultSet es = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemid="+itemid);
                    if (es.next()) {
                        equipPack.addString("\",\"sFile\":\"");
                        equipPack.addString(es.getString("sFile"));
                        equipPack.addString("\",\"sLink\":\"");
                        equipPack.addString(es.getString("sLink"));
                    }
                    equipPack.addString("\"}");
                    es.close();
                }
                rs.close();
                i++;
            }
        } catch (Exception e) {
            debug("Exception in get equipment: "+e.getMessage());
        }
        return equipPack.getPacket();
    }

    protected void getQuests(String quests)
    {
        try {
            Packet sendPack = new Packet();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"getQuests\",\"quests\":{");
            /*String packet_handled[] = quests.split("%");
            for (int i = 1; i < packet_handled.length; i++) {
                ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_quests WHERE id="+packet_handled[i]);
                if (rs.next()) {
                    sendPack.addString("");
                }
            }
            rs.close();*/
            sendPack.addString("}}}}");
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in get quests: "+e.getMessage());
        }
    }

    protected void loadBigInventory(){
        Packet sendPack = new Packet();
        sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"loadInventoryBig\",\"friends\":[");
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_friends WHERE userid="+this.userid);
            if (rs.next()) {
                if (!rs.getString("friendid").equals("")) {
                    String[] friendslist = rs.getString("friendid").split(",");
                    int i = friendslist.length;
                    int e = 0;
                    rs.close();
                    while (e < i) {
                        if (e != 0) {
                            sendPack.addString(",");
                        }
                        ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+friendslist[e]);
                        if (is.next()) {
                            this.friends[e] = is.getString("username");
                            sendPack.addString("{\"iLvl\":\""+is.getInt("level")+"\",\"ID\":\""+is.getInt("id")+"\",\"sName\":\""+is.getString("username")+"\",\"sServer\":\""+is.getString("curServer")+"\"}");
                        }
                        is.close();
                        e++;
                    }
                }
            }
            sendPack.addString("],\"items\":[");

            ResultSet rs2 = Main.sql.doquery("SELECT * FROM wqw_items WHERE sES NOT IN('hi','ho') AND userid="+this.userid+" AND bBank=0");
            int[] charitemid = new int[265];
            int[] itemid = new int[265];
            int[] equip = new int[265];
            int[] level = new int[265];
            int[] classxp = new int[265];
            int[] qty = new int[265];
            int[] enhid = new int[265];
            int i = 0;
            while (rs2.next()) {
                charitemid[i] = rs2.getInt("id");
                itemid[i] = rs2.getInt("itemid");
                equip[i] = rs2.getInt("equipped");
                level[i] = rs2.getInt("iLvl");
                enhid[i] = rs2.getInt("EnhID");
                qty[i] = rs2.getInt("iQty");
                if(rs2.getString("sES").equals("ar")){
                    classxp[i] = rs2.getInt("classXP");
                }
                i++;
            }
            rs2.close();
            int e = 0;
            while (e < i) {
                ResultSet is = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemID="+itemid[e]);
                if (is.next()) {
                    if (e != 0) {
                        sendPack.addString(",");
                    }
                    sendPack.addString("{\"ItemID\":\""+is.getInt("itemID")+"\",\"sLink\":\""+is.getString("sLink")+"\",\"sElmt\":\""+is.getString("sElmt")+"\",\"bStaff\":\""+is.getInt("bStaff")+"\",\"iRng\":\""+is.getInt("iRng")+"\",\"iDPS\":\""+is.getInt("iDPS")+"\",\"bCoins\":\""+is.getInt("bCoins")+"\",\"sES\":\""+is.getString("sES")+"\",\"sType\":\""+is.getString("sType")+"\",\"iCost\":\""+is.getInt("iCost")+"\",\"iRty\":\""+is.getInt("iRty")+"\",");
                    if(is.getString("sES").equals("ar")){
                        sendPack.addString("\"iQty\":\""+classxp[e]+"\",");
                    } else {
                        sendPack.addString("\"iQty\":\""+qty[e]+"\",");
                    }
                    if(is.getString("sES").equals("Weapon")){
                        sendPack.addString("\"EnhDPS\":\"100\",");
                    }
                    if(is.getString("sType").equals("Enhancement") || is.getString("sType").equals("Necklace") || is.getString("sType").equals("Item") || is.getString("sType").equals("Quest Item") || is.getString("sType").equals("Pet") || is.getString("sType").equals("Armor")){
                            sendPack.addString("\"EnhID\":\"0\",\"PatternID\":\""+enhid[e]+"\",");
                    }

                    if(is.getString("sType").equals("Enhancement") || enhid[e]==-1){
                        sendPack.addString("\"iLvl\":\""+is.getInt("iLvl"));
                    } else {
                        sendPack.addString("\"EnhLvl\":\""+level[e]+"\",\"EnhID\":\"1863\",\"EnhRty\":1,\"EnhPatternID\":\""+enhid[e]);
                    }
                    sendPack.addString("\",\"sIcon\":\""+is.getString("sIcon")+"\",\"bTemp\":\""+is.getInt("bTemp")+"\",\"CharItemID\":\""+charitemid[e]+"\",\"iHrs\":\""+is.getInt("iHrs")+"\",\"sFile\":\""+is.getString("sFile")+"\",\"iStk\":\""+is.getInt("iStk")+"\",\"sDesc\":\""+is.getString("sDesc")+"\",\"bBank\":\""+0+"\",\"bUpg\":\""+is.getInt("bUpg")+"\",\"bEquip\":\""+equip[e]+"\",\"sName\":\""+is.getString("sName")+"\"}");
                }
                is.close();
                e++;
            }

            sendPack.addString("],\"factions\":[],\"hitems\":[");
            ResultSet hs = Main.sql.doquery("SELECT * FROM wqw_items WHERE sES IN('hi','ho') AND userid="+this.userid+" AND bBank=0");
            int x=0;
            int[] hequip = new int[30];
            int[] hcharitemid = new int[30];
            int[] hitemid = new int[30];
            while (hs.next()) {
                hequip[x] = hs.getInt("equipped");
                hcharitemid[x] = hs.getInt("id");
                hitemid[x] = hs.getInt("itemid");
                x++;
            }
            int z = 0;
            while (z < x) {
                ResultSet is = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemid="+hitemid[z]);
                if (is.next()) {
                    if (z != 0) {
                        sendPack.addString(",");
                    }
                    sendPack.addString("{\"ItemID\":\""+is.getInt("itemID")+"\",\"sLink\":\""+is.getString("sLink")+"\",\"sElmt\":\""+is.getString("sElmt")+"\",\"bStaff\":\""+is.getInt("bStaff")+"\",\"iRng\":\""+is.getInt("iRng")+"\",\"iDPS\":\""+is.getInt("iDPS")+"\",\"bCoins\":\""+is.getInt("bCoins")+"\",\"sES\":\""+is.getString("sES")+"\",\"sType\":\""+is.getString("sType")+"\",\"iCost\":\""+is.getInt("iCost")+"\",\"iRty\":\""+is.getInt("iRty")+"\",");
                    sendPack.addString("\"iQty\":\"1\",");
                    sendPack.addString("\"iLvl\":\""+is.getInt("iLvl"));
                    sendPack.addString("\",\"sIcon\":\""+is.getString("sIcon")+"\",\"bTemp\":\""+is.getInt("bTemp")+"\",\"CharItemID\":\""+hcharitemid[z]+"\",\"iHrs\":\""+is.getInt("iHrs")+"\",\"sFile\":\""+is.getString("sFile")+"\",\"iStk\":\""+is.getInt("iStk")+"\",\"sDesc\":\""+is.getString("sDesc")+"\",\"bBank\":\""+0+"\",\"bUpg\":\""+is.getInt("bUpg")+"\",\"bEquip\":\""+hequip[z]+"\",\"sName\":\""+is.getString("sName")+"\"}");
                }
                is.close();
                z++;
            }
            sendPack.addString("]}}}");
            send(sendPack, true);
            sendPack.clean();
            sendPack.addString("%xt%server%-1%Character load complete.%");
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in load big inventory: "+e.getMessage());
        }
    }


    protected String getMessage()
    {
        try {
            ResultSet rs = Main.sql.doquery("SELECT message FROM wqw_settings LIMIT 1");
            if (rs.next()) {
                return rs.getString("message");
            }
            rs.close();
        } catch (Exception e) {
            //debug("Exception in get news: "+e.getMessage());
        }
        return "";
    }

    protected String getNews()
    {
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_settings LIMIT 1");
            if (rs.next()) {
                return "sNews="+rs.getString("newsFile")+",sMap="+rs.getString("mapFile")+",sBook="+rs.getString("bookFile");
            }
            rs.close();
        } catch (Exception e) {
            //debug("Exception in get news: "+e.getMessage());
        }
        return "";
    }

    protected void joinRoom(String newroom, int roomnumb, String frame, String pad)
    {
        Packet sendPack = new Packet();
        int oldroom[] = gameServer.getPlayerRoom(this.account);
        if (gameServer.addToRoom(newroom, roomnumb, this.accountid)) {
            this.playerRoom = gameServer.room[gameServer.getPlayerRoom(this.account)[0]][gameServer.getPlayerRoom(this.account)[1]];
            this.playerSlot = this.playerRoom.getPlayerSlot(this.account);
            this.playerRoom.frame[this.playerSlot] = frame;
            this.playerRoom.pad[this.playerSlot] = pad;
            this.playerRoom.tx[this.playerSlot] = 0;
            this.playerRoom.ty[this.playerSlot] = 0;
            sendPack.addXMLSingle(1,"msg t","sys");
            sendPack.addXMLSingle(1,"body action","userGone","r",""+oldroom[0]);
            sendPack.addXMLSingle(0,"user id",""+this.accountid);
            sendPack.addXMLSingle(2,"body");
            sendPack.addXMLSingle(2,"msg");
            gameServer.writeOtherMapPacket(oldroom, sendPack, true);
            sendPack.clean();
            sendPack.addString("%xt%exitArea%-1%");
            sendPack.addInt(this.accountid);
            sendPack.addString("%"+this.account+"%");
            gameServer.writeOtherMapPacket(oldroom, sendPack, true);
            sendLobby();
        } else {
            sendPack.addString("%xt%warning%-1%\"");
            sendPack.addString(newroom);
            sendPack.addString("\" is not a recognized Map name%");
            send(sendPack, true);
        }
    }

    protected void loadBank()
    {
        try {
            Packet sendPack = new Packet();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"loadBank\",\"items\":[");
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+this.userid+" AND bBank=1");
            int[] charitemid = new int[40];
            int[] itemid = new int[40];
            int[] equip = new int[40];
            int[] level = new int[40];
            int i = 0;
            while (rs.next()) {
                charitemid[i] = rs.getInt("id");
                itemid[i] = rs.getInt("itemid");
                equip[i] = rs.getInt("equipped");
                level[i] = rs.getInt("iLvl");
                i++;
            }
            rs.close();
            int e = 0;
            while (e < i) {
                if (e != 0) {
                    sendPack.addString(",");
                }
                ResultSet is = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemID="+itemid[e]);
                if (is.next()) {
                    sendPack.addString("{\"sIcon\":\""+is.getString("sIcon")+"\",\"ItemID\":\""+is.getInt("itemID")+"\",\"iLvl\":\""+level[e]+"\",\"iEnh\":\""+is.getInt("iEnh")+"\",\"sElmt\":\""+is.getString("sElmt")+"\",\"bTemp\":\""+is.getInt("bTemp")+"\",\"sLink\":\""+is.getString("sLink")+"\",\"bStaff\":\""+is.getInt("bStaff")+"\",\"CharItemID\":\""+charitemid[e]+"\",\"iRng\":\""+is.getInt("iRng")+"\",\"bCoins\":\""+is.getInt("bCoins")+"\",\"iDPS\":\""+is.getInt("iDPS")+"\",\"sES\":\""+is.getString("sES")+"\",\"iHrs\":\""+is.getInt("iHRS")+"\",\"sFile\":\""+is.getString("sFile")+"\",\"sType\":\""+is.getString("sType")+"\",\"sDesc\":\""+is.getString("sDesc")+"\",\"iStk\":\""+is.getInt("iStk")+"\",\"iCost\":\""+is.getInt("iCost")+"\",\"bEquip\":\""+equip[e]+"\",\"bUpg\":\""+is.getInt("bUpg")+"\",\"iRty\":\""+is.getInt("iRty")+"\",\"sName\":\""+is.getString("sName")+"\",\"iQty\":\""+is.getInt("iQty")+"\"}");
                }
                is.close();
                e++;
            }
            sendPack.addString("]}}}");
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in load bank: "+e.getMessage());
        }
    }

    protected void loadFriends()
    {
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_friends WHERE userid="+this.userid);
            if (rs.next()) {
                if (!rs.getString("friendid").equals("")) {
                    Packet sendPack = new Packet();
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"loadFriendsList\",\"friends\":[");
                    String[] friends = rs.getString("friendid").split(",");
                    int i = friends.length;
                    int e = 0;
                    rs.close();
                    while (e < i) {
                        if (e != 0) {
                            sendPack.addString(",");
                        }
                        ResultSet is = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+friends[e]);
                        if (is.next()) {
                            sendPack.addString("{\"iLvl\":\""+is.getInt("level")+"\",\"ID\":\""+is.getInt("id")+"\",\"sName\":\""+is.getString("username")+"\",\"sServer\":\""+is.getString("curServer")+"\"}");
                        }
                        is.close();
                        e++;
                    }
                    sendPack.addString("]}}}");
                    send(sendPack, true);
                }
            }
        } catch (Exception e) {
            debug("Exception in load friends: "+e.getMessage());
        }
    }

    protected void loadHouseInventory()
    {
        Packet sendPack = new Packet();
        sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"loadHouseInventory\",\"sHouseInfo\":[],\"items\":[]}}}");
        send(sendPack, true);
    }

    protected void loadShop(int shopid)
    {
        try {
            Packet sendPack = new Packet();
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_shops WHERE shopid="+shopid);
            if (rs.next()) {
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"shopinfo\":{\"strName\":\"");
                sendPack.addString(rs.getString("strName"));
                sendPack.addString("\",\"bitSuccess\":\"1\",\"items\":[");
                String[] items = rs.getString("items").split(",");
                int house = rs.getInt("bhouse");
                int staff = rs.getInt("bStaff");
                String field = rs.getString("sField");
                int i = items.length;
                int e = 0;
                rs.close();
                while (e < i) {
                    if (e != 0) {
                        sendPack.addString(",");
                    }
                    ResultSet is = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemID="+items[e]);
                    if (is.next()) {
                        sendPack.addString("{\"sFaction\":\""+is.getString("sFaction")+"\",\"ItemID\":\""+is.getInt("itemID")+"\",\"iClass\":\""+is.getInt("iClass")+"\",\"sElmt\":\""+is.getString("sElmt")+"\",\"sLink\":\""+is.getString("sLink")+"\",\"bStaff\":\""+is.getInt("bStaff")+"\",\"iRng\":\""+is.getInt("iRng")+"\",\"iDPS\":\""+is.getInt("iDPS")+"\",\"bCoins\":\""+is.getInt("bCoins")+"\",\"sES\":\""+is.getString("sES")+"\",\"sType\":\""+is.getString("sType")+"\",\"iCost\":\""+is.getInt("iCost")+"\",\"iRty\":\""+is.getInt("iRty")+"\",\"iQty\":\""+is.getInt("iQty")+"\",\"sIcon\":\""+is.getString("sIcon")+"\",\"iLvl\":\""+is.getInt("iLvl")+"\",\"FactionID\":\""+is.getInt("FactionID")+"\",\"iEnh\":\""+is.getInt("iEnh")+"\",\"bTemp\":\""+is.getInt("bTemp")+"\",\"iReqRep\":\""+is.getInt("iReqRep")+"\",\"ShopItemID\":\""+(shopid+e)+"\",\"sFile\":\""+is.getString("sFile")+"\",\"iStk\":\""+is.getInt("iStk")+"\",\"sDesc\":\""+is.getString("sDesc")+"\",\"bUpg\":\""+is.getInt("bUpg")+"\",\"bHouse\":\""+house+"\",\"iReqCP\":\""+is.getInt("iReqCP")+"\",\"sName\":\""+is.getString("sName")+"\"}");
                    }
                    is.close();
                    e++;
                }
                sendPack.addString("],\"ShopID\":\"");
                sendPack.addInt(shopid);
                sendPack.addString("\",\"sField\":\""+field+"\",\"bStaff\":\""+staff+"\",\"bHouse\":\""+house);
                sendPack.addString("\",\"iIndex\":\"-1\"},\"cmd\":\"loadShop\"}}}");
                send(sendPack, true);
            }
        } catch (Exception e) {
            debug("Exception in load shop: "+e.getMessage());
        }
    }

    protected void loadSkills()
    {
        Packet sendPack = new Packet();
        sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"sAct\",\"actions\":{\"passive\":[],\"active\":[{\"icon\":\"iwd1\",\"nam\":\"Auto attack\",\"anim\":\"Attack1,Attack2\",\"desc\":\"A basic attack, taught to all adventurers.\",\"range\":201,\"fx\":\"m\",\"damage\":0.75,\"mana\":0,\"dsrc\":\"wDMG\",\"ref\":\"aa\",\"auto\":true,\"tgt\":\"h\",\"typ\":\"aa\",\"strl\":\"\",\"cd\":1500}]}}}}");
        send(sendPack, true);
    }

    protected void moveToCell(String frame, String pad)
    {
        Packet sendPack = new Packet();
        this.playerRoom.pad[this.playerSlot] = pad;
        this.playerRoom.frame[this.playerSlot] = frame;
        this.playerRoom.tx[this.playerSlot] = 0;
        this.playerRoom.ty[this.playerSlot] = 0;
        sendPack.addString("%xt%uotls%-1%");
        sendPack.addString(this.account);
        sendPack.addString("%strPad:");
        sendPack.addString(pad);
        sendPack.addString(",tx:0,strFrame:");
        sendPack.addString(frame);
        sendPack.addString(",ty:0%");
        gameServer.writeMapPacket(this.account, sendPack, true, true);
    }

    protected void moveToUser(int playerid)
    {
        Room otherroom = gameServer.room[gameServer.getPlayerRoom(gameServer.charName[playerid])[0]][gameServer.getPlayerRoom(gameServer.charName[playerid])[1]];
        int otherslot = otherroom.getPlayerSlot(gameServer.charName[playerid]);
        int newx = otherroom.tx[otherslot];
        int newy = otherroom.ty[otherslot];
        if (otherroom.tx[otherslot] > this.playerRoom.tx[this.playerSlot]) {
            newx -= 96;
        } else {
            newx += 96;
        }
        userMove(newx, newy, 16, true);
    }

    protected void partyAccept(int partyid)
    {
        Packet sendPack = new Packet();
        if (gameServer.partyRoom[this.accountid] == 0) {
            this.partyID = gameServer.addToParty(this.account, partyid);
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pa\",\"ul\":");
            sendPack.addString(gameServer.party[this.partyID].getPlayers());
            sendPack.addString(",\"owner\":\""+gameServer.party[this.partyID].partyOwner+"\",\"pid\":"+this.partyID+"}}}");
            send(sendPack, true);
            sendPack.clean();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pa\",\"ul\":[\"");
            sendPack.addString(this.account);
            sendPack.addString("\"],\"owner\":\""+gameServer.party[this.partyID].partyOwner+"\",\"pid\":"+this.partyID+"}}}");
            gameServer.writePartyPacket(this.account, sendPack, true, true);
        } else {
            sendPack.addString("%xt%warning%-1%You are already in a party.%");
            send(sendPack, true);
        }
        sendPack.clean();
    }

    protected void partyDecline(int partyid)
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%server%-1%You have declined the invitation.%");
        send(sendPack, true);
        sendPack.clean();
        sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pd\",\"unm\":\"");
        sendPack.addString(this.account);
        sendPack.addString("\"}}}");
        gameServer.writePlayerPacket(gameServer.party[partyid].partyOwner, sendPack, true);
        sendPack.clean();
    }

    protected void partyKick(String otherchar)
    {
        Packet sendPack = new Packet();
        if (gameServer.partyRoom[gameServer.getPlayerID(otherchar)] == gameServer.partyRoom[this.accountid]) {
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pr\",\"owner\":\""+gameServer.party[this.partyID].partyOwner+"\",\"pid\":"+this.partyID+",\"typ\":\"k\",\"unm\":\""+otherchar+"\"}}}");
            gameServer.writePartyPacket(this.account, sendPack, true, false);
            gameServer.leaveParty(otherchar);
        } else {
            sendPack.addString("%xt%warning%-1%That player is not in your party.%");
            send(sendPack, true);
        }
    }

    protected void partyInvite(String otherchar)
    {
        Packet sendPack = new Packet();
        if (gameServer.partyRoom[this.accountid] == 0) {
            this.partyID = gameServer.addToParty(this.account, 0);
        }
        sendPack.addString("%xt%server%-1%You have invited "+otherchar+" to join your party.%");
        send(sendPack, true);
        sendPack.clean();
        sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pi\",\"owner\":\"");
        sendPack.addString(gameServer.party[this.partyID].partyOwner);
        sendPack.addString("\",\"pid\":"+this.partyID+"}}}");
        gameServer.writePlayerPacket(otherchar, sendPack, true);
    }

    protected void partyLeave()
    {
        Packet sendPack = new Packet();
        if (gameServer.partyRoom[this.accountid] != 0) {
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pr\",\"owner\":\"");
            sendPack.addString(gameServer.party[this.partyID].partyOwner);
            sendPack.addString("\",\"pid\":"+this.partyID+",\"typ\":\"l\",\"unm\":\""+this.account+"\"}}}");
            gameServer.writePartyPacket(this.account, sendPack, true, false);
            gameServer.leaveParty(this.account);
            this.partyID = 0;
        } else {
            sendPack.addString("%xt%warning%-1%You are not in a party.%");
            send(sendPack, true);
        }
    }

    protected void partyPromote(String otherchar)
    {
        Packet sendPack = new Packet();
        if (gameServer.partyRoom[gameServer.getPlayerID(otherchar)] == gameServer.partyRoom[this.accountid]) {
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pp\",\"owner\":\""+otherchar+"\"}}}");
            gameServer.writePartyPacket(this.account, sendPack, true, false);
            gameServer.party[gameServer.partyRoom[this.accountid]].partyOwner = otherchar;
        } else {
            sendPack.addString("%xt%warning%-1%That player is not in your party.%");
            send(sendPack, true);
        }
    }
    protected void partySummon(String otherchar)
    {
        Packet sendPack = new Packet();
        if (gameServer.partyRoom[gameServer.getPlayerID(otherchar)] == gameServer.partyRoom[this.accountid]) {
            sendPack.addString("%xt%server%-1%You attempt to summon "+otherchar+" to you.%");
            send(sendPack, true);
            sendPack.clean();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"ps\",\"strF\":\""+this.playerRoom.frame[this.playerSlot]+"\",\"unm\":\""+this.account+"\",\"strP\":\""+this.playerRoom.pad[this.playerSlot]+"\"}}}");
            gameServer.writePlayerPacket(otherchar, sendPack, true);
        } else {
            sendPack.addString("%xt%warning%-1%That player is not in your party.%");
            send(sendPack, true);
        }
    }

    protected void partySummonAccept()
    {
        /*Packet sendPack = new Packet();
        sendPack.addString("%xt%server%-1%You declined the summon.%");
        send(sendPack, true);
        sendPack.clean();
        sendPack.addString("%xt%server%-1%"+this.account+" declined your summon.%");
        gameServer.writePlayerPacket(otherchar, sendPack, true);*/
    }

    protected void partySummonDecline(String otherchar)
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%server%-1%You declined the summon.%");
        send(sendPack, true);
        sendPack.clean();
        sendPack.addString("%xt%server%-1%"+this.account+" declined your summon.%");
        gameServer.writePlayerPacket(otherchar, sendPack, true);
    }

    protected void playerAttack(String skill, int turn, String type, int monsterid, int weaponlevel2)
    {
        this.fighting = true;
        pvpTime.main(this);
        Packet sendPack = new Packet();
        int monsterid2 = monsterid - 1;
        int damage = (int)(50+(weaponlevel2*weaponlevel2/8)+((weaponlevel2*1.25)));
        int damage2 = (int) (60+(weaponlevel2*weaponlevel2/4)+(weaponlevel2*1.25));
        int damage3 = damage + this.generator.nextInt(damage2-damage);
        int crit = this.generator.nextInt(10);
        String hit = "hit";
        if (crit > 8) {
            hit = "crit";
            damage3 = damage3*2;
        }
        boolean canCon = true;
        if (gameServer.hp[this.accountid] >= 1) {
            if (type.equals("p")) {
                Room otherroom = gameServer.room[gameServer.getPlayerRoom(gameServer.charName[monsterid])[0]][gameServer.getPlayerRoom(gameServer.charName[monsterid])[1]];
                int otherslot = otherroom.getPlayerSlot(gameServer.charName[monsterid]);
                if (!this.playerRoom.frame[this.playerSlot].equals(otherroom.frame[otherslot]) && gameServer.isAlive[monsterid] == true) {
                    sendPack.clean();
                    sendPack.addString("%xt%server%-1%You are not in the same location as "+gameServer.charName[monsterid]+".%");
                    send(sendPack, true);
                    canCon = false;
                    this.monfighting = 0;
                } else if (gameServer.isAlive[monsterid] == true) {
                    gameServer.hp[monsterid] -= damage3;
                    if (gameServer.hp[monsterid] <= 0) {
                        gameServer.hp[monsterid] = 0;
                        gameServer.mp[monsterid] = 0;
                        sendPack.clean();
                        sendPack.addString("%xt%uotls%-1%"+this.account+"%intState:1%");
                        gameServer.writeMapPacket(this.account, sendPack, true, false);
                        sendPack.clean();
                        sendPack.addString(addMonsterRewards(0, type, monsterid));
                        send(sendPack, true);
                        sendPack.clean();
                        sendPack.addString("%xt%uotls%-1%"+gameServer.charName[monsterid]+"%intState:0,intHP:0,intMP:0%");
                        gameServer.writeMapPacket(this.account, sendPack, true, false);
                        this.monfighting = 0;
                        Main.sql.doupdate("UPDATE wqw_users SET pvpkill=pvpkill+1 WHERE username='"+this.account+"'");
                        gameServer.isAlive[monsterid] = false;
                        this.monkilled++;
                        if (this.monkilled > 10) {
                            gameServer.banPlayer(this.account,"the server for hacking");
                        }
                    } else {
                        if (this.monfighting == 0) {
                            this.monfighting = monsterid;
                            moveToUser(monsterid);
                        }
                        sendPack.addString("%xt%uotls%-1%"+gameServer.charName[monsterid]+"%intHP:"+gameServer.hp[monsterid]+"%");
                        gameServer.writeMapPacket(this.account, sendPack, true, false);
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            debug("Exception in sleep: "+e.getMessage());
                        }
                    }
                }
            } else {
                this.playerRoom.monsterHP[monsterid2] -= damage3;
                if (this.playerRoom.monsterHP[monsterid2] <= 0 && this.playerRoom.monsterState[monsterid2] == 1) {
                    this.playerRoom.monsterHP[monsterid2] = 0;
                    this.playerRoom.monsterMP[monsterid2] = 0;
                    this.playerRoom.monsterState[monsterid2] = 0;
                    sendPack.clean();
                    sendPack.addString("%xt%uotls%-1%"+this.account+"%intState:1%");
                    gameServer.writeMapPacket(this.account, sendPack, true, false);
                    sendPack.clean();
                    sendPack.addString(addMonsterRewards(this.playerRoom.monsterType[monsterid2], type, monsterid));
                    send(sendPack, true);
                    sendPack.clean();
                    sendPack.addString("%xt%mtls%-1%"+monsterid+"%intState:0,intHP:0,intMP:0%");
                    gameServer.writeMapPacket(this.account, sendPack, true, false);
                    this.monfighting = 0;
                    this.playerRoom.respawnMonster(monsterid2, this.playerRoom.monsterType[monsterid2]);
                    Main.sql.doupdate("UPDATE wqw_users SET monkill=monkill+1 WHERE username='"+this.account+"'");
                    this.monkilled++;
                    if (this.monkilled > 10) {
                        gameServer.banPlayer(this.account,"the server for hacking");
                    }
                } else if (this.playerRoom.monsterState[monsterid2] == 1) {
                    sendPack.addString("%xt%mtls%-1%"+monsterid+"%intHP:"+this.playerRoom.monsterHP[monsterid2]+"%");
                    gameServer.writeMapPacket(this.account, sendPack, true, false);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        debug("Exception in sleep: "+e.getMessage());
                    }
                    playerHit(monsterid);
                }
            }
            if (canCon) {
                if (turn == 0) {
                    sendPack.clean();
                    sendPack.addString("%xt%uotls%-1%"+this.account+"%intState:2%");
                    gameServer.writeMapPacket(this.account, sendPack, true, false);
                    this.monfighting = monsterid;
                    if (type.equals("p")) {
                        sendPack.clean();
                        sendPack.addString("%xt%uotls%-1%"+gameServer.charName[monsterid]+"%intState:2%");
                        gameServer.writeMapPacket(this.account, sendPack, true, false);
                    }
                }
                sendPack.clean();
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"showActionResult\",\"intResult\":1,\"actionResult\":{\"actRef\":\""+skill+"\",\"cInf\":\"p:"+this.accountid+"\",\"damage\":"+damage3+",\"tInf\":\""+type+":"+monsterid+"\",\"type\":\""+hit+"\"},\"actID\":"+turn+"}}}");
                if (type.equals("p")) {
                    send(sendPack, true);
                    gameServer.writePlayerPacket(gameServer.charName[monsterid], sendPack, true);
                } else {
                    send(sendPack, true);
                }
                sendPack.clean();
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cInf\":\"p:"+this.accountid+"\",\"anim\":\"Attack1,Attack2\",\"cmd\":\"anim\",\"fx\":\""+type+"\",\"tInf\":\""+type+":"+monsterid+"\",\"strl\":\"\"}}}");
                gameServer.writeMapPacket(this.account, sendPack, true, false);
                if (type.equals("p")) {
                    try {
                        Thread.sleep(750);
                    } catch (Exception e) {
                        debug("Exception in sleep: "+e.getMessage());
                    }
                    server.setPvP(gameServer.playerSocket[monsterid].getRemoteSocketAddress(), turn, this.accountid, this.weaponlevel);
                }
            }
        } else if (!type.equals("p")) {
            playerHit(monsterid);
        }
    }

    protected void playerHit(int monsterid)
    {
        this.fighting = true;
        pvpTime.main(this);
        Packet sendPack = new Packet();
        int monsterid2 = monsterid - 1;
        int damage = (int)(25+(this.playerRoom.monsterLevel[monsterid2]*this.playerRoom.monsterLevel[monsterid2]/8)+((this.playerRoom.monsterLevel[monsterid2]*1.25)));
        int damage2 = (int) (30+(this.playerRoom.monsterLevel[monsterid2]*this.playerRoom.monsterLevel[monsterid2]/4)+(this.playerRoom.monsterLevel[monsterid2]*1.25));
        int damage3 = damage + this.generator.nextInt(damage2-damage);
        int crit = this.generator.nextInt(20);
        String hit = "hit";
        if (crit > 18) {
            hit = "crit";
            damage3 = damage3*2;
        }
        gameServer.hp[this.accountid] -= damage3;
        if (gameServer.hp[this.accountid] <= 0) {
            gameServer.hp[this.accountid] = 0;
            sendPack.addString("%xt%uotls%-1%"+this.account+"%intState:0,intHP:0,intMP:0%");
            gameServer.writeMapPacket(this.account, sendPack, true, false);
            sendPack.clean();
        } else {
            sendPack.addString("%xt%uotls%-1%"+this.account+"%intHP:"+gameServer.hp[this.accountid]+"%");
            gameServer.writeMapPacket(this.account, sendPack, true, false);
            sendPack.clean();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"showActionResult\",\"intResult\":1,\"actionResult\":{\"cInf\":\"m:"+monsterid+"\",\"damage\":"+damage3+",\"tInf\":\"p:"+this.accountid+"\",\"type\":\""+hit+"\"}}}}");
            send(sendPack, true);
            sendPack.clean();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cInf\":\"m:"+monsterid+"\",\"anim\":\"Attack1,Attack2\",\"cmd\":\"anim\",\"fx\":\"m\",\"tInf\":\"p:"+this.accountid+"\",\"strl\":\"\"}}}");
            gameServer.writeMapPacket(this.account, sendPack, true, false);
            sendPack.clean();
        }
    }

    protected void playerTimerAttack()
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%hi%-1%");
        send(sendPack, true);
        sendPack.clean();
        //playerHit(this.monfighting);
    }

    protected void respawnPlayer()
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%resTimed%-1%");
        send(sendPack, true);
        this.monfighting = 0;
        gameServer.hp[this.accountid] = gameServer.hpmax[this.accountid];
        gameServer.mp[this.accountid] = gameServer.mpmax[this.accountid];
        gameServer.isAlive[this.accountid] = true;
        sendPack.clean();
        sendPack.addString("%xt%uotls%-1%"+this.account+"%intState:1,intHP:"+gameServer.hp[this.accountid]+",intMP:"+gameServer.mp[this.accountid]+"%");
        gameServer.writeMapPacket(this.account, sendPack, true, false);
    }

    protected void restPlayer()
    {
        Packet sendPack = new Packet();
        gameServer.hp[this.accountid] += gameServer.hpmax[this.accountid]/20;
        if (gameServer.hp[this.accountid] > gameServer.hpmax[this.accountid]) {
            gameServer.hp[this.accountid] = gameServer.hpmax[this.accountid];
        }
        gameServer.mp[this.accountid] += gameServer.mpmax[this.accountid]/20;
        if (gameServer.mp[this.accountid] > gameServer.mpmax[this.accountid]) {
            gameServer.mp[this.accountid] = gameServer.mpmax[this.accountid];
        }
        sendPack.addString("%xt%uotls%-1%"+this.account+"%intHP:"+gameServer.hp[this.accountid]+",intMP:"+gameServer.mp[this.accountid]+"%");
        gameServer.writeMapPacket(this.account, sendPack, true, false);
        sendPack.clean();
    }

    protected void requestFriend(String otherchar)
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%server%-1%You have requested "+otherchar+" to be friends.%");
        send(sendPack, true);
        sendPack.clean();
        sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"requestFriend\",\"unm\":\""+this.account+"\"}}}");
        gameServer.writePlayerPacket(otherchar, sendPack, true);
    }

    boolean isInteger(String input){
        try{
            Integer.parseInt(input);
            return true;
        }
        catch(NumberFormatException nfe){
            return false;
        }
    }
    protected void retrieveUserDatas(String Packet)
    {
        try
        {
            String packet_handled[] = Packet.split("%");
            Packet sendPack = new Packet();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"initUserDatas\",\"a\":[");
            for (int i = 2; i < packet_handled.length; i++) {
                if(isInteger(packet_handled[i])){
                    if(i != 2){
                        sendPack.addString(",");
                    }

                    sendPack.addString("{");
                    int pID = Integer.parseInt(packet_handled[i]);
                    int id = gameServer.userID[pID];

                    debug("Retrieving user data for uID: "+id+", pID: "+pID+", Character: "+gameServer.charName[pID]);

                    int classPoints = gameServer.getClassPoints(id);
                    String className = getClassName(id);
                    String equipment = getEquipment(id);
                    Room room = this.playerRoom;
                    ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+id+" LIMIT 1");

                    if(rs.next()){
                        String userName = rs.getString("username").toLowerCase();
                        int playerslot = room.getPlayerSlot(userName);
                        int playerLevel = rs.getInt("level");
                        if(userName.equals(this.account)){
                            this.playerlevel = playerLevel;
                        }
                        sendPack.addString("\"uid\":");
                        sendPack.addInt(gameServer.getPlayerID(userName));
                        sendPack.addString(",\"strFrame\":");
                        sendPack.addString("\"" + room.frame[playerslot] + "\"");
                        sendPack.addString(",\"strPad\":");
                        sendPack.addString("\"" + room.pad[playerslot] + "\"");
                        sendPack.addString(",\"data\":{\"intColorAccessory\":\"");
                        sendPack.addInt(rs.getInt("cosColorAccessory"));
                        sendPack.addString("\",\"iCP\":");
                        sendPack.addInt(classPoints);
                        sendPack.addString(",\"intLevel\":\"");
                        sendPack.addInt(playerLevel);
                        sendPack.addString("\",\"iBagSlots\":");
                        sendPack.addInt(rs.getInt("slotBag"));
                        sendPack.addString(",\"ig0\":0,\"iUpgDays\":\"-");
                        sendPack.addInt(rs.getInt("upgDays"));
                        sendPack.addString("\",\"intColorBase\":\"");
                        sendPack.addInt(rs.getInt("cosColorBase"));
                        sendPack.addString("\",\"sCountry\":\"US\"");
                        sendPack.addString(",\"iSTR\":\"");
                        sendPack.addInt(rs.getInt("str"));
                        sendPack.addString("\",\"ip0\":0,\"iq0\":0,\"iAge\":\"");
                        sendPack.addInt(rs.getInt("age"));
                        sendPack.addString("\",\"iWIS\":\"");
                        sendPack.addInt(rs.getInt("WIS"));
                        sendPack.addString("\",\"intExpToLevel\":\"");
                        sendPack.addInt(getXpToLevel(playerLevel)); //Calculate this
                        sendPack.addString("\",\"intGold\":");
                        sendPack.addInt(rs.getInt("gold"));
                        sendPack.addString(",\"intMP\":");
                        sendPack.addInt(gameServer.calculateMP(playerLevel)); //Calculate this
                        sendPack.addString(",\"sHouseInfo\":[]");
                        sendPack.addString(",\"iBankSlots\":");
                        sendPack.addInt(rs.getInt("slotBank"));
                        sendPack.addString(",\"iHouseSlots\":");
                        sendPack.addInt(rs.getInt("slotHouse"));
                        sendPack.addString(",\"id0\":0,\"intColorSkin\":\"");
                        sendPack.addInt(rs.getInt("plaColorSkin"));
                        sendPack.addString("\",\"intMPMax\":");
                        sendPack.addInt(gameServer.calculateMP(playerLevel)); //Calculate this
                        sendPack.addString(",\"intHPMax\":");
                        sendPack.addInt(gameServer.calculateHP(playerLevel)); //Calculate this
                        sendPack.addString(",\"dUpgExp\":\"");
                        sendPack.addString("2012-01-20T17:53:00"/*+rs.getString("upgDate")*/);
                        sendPack.addString("\",\"iUpg\":\"");
                        sendPack.addInt(rs.getInt("upgrade"));
                        sendPack.addString("\",\"CharID\":\"");
                        sendPack.addInt(id);
                        sendPack.addString("\",\"strEmail\":\"none\"");
                        sendPack.addString(",\"iINT\":\"");
                        sendPack.addInt(rs.getInt("INT"));
                        sendPack.addString("\",\"intColorTrim\":\"");
                        sendPack.addInt(rs.getInt("cosColorTrim"));
                        sendPack.addString("\",\"lastArea\":\"");
                        sendPack.addString(rs.getString("lastVisited"));
                        sendPack.addString("\",\"iFounder\":\"1\"");
                        sendPack.addString(",\"intDBExp\":");
                        sendPack.addInt(rs.getInt("xp"));
                        sendPack.addString(",\"intExp\":");
                        sendPack.addInt(rs.getInt("xp"));
                        sendPack.addString(",\"UserID\":\"");
                        sendPack.addInt(id);
                        sendPack.addString("\",\"ia1\":\"0\",\"ia0\":0,\"intHP\":");
                        sendPack.addInt(gameServer.calculateHP(playerLevel)); //Calculate this
                        sendPack.addString(",\"dCreated\":\"0000-00-00T00:00:00\"");
                        sendPack.addString(",\"strQuests\":\"ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ\",\"bitSuccess\":\"1\",\"strHairName\":\"");
                        sendPack.addString(rs.getString("hairName"));
                        sendPack.addString("\",\"intColorEye\":\"");
                        sendPack.addInt(rs.getInt("plaColorEyes"));
                        sendPack.addString("\",\"iLCK\":\"");
                        sendPack.addInt(rs.getInt("LCK"));
                        sendPack.addString("\",\"eqp\":{");
                        sendPack.addString(equipment);
                        sendPack.addString("},\"iDBCP\":");
                        sendPack.addInt(classPoints);
                        sendPack.addString(",\"intDBGold\":");
                        sendPack.addInt(rs.getInt("gold"));
                        sendPack.addString(",\"strClassName\":\"");
                        sendPack.addString(className);
                        sendPack.addString("\",\"intActivationFlag\":\"");
                        sendPack.addInt(rs.getInt("emailActive"));
                        sendPack.addString("\",\"intAccessLevel\":\"");
                        sendPack.addInt(rs.getInt("access"));
                        sendPack.addString("\",\"strHairFilename\":\"");
                        sendPack.addString(rs.getString("hairFile"));
                        sendPack.addString("\",\"intColorHair\":\"");
                        sendPack.addInt(rs.getInt("plaColorHair"));
                        sendPack.addString("\",\"HairID\":\"");
                        sendPack.addInt(rs.getInt("hairID"));
                        sendPack.addString("\",\"strGender\":\"");
                        sendPack.addString(rs.getString("gender"));
                        sendPack.addString("\",\"strUsername\":\"");
                        sendPack.addString(userName);
                        sendPack.addString("\",\"iDEX\":\"");
                        sendPack.addInt(rs.getInt("DEX"));
                        sendPack.addString("\",\"intCoins\":");
                        sendPack.addInt(rs.getInt("coins"));
                        sendPack.addString(",\"iEND\":\"");
                        sendPack.addInt(rs.getInt("END"));
                        sendPack.addString("\",\"strMapName\":\"");
                        sendPack.addString(room.roomName + "\"");
                    }
                    sendPack.addString("}}");
                    rs.close();
                }
            }
            sendPack.addString("]}}}");
            send(sendPack, true);
            sendPack.clean();
        }
        catch(Exception e)
        {
            debug("Exception in retrieve user datas: "+e.getMessage()+", uid: "+Packet);
        }
    }

    protected void retrieveUserData(int id2, boolean doAgain)
    {
        int uid = id2;
        try {
            int id = gameServer.userID[id2];
            if (id > 0) {
                debug("Attempting to retrieve user data: "+id+", "+gameServer.charName[id2]);
                int cp = getClassPoints(id);
                String cn = getClassName(id);
                String equip = getEquipment(id);
                Packet sendPack = new Packet();
                Room room = this.playerRoom; //gameServer.room[gameServer.getPlayerRoom(user)[0]][gameServer.getPlayerRoom(user)[1]];
                ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_users WHERE id="+id+" LIMIT 1");
                if (rs.next()) {
                    String user = rs.getString("username");
                    user = user.toLowerCase();
                    int slot = room.getPlayerSlot(user);
                    int level = rs.getInt("level");
                    if (id == this.userid) {
                        this.playerlevel = level;
                    }
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"uid\":");
                    sendPack.addInt(gameServer.getPlayerID(user));
                    sendPack.addString(",\"strFrame\":\"");
                    sendPack.addString(room.frame[slot]);
                    sendPack.addString("\",\"cmd\":\"initUserData\",\"strPad\":\"");
                    sendPack.addString(room.pad[slot]);
                    sendPack.addString("\",\"data\":{\"intColorAccessory\":\"");
                    sendPack.addInt(rs.getInt("cosColorAccessory"));
                    sendPack.addString("\",\"iCP\":");
                    sendPack.addInt(cp);
                    sendPack.addString(",\"intLevel\":\"");
                    sendPack.addInt(level);
                    sendPack.addString("\",\"iBagSlots\":");
                    sendPack.addInt(rs.getInt("slotBag"));
                    sendPack.addString(",\"ig0\":0,\"iUpgDays\":\"-");
                    sendPack.addInt(rs.getInt("upgDays"));
                    sendPack.addString("\",\"intColorBase\":\"");
                    sendPack.addInt(rs.getInt("cosColorBase"));
                    sendPack.addString("\",\"iSTR\":\"");
                    sendPack.addInt(rs.getInt("str"));
                    sendPack.addString("\",\"ip0\":0,\"iq0\":0,\"iAge\":\"");
                    sendPack.addInt(rs.getInt("age"));
                    sendPack.addString("\",\"iWIS\":\"");
                    sendPack.addInt(rs.getInt("WIS"));
                    sendPack.addString("\",\"intExpToLevel\":\"");
                    sendPack.addInt(getXpToLevel(level)); //Calculate this
                    sendPack.addString("\",\"intGold\":");
                    sendPack.addInt(rs.getInt("gold"));
                    sendPack.addString(",\"intMP\":");
                    sendPack.addInt(19+level); //Calculate this
                    sendPack.addString(",\"iBankSlots\":");
                    sendPack.addInt(rs.getInt("slotBank"));
                    sendPack.addString(",\"iHouseSlots\":");
                    sendPack.addInt(rs.getInt("slotHouse"));
                    sendPack.addString(",\"id0\":0,\"intColorSkin\":\"");
                    sendPack.addInt(rs.getInt("plaColorSkin"));
                    sendPack.addString("\",\"intMPMax\":");
                    sendPack.addInt(19+level); //Calculate this
                    sendPack.addString(",\"intHPMax\":");
                    sendPack.addInt(700+((level+1)*20)); //Calculate this
                    sendPack.addString(",\"dUpgExp\":\"");
                    sendPack.addString("2009-01-20T17:53:00"/*+rs.getString("upgDate")*/);
                    sendPack.addString("\",\"iUpg\":\"");
                    sendPack.addInt(rs.getInt("upgrade"));
                    sendPack.addString("\",\"CharID\":\"");
                    sendPack.addInt(id);
                    sendPack.addString("\",\"strClassName\":\"");
                    sendPack.addString(cn);
                    sendPack.addString("\",\"iINT\":\"");
                    sendPack.addInt(rs.getInt("INT"));
                    sendPack.addString("\",\"ItemID\":\"");
                    sendPack.addInt(rs.getInt("currentClass"));
                    sendPack.addString("\",\"lastArea\":\"");
                    sendPack.addString(rs.getString("lastVisited"));
                    sendPack.addString("\",\"intColorTrim\":\"");
                    sendPack.addInt(rs.getInt("cosColorTrim"));
                    sendPack.addString("\",\"intDBExp\":");
                    sendPack.addInt(rs.getInt("xp"));
                    sendPack.addString(",\"intExp\":");
                    sendPack.addInt(rs.getInt("xp"));
                    sendPack.addString(",\"UserID\":\"");
                    sendPack.addInt(id);
                    sendPack.addString("\",\"ia1\":\"0\",\"ia0\":0,\"intHP\":");
                    sendPack.addInt(700+((level+1)*20)); //Calculate this
                    sendPack.addString(",\"strQuests\":\"000000000000000000000QT000000000000000000000000000\",\"bitSuccess\":\"1\",\"strHairName\":\"");
                    sendPack.addString(rs.getString("hairName"));
                    sendPack.addString("\",\"intColorEye\":\"");
                    sendPack.addInt(rs.getInt("plaColorEyes"));
                    sendPack.addString("\",\"iLCK\":\"");
                    sendPack.addInt(rs.getInt("LCK"));
                    sendPack.addString("\",\"eqp\":{");
                    sendPack.addString(equip);
                    sendPack.addString("},\"iDBCP\":");
                    sendPack.addInt(cp);
                    sendPack.addString(",\"intDBGold\":");
                    sendPack.addInt(rs.getInt("LCK"));
                    sendPack.addString(",\"intActivationFlag\":\"");
                    sendPack.addInt(rs.getInt("emailActive"));
                    sendPack.addString("\",\"intAccessLevel\":\"");
                    sendPack.addInt(rs.getInt("access"));
                    sendPack.addString("\",\"strHairFilename\":\"");
                    sendPack.addString(rs.getString("hairFile"));
                    sendPack.addString("\",\"intColorHair\":\"");
                    sendPack.addInt(rs.getInt("plaColorHair"));
                    sendPack.addString("\",\"HairID\":\"");
                    sendPack.addInt(rs.getInt("hairID"));
                    sendPack.addString("\",\"strGender\":\"");
                    sendPack.addString(rs.getString("gender"));
                    sendPack.addString("\",\"strUsername\":\"");
                    sendPack.addString(user);
                    sendPack.addString("\",\"iDEX\":\"");
                    sendPack.addInt(rs.getInt("DEX"));
                    sendPack.addString("\",\"intCoins\":");
                    sendPack.addInt(rs.getInt("coins"));
                    sendPack.addString(",\"iEND\":\"");
                    sendPack.addInt(rs.getInt("END"));
                    sendPack.addString("\",\"strMapName\":\"");
                    sendPack.addString(room.roomName);
                    sendPack.addString("\"}}}}");
                    send(sendPack,true);
                    sendPack.clean();
                    debug("Sent user data: "+id2);
                    rs.close();
                }
            }
        } catch (Exception e) {
            debug("Exception in retrieve user data: "+e.getMessage()+", uid: "+uid);
            try {
                Thread.sleep(200);
            } catch (Exception e2) {
                debug("retrieveUserData sleep failed: "+e2.getMessage());
            }
            if (doAgain) {
                retrieveUserData(uid, false);
            } else {
                if (gameServer.userID[id2] == this.userid) {
                    this.finalize();
                }
            }
        }
    }

    protected void sellItem(int itemid, int adjustid)
    {
        try {
            int sellprice = 0;
            int iscoins = 0;
            int qty = 1;
            String isitem="";
            Packet sendPack = new Packet();
            ResultSet es = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemid="+itemid);
            if (es.next()) {
                sellprice = es.getInt("iCost")/4;
                iscoins = es.getInt("bCoins");
                isitem = es.getString("sType");
                qty = es.getInt("iQty");
            }
            es.close();
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_items WHERE userid="+this.userid+" AND id="+adjustid+" AND equipped=0 AND itemid="+itemid);
            if (rs.next()) {

                if(iscoins!=1){
                    Main.sql.doupdate("UPDATE wqw_users SET gold=gold+"+sellprice+" WHERE id="+this.userid);
                }else{
                    Main.sql.doupdate("UPDATE wqw_users SET coins=coins+"+sellprice+" WHERE id="+this.userid);
                }
                if(isitem.equals("Item") || isitem.equals("Quest Item")){
                    Main.sql.doupdate("UPDATE wqw_items SET iQty=iQty-1 WHERE itemid="+itemid+" AND userid="+this.userid);
                    if(qty==1){
                        Main.sql.doupdate("DELETE FROM wqw_items WHERE id="+adjustid);
                    }
                } else {
                    Main.sql.doupdate("DELETE FROM wqw_items WHERE id="+adjustid);
                }
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"sellItem\",\"intAmount\":"+sellprice+",\"CharItemID\":"+adjustid+",\"bCoins\":\""+iscoins+"\"}}}");
            } else {
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"sellItem\",\"bitSuccess\":0,\"strMessage\":\"Item Does Not Exist\",\"CharItemID\":-1}}}");
            }
            rs.close();
            send(sendPack, true);
        } catch (Exception e) {
            debug("Exception in sell item: "+e.getMessage());
        }
    }

    protected void sendArea(boolean doAgain)
    {
        try {
            ResultSet is = Main.sql.doquery("SELECT * FROM wqw_maps WHERE id="+this.playerRoom.roomType+" LIMIT 1");
            if (is.next()) {
                String[] mons = is.getString("monsterid").split(",");
                String[] monnumbs = is.getString("monsternumb").split(",");
                String[] monframe = is.getString("monsterframe").split(",");
                Packet sendPack = new Packet();
                if (is.getString("monsternumb").equals("")) {
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"moveToArea\",\"areaName\":\"");
                    sendPack.addString(this.playerRoom.roomName+"-"+gameServer.getPlayerRoom(this.account)[1]);
                    sendPack.addString("\",\"intKillCount\":0,\"uoBranch\":[");
                    for (int i = 0; i < 10; i++) {
                        if (!this.playerRoom.roomSlot[i].equals("")) {
                            int playerI = gameServer.getPlayerID(this.playerRoom.roomSlot[i]);
                            if (playerI > 0 && !this.playerRoom.roomSlot[i].equals("")) {
                                if (i != 0) {
                                    sendPack.addString(",");
                                }
                                sendPack.addString(this.playerRoom.getPlayerInfo(i));
                            }
                        }
                    }
                    sendPack.addString("],\"strMapFileName\":\"");
                    sendPack.addString(this.playerRoom.fileName);
                    sendPack.addString("\",\"intType\":\"2\",\"monBranch\":[],\"sExtra\":\"\",\"areaId\":");
                    sendPack.addString(""+gameServer.getPlayerRoom(this.account)[0]);
                    sendPack.addString(",\"strMapName\":\"");
                    sendPack.addString(this.playerRoom.roomName);
                    sendPack.addString("\"}}}");
                    send(sendPack,true);
                } else {
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"intType\":\"2\",\"mondef\":[");
                    for (int e = 0; e < mons.length; e++) {
                        if (e != 0) {
                            sendPack.addString(",");
                        }
                        is.close();
                        ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_monsters WHERE MonID="+mons[e]+" LIMIT 1");
                        if (rs.next()) {
                            sendPack.addString("{\"sRace\":\""+rs.getString("sRace")+"\",\"MonID\":\""+rs.getInt("MonID")+"\",\"intMP\":\""+rs.getInt("intMPMax")+"\",\"intGold\":"+rs.getInt("intGold")+",\"intLevel\":\""+rs.getInt("intLevel")+"\",\"strDrops\":\""+rs.getString("strDrops")+"\",\"intExp\":"+rs.getInt("intExp")+",\"intMPMax\":"+rs.getInt("intMPMax")+",\"iDPS\":\""+rs.getInt("iDPS")+"\",\"intHP\":\""+rs.getInt("intHPMax")+"\",\"strBehave\":\"walk\",\"intHPMax\":\""+rs.getInt("intHPMax")+"\",\"strElement\":\""+rs.getString("strElement")+"\",\"intRSC\":\""+rs.getInt("intRSC")+"\",\"strLinkage\":\""+rs.getString("strLinkage")+"\",\"strActions\":[\"a6\"],\"drops\":[");
                            String[] drops = rs.getString("strDrops").split(",");
                            for (int a = 0; a < drops.length; a++) {
                                if (a != 0) {
                                    sendPack.addString(",");
                                }
                                String [] droppart = drops[a].split(":");
                                sendPack.addString("{\"ItemID\":\""+droppart[0]+"\",\"iRate\":\""+(Double.parseDouble(droppart[2])*100)+"\",\"iQty\":\"1\"}");
                            }
                            sendPack.addString("],\"strMonFileName\":\""+rs.getString("strMonFileName")+"\",\"strMonName\":\""+rs.getString("strMonName")+"\",\"intRep\":\""+rs.getInt("intRep")+"\"}");
                        }
                        rs.close();
                    }
                    sendPack.addString("],\"monBranch\":[");
                    sendPack.addString(this.playerRoom.getMon(monnumbs));
                    sendPack.addString("],\"sExtra\":\"\",\"areaId\":"+gameServer.getPlayerRoom(this.account)[0]+",\"cmd\":\"moveToArea\",\"areaName\":\""+this.playerRoom.roomName+"-"+gameServer.getPlayerRoom(this.account)[1]+"\",\"intKillCount\":0,\"uoBranch\":[");
                    for (int i = 0; i < 10; i++) {
                        if (!this.playerRoom.roomSlot[i].equals("")) {
                            int playerI = gameServer.getPlayerID(this.playerRoom.roomSlot[i]);
                            if (playerI > 0 && !this.playerRoom.roomSlot[i].equals("")) {
                                if (i != 0) {
                                    sendPack.addString(",");
                                }
                                sendPack.addString(this.playerRoom.getPlayerInfo(i));
                            }
                        }
                    }
                    sendPack.addString("],\"strMapFileName\":\""+this.playerRoom.fileName+"\",\"wB\":[],\"monmap\":[");
                    for (int u = 0; u < monnumbs.length; u++) {
                        if (u != 0) {
                            sendPack.addString(",");
                        }
                        sendPack.addString("{\"MonMapID\":\""+(u+1)+"\",\"strFrame\":\""+monframe[u]+"\",\"intRSS\":\"-1\",\"MonID\":\""+monnumbs[u]+"\",\"bRed\":\"0\"}");
                    }
                    sendPack.addString("],\"strMapName\":\""+this.playerRoom.roomName+"\"}}}");
                    send(sendPack,true);
                }
                sendPack.clean();
                sendPack.addString("%xt%server%-1%You joined \""+this.playerRoom.roomName+"-"+gameServer.getPlayerRoom(this.account)[1]+"\"!%");
                send(sendPack,true);
                sendPack.clean();
                sendPack.addXMLSingle(1,"msg t","sys");
                sendPack.addXMLSingle(1,"body action","uER","r",""+gameServer.getPlayerRoom(this.account)[0]*gameServer.getPlayerRoom(this.account)[1]);
                sendPack.addXMLSingle(1,"u i",""+gameServer.getPlayerID(this.account),"m",""+gameServer.getModerator(gameServer.getPlayerID(this.account)),"s","0","p",""+(gameServer.room[gameServer.getPlayerRoom(this.account)[0]][gameServer.getPlayerRoom(this.account)[1]].getPlayerSlot(this.account)+1));
                sendPack.addXML("n","",1);
                sendPack.addCDATA(this.account);
                sendPack.addXML("n","",2);
                sendPack.addXML("vars","",0);
                sendPack.addXMLSingle(2,"u");
                sendPack.addXMLSingle(2,"body");
                sendPack.addXMLSingle(2,"msg");
                gameServer.writeMapPacket(this.account, sendPack, true, true);
                gameServer.sendPlayerDetails(this.account);
            }
        } catch (Exception e) {
            debug("Exception in send area: "+e.getMessage());
            if (doAgain) {
                sendArea(false);
            } else {
                this.finalize();
            }
        }
    }

    protected void sendLobby()
    {
        Packet sendPack = new Packet();
        sendPack.addXMLSingle(1,"msg t","sys");
        sendPack.addXMLSingle(1,"body action","joinOK","r",""+gameServer.getPlayerRoom(this.account)[0]);
        sendPack.addXMLSingle(0,"pid id",""+(this.playerRoom.getPlayerSlot(this.account)+1));
        sendPack.addXMLSingle(0,"vars");
        sendPack.addXMLSingle(1,"uLs r",""+gameServer.getPlayerRoom(this.account)[0]*gameServer.getPlayerRoom(this.account)[1]);
        
        for (int e = 0; e < 10; e++) {
            if (!this.playerRoom.roomSlot[e].equals("")) {
                int playerI = gameServer.getPlayerID(this.playerRoom.roomSlot[e]);
                if (playerI > 0 && !this.playerRoom.roomSlot[e].equals("")) {
                    int mod = gameServer.getModerator(playerI);
                    sendPack.addXMLSingle(1,"u i",""+playerI,"m",""+mod,"s","0","p",""+(e+1));
                    sendPack.addXML("n","",1);
                    sendPack.addCDATA(gameServer.getCharname(playerI));
                    sendPack.addXML("n","",2);
                    sendPack.addXML("vars","",0);
                    sendPack.addXMLSingle(2,"u");
                }
            }
        }
        sendPack.addXMLSingle(2,"uLs");
        sendPack.addXMLSingle(2,"body");
        sendPack.addXMLSingle(2,"msg");
        send(sendPack, true);
        sendPack.clean();
        debug("Sent lobby: "+this.account);
        sendArea(true);
    }

    protected void sendPolicy()
    {
        Packet sendPack = new Packet();
        sendPack.addXML("cross-domain-policy","",1);
        sendPack.addXMLSingle(0,"allow-access-from domain","*","to-ports",""+Main.port);
        sendPack.addXML("cross-domain-policy","",2);
        send(sendPack, true);
        debug("Sent policy to: "+this.ip);
        this.finalize();
    }

    protected void sendVersion()
    {
        Packet sendPack = new Packet();
        sendPack.addXMLSingle(1,"msg t","sys");
        sendPack.addXMLSingle(1,"body action","apiOK");
        sendPack.addXMLSingle(2,"body");
        sendPack.addXMLSingle(2,"msg");
        send(sendPack, true);
        debug("Sent version to: "+this.ip);
    }

    protected void setAFK(boolean afk)
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%uotls%-1%");
        sendPack.addString(this.account);
        sendPack.addString("%afk:");
        sendPack.addString(afk+"%");
        gameServer.writeMapPacket(this.account,sendPack,true,false);
        sendPack.clean();
        if(afk != this.playerRoom.afk[this.playerSlot]){
            this.playerRoom.afk[this.playerSlot] = afk;
            if (afk) {
                sendPack.addString("%xt%server%-1%You are now Away From Keyboard (AFK).%");
            } else {
                sendPack.addString("%xt%server%-1%You are no longer Away From Keyboard (AFK).%");
            }
            send(sendPack, true);
            sendPack.clean();
        }
        debug("Set afk: "+this.account+", "+afk);
    }
    
    protected void startPvP(String otherchar)
    {
        Packet sendPack = new Packet();
        otherchar = otherchar.toLowerCase();
        if (gameServer.getPlayerID(otherchar) > 0) {
            if ((this.playerRoom.roomType != 4 && this.playerRoom.roomType != 5) || gameServer.getModerator(gameServer.getPlayerID(this.account)) == 1) {
                if (gameServer.pvpOn[gameServer.getPlayerID(otherchar)] == true) {
                    if (gameServer.pvpOn[this.accountid] == true) {
                        if (!otherchar.equals(this.account)) {
                            //playerAttack("aa", 0, "p", gameServer.getPlayerID(otherchar));
                            server.startPvP(gameServer.playerSocket[this.accountid].getRemoteSocketAddress(), gameServer.playerSocket[gameServer.getPlayerID(otherchar)].getRemoteSocketAddress());
                        } else {
                            sendPack.addString("%xt%server%-1%You cannot attack yourself.%");
                            send(sendPack, true);
                        }
                    } else {
                        sendPack.addString("%xt%server%-1%You have PvP set off.%");
                        send(sendPack, true);
                    }
                } else {
                    sendPack.addString("%xt%server%-1%"+otherchar+" has PvP set off.%");
                    send(sendPack, true);
                }
            } else {
                sendPack.addString("%xt%server%-1%This map is a safezone - You cannot PvP here.%");
                send(sendPack, true);
            }
        } else {
            sendPack.addString("%xt%server%-1%"+otherchar+" is not in this zone.%");
            send(sendPack, true);
        }
    }

    protected void unequipItem(int itemid, int adjustid)
    {
        try {
            Packet sendPack = new Packet();
            sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"uid\":");
            sendPack.addInt(this.accountid);
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_equipment WHERE itemID="+itemid+" LIMIT 1");
            if (rs.next()) {
                sendPack.addString(",\"ItemID\":\""+itemid+"\",\"strES\":\""+rs.getString("sES")+"\",\"cmd\":\"unequipItem\"}}}");
            }
            rs.close();
            Main.sql.doupdate("UPDATE wqw_items SET equipped=0 WHERE userid="+this.userid+" AND id="+adjustid);
            gameServer.writeMapPacket(this.account, sendPack, true, false);
        } catch (Exception e) {
            debug("Exception in equip item: "+e.getMessage());
        }
    }

    protected void userChat(int room, String message, String zone)
    {
        Packet sendPack = new Packet();
        sendPack.addString("%xt%chatm%"+room+"%");
        /*if(gameServer.getModerator(gameServer.getPlayerID(this.account)) == 1){
            zone = "moderator";
        }*/
        sendPack.addString(zone+"~"+message);
        sendPack.addString("%"+this.account);
        sendPack.addString("%"+this.userid);
        sendPack.addString("%"+room+"%");
        if (zone.equals("party")) {
            if (gameServer.partyRoom[this.accountid] > 0) {
                gameServer.writePartyPacket(this.account,sendPack,true,false);
            } else {
                sendPack.clean();
                sendPack.addString("%xt%warning%-1%You are not in a party.%");
                send(sendPack,true);
            }
        } else if(zone.equals("moderator")){
            gameServer.writeGlobalPacket(this.account, sendPack, true, false);
        } else {
            gameServer.writeMapPacket(this.account,sendPack,true,false);
        }
        sendPack.clean();
        debug("Sent chat: "+this.account+", "+message+", "+zone);
    }

    protected void userCommand(String cmd)
    {
        Packet sendPack = new Packet();
        boolean startMod = true;
        int i = 0;
        byte[] cmdBytes = cmd.getBytes();
        String command = "";
        while (cmdBytes[i] != 0x20 && i < cmd.length() - 1) {
            i++;
        }
        if (i == cmd.length() - 1) {
            command = cmd.substring(0, cmd.length()).toLowerCase();
        } else {
            command = cmd.substring(0, i).toLowerCase();
        }
        if (command.equals("help") && gameServer.getModerator(gameServer.getPlayerID(this.account)) == 0) {
            sendPack.clean();
            sendPack.addString("%xt%server%-1%PvP (user) - Attempts to PvP (user).%");
            send(sendPack, true);
            sendPack.clean();
            sendPack.addString("%xt%server%-1%Setpvp - Turns PvP on/off (Must be out of combat for 30 seconds+).%");
            send(sendPack, true);
            startMod = false;
        } else if (command.equals("pvp")){
            debug("Starting PvP: "+cmd.substring(i + 1));
            startPvP(cmd.substring(i + 1));
            startMod = false;
        } else if (command.equals("setpvp")){
            debug("Setting PvP: "+this.account);
            if (this.fighting == false) {
                gameServer.pvpOn[this.accountid] = !gameServer.pvpOn[this.accountid];
                sendPack.addString("%xt%server%-1%PvP set to "+gameServer.pvpOn[this.accountid]+".%");
                send(sendPack, true);
            } else {
                sendPack.addString("%xt%server%-1%You must wait at least thirty seconds after a battle to change your PvP status.%");
                send(sendPack, true);
            }
            startMod = false;
        }
        if (gameServer.getModerator(gameServer.getPlayerID(this.account)) == 1 && startMod == true){
            if (command.equals("ban")){
                debug("Banning: "+cmd.substring(i + 1));
                gameServer.banPlayer(cmd.substring(i + 1), this.account);
            } else if (command.equals("cp")) {
                int cp = Integer.parseInt(cmd.substring(i + 1));
                debug("Adding "+cp+" CP to "+this.account);
                sendPack.clean();
                sendPack.addString(addRewards(0, 0, cp, 0, "m", 0));
                send(sendPack, true);
            } else if (command.equals("heal")) {
                gameServer.hp[this.accountid] = gameServer.hpmax[this.accountid];
                gameServer.mp[this.accountid] = gameServer.mpmax[this.accountid];
                sendPack.clean();
                sendPack.addString("%xt%uotls%-1%"+this.account+"%intHP:"+gameServer.hp[this.accountid]+",intMP:"+gameServer.mp[this.accountid]+"%");
                gameServer.writeMapPacket(this.account, sendPack, true, false);
            } else if (command.equals("help")) {
                sendPack.clean();
                sendPack.addString("%xt%server%-1%Ban/Unban (user) - Bans/Unbans (user).%");
                send(sendPack, true);
                sendPack.clean();
                sendPack.addString("%xt%server%-1%CP/Gold/XP (i) - Gives you (i) amount of CP/Gold/XP.%");
                send(sendPack, true);
                sendPack.clean();
                sendPack.addString("%xt%server%-1%Heal - Heals you to max HP and MP.%");
                send(sendPack, true);
                sendPack.clean();
                sendPack.addString("%xt%server%-1%Kick (user) - Kicks (user).%");
                send(sendPack, true);
                sendPack.clean();
                sendPack.addString("%xt%server%-1%Mod (message) - Displays (message) in the mod chat style.%");
                send(sendPack, true);
            } else if (command.equals("gold")) {
                int gold = Integer.parseInt(cmd.substring(i + 1));
                debug("Adding "+gold+" gold to "+this.account);
                sendPack.clean();
                sendPack.addString(addRewards(0, gold, 0, 0, "m", 0));
                send(sendPack, true);
            } else if (command.equals("kick")){
                debug("Kicking: "+cmd.substring(i + 1));
                gameServer.kickPlayer(cmd.substring(i + 1), this.account);
            } else if (command.equals("mod")){
                userChat(this.playerRoom.roomNumb, cmd.substring(i + 1), "moderator");
            } else if (command.equals("xp")) {
                int xp = Integer.parseInt(cmd.substring(i + 1));
                debug("Adding "+xp+" XP to "+this.account);
                sendPack.clean();
                sendPack.addString(addRewards(xp, 0, 0, 0, "m", 0));
                send(sendPack, true);
            } else if (command.equals("unban")) {
                debug("Unbanning: "+cmd.substring(i + 1));
                gameServer.unbanPlayer(cmd.substring(i + 1), this.account);
            } else {
                sendPack.addString("%xt%server%-1%Invalid command. Use the command help for a list of commands.%");
                send(sendPack, true);
            }
        }
    }

    protected void userMove(int tx, int ty, int speed, boolean cansee)
    {
        if (tx < 0) {
            tx = 0;
        }
        if (ty < 0) {
            ty = 0;
        }
        if (tx > 1000) {
            tx = 1000;
        }
        if (ty > 800) {
            ty = 800;
        }
        Packet sendPack = new Packet();
        sendPack.addString("%xt%uotls%-1%");
        sendPack.addString(this.account);
        sendPack.addString("%sp:"+speed);
        sendPack.addString(",tx:"+tx);
        sendPack.addString(",ty:"+ty);
        sendPack.addString(",strFrame:"+this.playerRoom.frame[this.playerSlot]+"%");
        this.playerRoom.tx[this.playerSlot] = tx;
        this.playerRoom.ty[this.playerSlot] = ty;
        gameServer.writeMapPacket(this.account,sendPack,true,!cansee);
        sendPack.clean();
        debug("Sent move: "+this.account);
    }

    protected void whisperChat(String message, String otheruser)
    {
        Packet sendPack = new Packet();
        if (gameServer.getPlayerID(otheruser) > 0) {
            sendPack.addString("%xt%whisper%-1%");
            sendPack.addString(message);
            sendPack.addString("%"+this.account+"%"+otheruser+"%0%");
            send(sendPack, true);
            gameServer.writePlayerPacket(otheruser,sendPack,true);
            debug("Sent whisper: "+this.account+" to "+otheruser+", "+message);
        } else {
            sendPack.addString("%xt%server%-1%Player "+otheruser+" could not be found%");
            send(sendPack, true);
        }
    }

    /*
     * END COMMAND RESPONSES
     * The above section contains responses for the messages sent to the server by the client
     * Above and below this section are the core functions
     */

    protected void send(Packet pack, boolean addNull)
    {
        String packet = pack.getPacket();
        if (addNull) {
            packet += "\u0000";
        }
        this.socketOut.write(packet);
        this.socketOut.flush();
    }

    protected String read()
    {
        StringBuffer buffer = new StringBuffer();
        int codePoint;
        boolean zeroByteRead = false;
        try {
            do {
                if (!this.hasFinalized) {
                    codePoint = this.socketIn.read();

                    if (codePoint == 0) {
                        zeroByteRead = true;
                    }

                    if (codePoint == -1) {
                        this.finalize();
                    }
                    else if (Character.isValidCodePoint(codePoint)) {
                        buffer.appendCodePoint(codePoint);
                    }
                }
            }
            while (!zeroByteRead);
        } catch (Exception e) {
            debug("Error (read): " + e.getMessage() + ", " + e.getCause());
            this.finalize();
        }
        return buffer.toString();
    }

    /**
     * Create a reader and writer for the socket
     */
    public void run()
    {
        try {
            this.socketIn = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.socketOut = new PrintWriter(this.socket.getOutputStream(), true);
            String packet;
            while((packet = read()) != null && !hasFinalized) {
                parseCMD(getcmd(packet),packet);
            }
        }
        catch (Exception e) {
            debug("Exception (run): " + e.getMessage() + ", " + e.getCause());
        }
        this.finalize();
    }

    /**
     * Closes the reader, the writer and the socket.
     */
    @Override
    protected void finalize() {	 
        try {
            this.hasFinalized = true;
            if(this.userid > 0){
                try {
                    Main.sql.doupdate("UPDATE wqw_users SET curServer='' WHERE id="+this.userid);
                } catch (Exception e){
                    debug("Exception (finalize sql), userid: "+this.userid+", "+e.getMessage());
                }
            }
            if (this.account != null && !this.account.equals("") && gameServer.getPlayerID(this.account) > 0) {
                gameServer.removeuser(this.account);
            } else {
                this.server.remove(this.getRemoteAddress());
            }
            this.socketOut.close();
            this.socket.close();
            this.socketIn.close();
            stop();
        }
        catch (Exception e) {
            debug("Exception (finalize): " + e.getMessage());
            gameServer.removeuser(this.account);
            this.server.remove(this.getRemoteAddress());
            stop();
        }
    }
}
