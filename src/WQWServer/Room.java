package WQWServer;

import java.io.*;
import java.sql.*;

/**
 * @author XVII
 * Rooms
 */
public class Room
{

    public String[] roomSlot = new String[10];
    public int roomType;
    public String roomName;
    public String fileName;
    public String[] pad = new String[10];
    public String[] frame = new String[10];
    public int[] tx = new int[10];
    public int[] ty = new int[10];
    public boolean[] afk = new boolean[10];
    public int roomNumb;
    public int users;
    private PrintWriter[] playerSocket = new PrintWriter[10];
    public String[] monsterBehave = new String[64];
    public int[] monsterHP = new int[64];
    public int[] monsterMP = new int[64];
    public int[] monsterHPMax = new int[64];
    public int[] monsterMPMax = new int[64];
    public int[] monsterState = new int[64];
    public int[] monsterType = new int[64];
    public int[] monsterLevel = new int[64];

    public Room(int type, int numb)
    {
        this.roomType = type;
        this.roomNumb = numb;
        for (int i = 0; i < 10; i++) {
            this.roomSlot[i] = "";
            this.pad[i] = "Spawn";
            this.frame[i] = "Enter";
            this.afk[i] = false;
        }
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_maps WHERE id="+type);
            if (rs.next()) {
                this.roomName = rs.getString("name");
                this.fileName = rs.getString("fileName");
                if (!rs.getString("monsternumb").equals("")) {
                    this.monsterBehave = rs.getString("monsternumb").split(",");
                    for (int a = 0; a < this.monsterBehave.length; a++) {
                        ResultSet is = Main.sql.doquery("SELECT * FROM wqw_monsters WHERE MonID="+this.monsterBehave[a]);
                        if (is.next()) {
                            this.monsterHP[a] = is.getInt("intHPMax");
                            this.monsterMP[a] = is.getInt("intMPMax");
                            this.monsterHPMax[a] = is.getInt("intHPMax");
                            this.monsterMPMax[a] = is.getInt("intMPMax");
                            this.monsterLevel[a] = is.getInt("intLevel");
                        }
                        this.monsterType[a] = Integer.parseInt(this.monsterBehave[a]);
                        this.monsterBehave[a] = "walk";
                        this.monsterState[a] = 1;
                    }
                }
            }
            rs.close();
        } catch (Exception e) {
            Main.server.debug("Exception in room: "+e.getMessage());
        }
    }

    protected String getMon(String[] monnumbs)
    {
        Packet sendPack2 = new Packet();
        try {
            for (int o = 0; o < monnumbs.length; o++) {
                ResultSet os = Main.sql.doquery("SELECT * FROM wqw_monsters WHERE MonID="+monnumbs[o]);
                if (os.next()) {
                    if (o != 0) {
                        sendPack2.addString(",");
                    }
                    sendPack2.addString("[\"MonMapID:"+(o+1)+"\",\"MonID:"+monnumbs[o]+"\",\"intState:"+this.monsterState[o]+"\",\"intHP:"+this.monsterHP[o]+"\",\"intMP:"+this.monsterMP[o]+"\",\"intHPMax:"+os.getInt("intHPMax")+"\",\"intMPMax:"+os.getInt("intMPMax")+"\",\"bRed:0\",\"iLvl:"+os.getInt("intLevel")+"\"]");
                }
                os.close();
            }
        } catch (Exception e) {
            Main.server.debug("Exception in get monster: "+e.getMessage());
        }
        return sendPack2.getPacket();
    }

    protected String getPlayerInfo(int slot)
    {
        int i = Main.server.gameServer.getPlayerID(this.roomSlot[slot]);

        Packet pInfo = new Packet();

        pInfo.addString("{\"strFrame\":\""+frame[slot]+"\",");
        pInfo.addString("\"intMP\":"+Main.server.gameServer.mp[i]+",");
        pInfo.addString("\"intLevel\":"+Main.server.gameServer.level[i]+",");
        pInfo.addString("\"entID\":"+i+",");
        pInfo.addString("\"strPad\":\""+pad[slot]+"\",");
        pInfo.addString("\"intMPMax\":"+Main.server.gameServer.mpmax[i]+",");
        pInfo.addString("\"intHP\":"+Main.server.gameServer.hp[i]+",");
        pInfo.addString("\"afk\":"+afk[slot]+",");
        pInfo.addString("\"intHPMax\":"+Main.server.gameServer.hpmax[i]+",");
        pInfo.addString("\"ty\":"+ty[slot]+",");
        pInfo.addString("\"tx\":"+tx[slot]+",");
        pInfo.addString("\"intState\":1,"); //ADD SUPPORT
        pInfo.addString("\"entType\":\"p\",");
        pInfo.addString("\"showHelm\":true,");
        pInfo.addString("\"showCloak\":true,");
        pInfo.addString("\"strUsername\":\""+roomSlot[slot]+"\",");
        pInfo.addString("\"uoName\":\""+roomSlot[slot]+"\"}");
        
        return pInfo.getPacket();
    }

    protected int getPlayerSlot(String charname)
    {
        for (int i = 0; i < 10; i++) {
            if (roomSlot[i].equals(charname)) {
                return i;
            }
        }
        return -1;
    }

    //NOT NEEDED FOR NOW
    /*protected String getPlayerSQL(int slot)
    {
        Packet sqlInfo = new Packet();
        try {
            ResultSet rs = Main.sql.doquery("SELECT * FROM wqw_users WHERE username='"+this.roomSlot[slot]+"'");
            int i = Main.server.gameServer.getPlayerID(this.roomSlot[slot]);
            if (rs.next()) {
                sqlInfo.addString("\"intMP\":"+Main.server.gameServer.mp[i]+",");
                sqlInfo.addString("\"intLevel\":"+rs.getInt("level")+",");

                sqlInfo.addString("\",\"intState:1\",\"intLevel:"+rs.getInt("level"));
                sqlInfo.addString("\",\"intHP:"+Main.server.gameServer.hp[i]);
                sqlInfo.addString("\",\"intMP:"+Main.server.gameServer.mp[i]);
                sqlInfo.addString("\",\"intHPMax:"+Main.server.gameServer.hpmax[i]);
                sqlInfo.addString("\",\"intMPMax:"+Main.server.gameServer.mpmax[i]);
            }
            rs.close();
        } catch (Exception e) {
            Main.server.debug("Exception in room init: "+e.getMessage());
        }
        return sqlInfo.getPacket();
    }*/

    protected void respawnMonster(int monsterid, int monstertype)
    {
        respawnTimer respawn = new respawnTimer();
        respawn.main(monsterid, monstertype, this.roomType, this.roomNumb);
    }

    protected void respawnMonsterDo(int monsterid)
    {
        try {
            ResultSet is = Main.sql.doquery("SELECT * FROM wqw_monsters WHERE MonID="+this.monsterType[monsterid]);
            if (is.next()) {
                this.monsterHP[monsterid] = is.getInt("intHPMax");
                this.monsterMP[monsterid] = is.getInt("intMPMax");
                this.monsterLevel[monsterid] = is.getInt("intLevel");
            }
            this.monsterState[monsterid] = 1;
            int[] temproom = new int[2];
            temproom[0] = this.roomType;
            temproom[1] = this.roomNumb;
            Packet sendPack = new Packet();
            sendPack.addString("%xt%mtls%-1%"+(monsterid+1)+"%intHP:"+this.monsterHP[monsterid]+",intMP:"+this.monsterMP[monsterid]+",intState:1%");
            Main.server.gameServer.writeOtherMapPacket(temproom, sendPack, true);
            sendPack.clean();
            sendPack.addString("%xt%respawnMon%-1%"+(monsterid+1)+"%");
            Main.server.gameServer.writeOtherMapPacket(temproom, sendPack, true);
            is.close();
        } catch (Exception e) {
            this.monsterHP[monsterid] = this.monsterHPMax[monsterid];
            this.monsterMP[monsterid] = this.monsterMPMax[monsterid];
            this.monsterState[monsterid] = 1;
            Main.server.debug("Exception in respawn monster: "+e.getMessage());
            this.monsterState[monsterid] = 1;
            int[] temproom = new int[2];
            temproom[0] = this.roomType;
            temproom[1] = this.roomNumb;
            Packet sendPack = new Packet();
            sendPack.addString("%xt%mtls%-1%"+(monsterid+1)+"%intHP:"+this.monsterHP[monsterid]+",intMP:"+this.monsterMP[monsterid]+",intState:1%");
            Main.server.gameServer.writeOtherMapPacket(temproom, sendPack, true);
            sendPack.clean();
            sendPack.addString("%xt%respawnMon%-1%"+(monsterid+1)+"%");
            Main.server.gameServer.writeOtherMapPacket(temproom, sendPack, true);
        }
    }

    protected void addPlayer(String charname, PrintWriter socket)
    {
        for (int i = 0; i < 10; i++) {
            if (roomSlot[i].equals("") || roomSlot[i] == null) {
                this.roomSlot[i] = charname;
                this.playerSocket[i] = socket;
                this.users++;
                break;
            }
        }
    }

    protected void removePlayer(String charname)
    {
        int ID = getPlayerSlot(charname);
        this.roomSlot[ID] = "";
        this.playerSocket[ID] = null;
        this.pad[ID] = "Spawn";
        this.frame[ID] = "Enter";
        this.afk[ID] = false;
        this.tx[ID] = 0;
        this.ty[ID] = 0;
        this.users--;
    }

    protected void finalize()
    {
        for (int e = 0; e < 10; e++) {
            removePlayer(this.roomSlot[e]);
        }
    }
}
