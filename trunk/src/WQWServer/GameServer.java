package WQWServer;

import java.net.*;
import java.io.*;
import java.sql.*;

/**
 * @author XVII
 * GameServer - The lobby
 */
public class GameServer {

    Functions functions;
    private int users = 0;
    public String[] charName = new String[256]; /* The username in each player slot */
    private PrintWriter[] userSocket = new PrintWriter[256];
    public Socket[] playerSocket = new Socket[256];
    public int[][] charRoom = new int[256][2]; /* The room the player is in */
    public int[] partyRoom = new int[256];
    public int[] hp = new int[256];
    public int[] mp = new int[256];
    public int[] hpmax = new int[256];
    public int[] mpmax = new int[256];
    public int xprate = 1;
    public int goldrate = 1;
    public int[] userID = new int[256];
    public int[] level = new int[256];
    public boolean[] isAlive = new boolean[256];
    public boolean[] pvpOn = new boolean[256];

    public Room[][] room = new Room[256][256];
    public Party[] party = new Party[256];

    protected void init()
    {
        /*for (int e = 0; e < 256; e++) {
            this.charName[e] = "";
            for (int a = 0; a < 256; a++) {
                //Do Something
            }
        }*/
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_settings LIMIT 1");
            if (rs.next()) {
                this.xprate = rs.getInt("xprate");
                this.goldrate = rs.getInt("goldrate");
            }
            rs.close();
        } catch (Exception e) {
            debug("Exception in get rates: "+e.getMessage());
        }
    }

    protected void debug(String msg) {
        Main.debug("[GameServer]", msg);
    }

    protected boolean addToRoom(String newroom, int roomnumb, int playernumb)
    {
        int roomid = getRoomID(newroom);
        if (roomid == 0) {
            return false;
        }
        if (roomnumb > 0) {
            if (this.room[roomid][roomnumb] == null) {
                this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]].removePlayer(this.charName[playernumb]);
                if (this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]].users < 1) {
                    this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]] = null;
                }
                this.room[roomid][roomnumb] = new Room(roomid, roomnumb);
                this.room[roomid][roomnumb].addPlayer(this.charName[playernumb], this.userSocket[playernumb]);
                this.charRoom[playernumb][0] = roomid;
                this.charRoom[playernumb][1] = roomnumb;
                sendJoinRoom(this.charName[playernumb]);
                return true;
            } else if (this.room[4][roomnumb].users < 10) {
                this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]].removePlayer(this.charName[playernumb]);
                if (this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]].users < 1) {
                    this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]] = null;
                }
                this.room[roomid][roomnumb].addPlayer(this.charName[playernumb], this.userSocket[playernumb]);
                this.charRoom[playernumb][0] = roomid;
                this.charRoom[playernumb][1] = roomnumb;
                sendJoinRoom(this.charName[playernumb]);
                return true;
            } else {
                roomnumb++;
                addToRoom(newroom, roomnumb, playernumb);
            }
        }
        for (int e = 1; e < 256; e++) {
            if (this.room[roomid][e] == null) {
                this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]].removePlayer(this.charName[playernumb]);
                if (this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]].users < 1) {
                    this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]] = null;
                }
                this.room[roomid][e] = new Room(roomid, e);
                this.room[roomid][e].addPlayer(this.charName[playernumb], this.userSocket[playernumb]);
                this.charRoom[playernumb][0] = roomid;
                this.charRoom[playernumb][1] = e;
                sendJoinRoom(this.charName[playernumb]);
                return true;
            } else if (this.room[4][e].users < 10) {
                this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]].removePlayer(this.charName[playernumb]);
                if (this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]].users < 1) {
                    this.room[this.charRoom[playernumb][0]][this.charRoom[playernumb][1]] = null;
                }
                this.room[roomid][e].addPlayer(this.charName[playernumb], this.userSocket[playernumb]);
                this.charRoom[playernumb][0] = roomid;
                this.charRoom[playernumb][1] = e;
                sendJoinRoom(this.charName[playernumb]);
                return true;
            } else {
                roomnumb++;
                addToRoom(newroom, roomnumb, playernumb);
            }
        }
        return false;
    }

    public int adduser(String username, PrintWriter sockout, Socket socket, int level)
    {
        if (getPlayerID(username) > 0 ) {
            kickPlayer(username, "the server for multiple logins.");
        }
        for (int i = 1; i < 256; i++) {
            if (this.charName[i] == null || this.charName[i].equals("")) {
                this.charName[i] = username;
                this.userSocket[i] = sockout;
                this.playerSocket[i] = socket;
                this.hp[i] = 700+((level+1)*20);
                this.mp[i] = 19+level;
                this.hpmax[i] = 700+((level+1)*20);
                this.mpmax[i] = 19+level;
                this.level[i] = level;
                this.isAlive[i] = true;
                this.pvpOn[i] = true;
                for (int e = 1; e < 256; e++) {
                    if (this.room[4][e] == null) {
                        this.room[4][e] = new Room(4, e);
                        this.room[4][e].addPlayer(username, sockout);
                        this.charRoom[i][0] = 4;
                        this.charRoom[i][1] = e;
                        break;
                    } else if (this.room[4][e].users < 10) {
                        this.room[4][e].addPlayer(username, sockout);
                        this.charRoom[i][0] = 4;
                        this.charRoom[i][1] = e;
                        break;
                    }
                }
                this.users++;
                Main.sql.doupdate("UPDATE wqw_servers SET count=count+1 WHERE name='"+Main.serverName+"'");
                return i;
            }
        }
        return -1;
    }

    protected int addToParty(String charname, int partyid)
    {
        int playernumb = getPlayerID(charname);
        if (partyid > 0) {
            if (this.party[partyid] == null) {
                this.party[partyid] = new Party(partyid);
                this.party[partyid].addPlayer(this.charName[playernumb], this.userSocket[playernumb]);
                this.partyRoom[playernumb] = partyid;
                return partyid;
            } else if (this.party[partyid].users < 10) {
                this.party[partyid].addPlayer(this.charName[playernumb], this.userSocket[playernumb]);
                this.partyRoom[playernumb] = partyid;
                return partyid;
            }
        }
        for (int e = 1; e < 256; e++) {
            if (this.party[e] == null) {
                this.party[e] = new Party(e);
                this.party[e].addPlayer(this.charName[playernumb], this.userSocket[playernumb]);
                this.partyRoom[playernumb] = e;
                return e;
            } else if (this.party[e].users < 10) {
                this.party[e] = new Party(e);
                this.party[e].addPlayer(this.charName[playernumb], this.userSocket[playernumb]);
                this.partyRoom[playernumb] = e;
                return e;
            }
        }
        return 0;
    }

    protected boolean checkInUse(Socket socket)
    {
        for (int i = 1; i < 256; i++) {
            if (this.playerSocket[i] != null) {
                if (Main.getip(this.playerSocket[i]).equals(Main.getip(socket))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void leaveParty(String charname)
    {
        int playernumb = getPlayerID(charname);
        this.party[this.partyRoom[playernumb]].removePlayer(charname);
        if (this.party[this.partyRoom[playernumb]].users < 2) {
            for (int i = 1; i < 256; i++) {
                if (this.partyRoom[i] == this.partyRoom[playernumb]) {
                    Packet sendPack = new Packet();
                    sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pc\",\"pid\":"+this.partyRoom[i]+"}}}");
                    writePartyPacket(this.charName[i],sendPack,true,false);
                    this.party[this.partyRoom[i]].removePlayer(this.charName[i]);
                    this.partyRoom[i] = 0;
                    break;
                }
            }
            this.party[this.partyRoom[playernumb]] = null;
        }
        this.partyRoom[playernumb] = 0;
    }

    public void removeuser(String charname)
    {
        int i = getPlayerID(charname);
        if (i >= 1 && i <= 255) {
            Main.server.remove(this.playerSocket[i].getRemoteSocketAddress());
            this.room[this.charRoom[i][0]][this.charRoom[i][1]].removePlayer(charname);
            Packet sendPack = new Packet();
            sendPack.addString("%xt%exitArea%-1%");
            sendPack.addInt(getPlayerID(this.charName[i]));
            sendPack.addString("%"+this.charName[i]+"%");
            writeMapPacket(this.charName[i],sendPack,true,true);
            sendPack.clean();
            sendPack.addXMLSingle(1,"msg t","sys");
            sendPack.addXMLSingle(1,"body action","userGone","r",""+getPlayerRoom(this.charName[i])[0]*getPlayerRoom(this.charName[i])[1]);
            sendPack.addXMLSingle(0,"user id",""+getPlayerID(this.charName[i]));
            sendPack.addXMLSingle(2,"body");
            sendPack.addXMLSingle(2,"msg");
            writeMapPacket(this.charName[i],sendPack,true,true);
            if (this.partyRoom[i] > 0) {
                sendPack.clean();
                sendPack.addString("{\"t\":\"xt\",\"b\":{\"r\":-1,\"o\":{\"cmd\":\"pr\",\"owner\":\"");
                sendPack.addString(party[this.partyRoom[i]].partyOwner);
                sendPack.addString("\",\"pid\":"+this.partyRoom[i]+",\"typ\":\"l\",\"unm\":\""+this.charName[i]+"\"}}}");
                writePartyPacket(charName[i], sendPack, true, false);
                leaveParty(this.charName[i]);
            }
            this.charName[i] = "";
            this.userSocket[i] = null;
            this.playerSocket[i] = null;
            this.charRoom[i][0] = 0;
            this.charRoom[i][1] = 0;
            this.hp[i] = 0;
            this.mp[i] = 0;
            this.hpmax[i] = 0;
            this.mpmax[i] = 0;
            this.users--;
            Main.sql.doupdate("UPDATE wqw_servers SET count=count-1 WHERE name='"+Main.serverName+"'");
            debug("User removed: "+charname);
        } else {
            Main.server.remove(this.playerSocket[i].getRemoteSocketAddress());
            this.room[this.charRoom[i][0]][this.charRoom[i][1]].removePlayer(charname);
            this.users--;
            Main.sql.doupdate("UPDATE wqw_servers SET count=count-1 WHERE name='"+Main.serverName+"'");
        }
    }

    protected void sendJoinRoom(String username)
    {
        Packet sendPack = new Packet();
        sendPack.addXMLSingle(1,"msg t","sys");
        sendPack.addXMLSingle(1,"body action","uER","r",""+getPlayerRoom(username)[0]*getPlayerRoom(username)[1]);
        sendPack.addXMLSingle(1,"u i",""+getPlayerID(username),"m",""+getModerator(getPlayerID(username)),"s","0","p",""+(                        this.room[getPlayerRoom(username)[0]][getPlayerRoom(username)[1]].getPlayerSlot(username)+1));
        sendPack.addXML("n","",1);
        sendPack.addCDATA(username);
        sendPack.addXML("n","",2);
        sendPack.addXML("vars","",0);
        sendPack.addXMLSingle(2,"u");
        sendPack.addXMLSingle(2,"body");
        sendPack.addXMLSingle(2,"msg");
        writeMapPacket(username, sendPack, true, true);
        sendPlayerDetails(username);
    }

    protected String getCharname(int ID)
    {
        return this.charName[ID];
    }

    protected int getLevel(String charname)
    {
        try {
            ResultSet rs = Main.sql.doquery("SELECT level FROM wqw_users WHERE username='"+charname+"' LIMIT 1");
            if (rs.next()) {
                return rs.getInt("level");
            }
            rs.close();
        } catch (Exception e) {
            debug("Exception in get level: "+e.getMessage());
        }
        return 0;
    }
    
    protected int getModerator(int ID)
    {
        String charname = getCharname(ID);
        try {
            ResultSet rs = Main.sql.doquery("SELECT moderator FROM wqw_users WHERE username='"+charname+"' LIMIT 1");
            if (rs.next()) {
                return rs.getInt("moderator");
            }
            rs.close();
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    protected int getVIP(int ID)
    {
        String charname = getCharname(ID);
        try {
            ResultSet rs = Main.sql.doquery("SELECT vip FROM wqw_users WHERE username='"+charname+"' LIMIT 1");
            if (rs.next()) {
                return rs.getInt("vip");
            }
            rs.close();
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    protected int getFD(int ID)
    {
        String charname = getCharname(ID);
        try {
            ResultSet rs = Main.sql.doquery("SELECT fd FROM wqw_users WHERE username='"+charname+"' LIMIT 1");
            if (rs.next()) {
                return rs.getInt("fd");
            }
            rs.close();
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    protected int getPlayerID(String charname)
    {
        int result = -1;
        int i = 1;
        do {
            if (this.charName[i] != null && this.charName[i].equals(charname) && !this.charName[i].equals("")) {
                result = i;
                break;
            }
            i++;
        } while (i < 255);
        return result;
    }

    protected int[] getPlayerRoom(String charname)
    {
        int ID = getPlayerID(charname);
        int result[] = new int[2];
        result[0] = this.charRoom[ID][0];
        result[1] = this.charRoom[ID][1];
        return result;
    }

    protected int getRoomID(String roomname)
    {
        try {
            ResultSet rs = Main.sql.doquery("SELECT id FROM wqw_maps WHERE name='"+roomname+"' LIMIT 1");
            if (rs.next()) {
                return rs.getInt("id");
            }
            rs.close();
        } catch (Exception e) {
            debug("Exception in get room id: "+e.getMessage());
        }
        return 0;
    }
    protected String getRoomName(int roomid)
    {
        try {
            ResultSet rs = Main.sql.doquery("SELECT name FROM wqw_maps WHERE id='"+roomid+"' LIMIT 1");
            if (rs.next()) {
                return rs.getString("name");
            }
            rs.close();
        } catch (Exception e) {
            debug("Exception in get room name: "+e.getMessage());
        }
        return "null";
    }

    protected void sendPlayerDetails(String charname)
    {
        Packet sendPack = new Packet();
        Room cRoom = this.room[getPlayerRoom(charname)[0]][getPlayerRoom(charname)[1]];
        int slot = cRoom.getPlayerSlot(charname);
        int i = getPlayerID(charname);
        sendPack.addString("%xt%uotls%-1%");
        sendPack.addString(charname);
        sendPack.addString("%afk:");
        sendPack.addString(""+cRoom.afk[slot]);
        sendPack.addString(",intHP:");
        sendPack.addInt(this.hp[i]);
        sendPack.addString(",strPad:");
        sendPack.addString(cRoom.pad[slot]);
        sendPack.addString(",intMPMax:");
        sendPack.addInt(this.mp[i]);
        sendPack.addString(",uoName:");
        sendPack.addString(charname);
        sendPack.addString(",tx:");
        sendPack.addInt(cRoom.tx[slot]);
        sendPack.addString(",ty:");
        sendPack.addInt(cRoom.ty[slot]);
        sendPack.addString(",intState:1,intLevel:");
        sendPack.addInt(getLevel(charname));
        sendPack.addString(",strUsername:");
        sendPack.addString(charname);
        sendPack.addString(",intHPMax:");
        sendPack.addInt(this.hpmax[i]);
        sendPack.addString(",intMP:");
        sendPack.addInt(this.mpmax[i]);
        sendPack.addString(",strFrame:");
        sendPack.addString(cRoom.frame[slot]+"%");
        writeMapPacket(charname, sendPack, true, true);
    }

    public void writeGlobalPacket(String charname, Packet pack, boolean addNull, boolean notme)
    {
        int i = 1;
        int charI = getPlayerID(charname);
        String packet = pack.getPacket();
        if (addNull) {
            packet += "\u0000";
        }
        do {
            if (!(notme == true && i == charI) && this.userSocket[i] != null) {
                this.userSocket[i].write(packet);
                this.userSocket[i].flush();
            }
            i++;
        } while (i < 255);
    }

    public void writeOtherMapPacket(int[] room, Packet pack, boolean addNull)
    {
        int i = 1;
        String packet = pack.getPacket();
        if (addNull) {
            packet += "\u0000";
        }
        do {
            if ((this.charRoom[i][0] == room[0]) && (this.charRoom[i][1] == room[1])) {
                this.userSocket[i].write(packet);
                this.userSocket[i].flush();
            }
            i++;
        } while (i < 255);
    }

    public void writeMapPacket(String charname, Packet pack, boolean addNull, boolean notme)
    {
        int i = 1;
        int charI = getPlayerID(charname);
        String packet = pack.getPacket();
        if (addNull) {
            packet += "\u0000";
        }
        do {
            if (!(notme == true && i == charI) && (this.charRoom[i][0] == this.charRoom[charI][0]) && (this.charRoom[i][1] == this.charRoom[charI][1])) {
                this.userSocket[i].write(packet);
                this.userSocket[i].flush();
            }
            i++;
        } while (i < 255);
    }

    public void writePartyPacket(String charname, Packet pack, boolean addNull, boolean notme)
    {
        int i = 1;
        int charI = getPlayerID(charname);
        String packet = pack.getPacket();
        if (addNull) {
            packet += "\u0000";
        }
        do {
            if (!(notme == true && i == charI) && (this.partyRoom[i] == this.partyRoom[charI])) {
                this.userSocket[i].write(packet);
                this.userSocket[i].flush();
            }
            i++;
        } while (i < 255);
    }

    public void writePlayerPacket(String charname, Packet pack, boolean addNull)
    {
        int i = getPlayerID(charname);
        String packet = pack.getPacket();
        if (addNull) {
            packet += "\u0000";
        }
        this.userSocket[i].write(packet);
        this.userSocket[i].flush();
    }

    protected void kickPlayer(String charname, String thisname)
    {
        try {
            Packet sendPack = new Packet();
            charname = charname.toLowerCase();
            thisname = thisname.toLowerCase();
            if (charname.equals("xvii") | charname.equals("divien") | charname.equals("cris")) {
                if (!thisname.equals("xvii") && !thisname.equals("divien") && !thisname.equals("cris")) {
                    sendPack.addString("%xt%server%-1%"+thisname+" has attempted to kick "+charname+" and has been gannonbanned for insubordination.%");
                    writeGlobalPacket(charname, sendPack, true, false);
                    kickPlayer(thisname, charname);
                }
            } else {
                int num = getPlayerID(charname);
                if (num > 0) {
                    Main.sql.doupdate("UPDATE wqw_users SET curServer='' WHERE username='"+charname+"'");
                    if (userSocket[num] != null) {
                        Main.server.remove(playerSocket[num].getRemoteSocketAddress());
                        removeuser(charname);
                    } else {
                        removeuser(charname);
                    }
                    sendPack.addString("%xt%server%-1%"+charname+" has been kicked by "+thisname+".%");
                    writeGlobalPacket(charname, sendPack, true, false);
                }
            }
        } catch (Exception e) {
            debug("Exception in kick player: "+e.getMessage());
        }
    }

    protected void banPlayer(String charname, String thisname)
    {
        try {
            Packet sendPack = new Packet();
            charname = charname.toLowerCase();
            thisname = thisname.toLowerCase();
            if (charname.equals("xvii") | charname.equals("divien") | charname.equals("cris")) {
                if (!thisname.equals("xvii") && !thisname.equals("divien") && !thisname.equals("cris")) {
                    sendPack.addString("%xt%server%-1%"+thisname+" has attempted to ban "+charname+" and has been gannonbanned for insubordination.%");
                    writeGlobalPacket(charname, sendPack, true, false);
                    kickPlayer(thisname, charname);
                }
            } else {
                Main.sql.doupdate("UPDATE wqw_users SET banned=1 WHERE username='"+charname+"'");
                sendPack.addString("%xt%server%-1%"+charname+" has been banned by "+thisname+".%");
                writeGlobalPacket(charname, sendPack, true, false);
                kickPlayer(charname, thisname);
            }
        } catch (Exception e) {
            debug("Exception in ban player: "+e.getMessage());
        }
    }

    protected void unbanPlayer(String charname, String thisname)
    {
        try {
            Packet sendPack = new Packet();
            Main.sql.doupdate("UPDATE wqw_users SET banned=0 WHERE username='"+charname+"'");
            sendPack.addString("%xt%server%-1%"+charname+" has been unbanned by "+thisname+".%");
            writeMapPacket(thisname, sendPack, true, false);
        } catch (Exception e) {
            debug("Exception in unban player: "+e.getMessage());
        }
    }

    protected void finalize()
    {
        for (int e = 0; e < 256; e++) {
            /* Parties */
            if (this.party[e] != null) {
                this.party[e].finalize();
                this.party[e] = null;
            }
            /* Rooms */
            for (int a = 0; a < 256; a++) {
                if (this.room[e][a] != null) {
                    this.room[e][a].finalize();
                    this.room[e][a] = null;
                }
            }
            /* Players */
            if (this.charName[e] != null) {
                removeuser(this.charName[e]);
            }
        }
    }
}
