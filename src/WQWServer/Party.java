package WQWServer;

import java.io.*;
import java.sql.*;

/**
 * @author XVII
 * Partys
 */
public class Party
{

    public String[] partySlot = new String[10];
    public String partyOwner;
    public int partyID;
    public int users = 0;

    public Party(int numb)
    {
        this.partyID = numb;
        for (int i = 0; i < 10; i++) {
            this.partySlot[i] = "";
        }
        this.partyOwner = "";
    }

    protected int getPlayerSlot(String charname)
    {
        for (int i = 0; i < 10; i++) {
            if (partySlot[i].equals(charname)) {
                return i;
            }
        }
        return -1;
    }

    protected void addPlayer(String charname, PrintWriter socket)
    {
        for (int i = 0; i < 10; i++) {
            if (partySlot[i].equals("") || partySlot[i] == null) {
                if (i == 0) {
                    this.partyOwner = charname;
                }
                this.partySlot[i] = charname;
                this.users++;
                break;
            }
        }
    }

    protected String getPlayers()
    {
        String result = "[";
        for (int i = 0; i < 10; i++) {
            if (!partySlot[i].equals("") && partySlot[i] != null) {
                if (i != 0) {
                    result += ",";
                }
                result += "\""+partySlot[i]+"\"";
            }
        }
        result += "]";
        return result;
    }

    protected void removePlayer(String charname)
    {
        int ID = getPlayerSlot(charname);
        this.partySlot[ID] = "";
        if (this.partyOwner.equals("charname")) {
            for (int i = 0; i < 10; i++) {
                if (!partySlot[i].equals("") && partySlot[i] != null) {
                    this.partyOwner = partySlot[i];
                    break;
                }
            }
        }
        this.users--;
    }

    protected void finalize()
    {
        for (int e = 0; e < 10; e++) {
            removePlayer(this.partySlot[e]);
        }
    }
}
