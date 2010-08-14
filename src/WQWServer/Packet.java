package WQWServer;

/**
 * @author XVII
 * Creates a packet ready for sending
 */

public class Packet {

    /**
     * Usage:
     * Create a packet (Packet sendPack = new Packet();)
     * Add data into it (sendPack.addInt(playerLevel);)
     * Send it (Send the value sendPack.getPacket());
     */

    private String packet = "";
    private String header = "";

    public int getLen()
    {
        return this.packet.length();
    }

    public void setPacket(String pack)
    {
        this.packet = pack;
    }

    public void removeHeader()
    {
        if (packet.startsWith("<")) {
            int endArrow = packet.indexOf(">") + 1;
            int endSlash = packet.indexOf("/>") + 2;
            if (endSlash < endArrow) {
                this.packet = packet.substring(endSlash);
            } else {
                this.packet = packet.substring(endArrow);
            }
        } else if (packet.startsWith("%")) {
            String packet_handled[] = packet.split("%");
            this.packet = packet.substring(packet_handled[1].length()+packet_handled[2].length()+packet_handled[3].length()+3);
        }
    }

    public void addXML(String field, String value, int part)
    {
        switch (part) {
            case 0:
                this.packet += "<"+field+">"+value+"</"+field+">";
                break;
            case 1:
                this.packet += "<"+field+">";
                break;
            case 2:
                this.packet += "</"+field+">";
                break;
        }
    }

    public String getXML(String field)
    {
        int startPos = this.packet.indexOf("<"+field+">") + field.length() + 2;
        int endPos = this.packet.indexOf("</"+field+">", startPos);
        return this.packet.substring(startPos,endPos);
    }

    public void addXMLSingle(int part, String ... data)
    {
        int i = 0;
        if (part == 2) {
            this.packet += "</";
        } else {
            this.packet += "<";
        }
        for (String str:data) {
            if (i % 2 == 0) {
                if (i != 0) {
                    this.packet += " ";
                }
                this.packet += str;
            } else {
                this.packet += "='"+str+"'";
            }
            i++;
        }
        if (part == 0) {
            this.packet += " />";
        } else {
            this.packet += ">";
        }
    }

    public String getXMLSingle(String field)
    {
        int startPos = this.packet.indexOf(field+"='") + field.length() + 2;
        int endPos = this.packet.indexOf("'", startPos);
        return this.packet.substring(startPos,endPos);
    }

    public void addCDATA(String string)
    {
        this.packet += "<![CDATA[" + string + "]]>";
    }

    public String getCDATA(String string)
    {
        String toReturn;

        int startPos = string.indexOf("CDATA[") + 6;
        int endPos = string.indexOf("]", startPos);

        toReturn = string.substring(startPos,endPos);

        if(string.contains("N5O8S4M4J7N4B3B6K7")){
            return toReturn.split("~")[1];
        }

        return toReturn;
    }

    public void addString(String string)
    {
        this.packet += string;
    }

    public String getString(int start, int end, boolean nulled)
    {
        String thestring = this.packet.substring(start, end);
        this.packet = this.packet.substring(end);
        if(nulled)
            return thestring;
        else
            return removenullbyte(thestring);

    }

    protected String removenullbyte(String thestring)
    {
        byte[] stringbyte = thestring.getBytes();
        int a = 0;
        while(stringbyte[a] != 0x00)
            a++;

        return thestring.substring(0, a);
    }

    public String getasByte(int var, int num)
    {
        if(num == 1){
            int b1 = var & 0xff;
            byte[] varbyte = {(byte)b1};
            return new String(varbyte);
        } else if(num == 2){
            int b1 = var & 0xff;
            int b2 = (var >> 8) & 0xff;
            byte[] varbyte = {(byte)b1,(byte)b2};
            return new String(varbyte);
        } else if(num == 4){
            int b1 = var & 0xff;
            int b2 = (var >> 8) & 0xff;
            int b3 = (var >> 16) & 0xff;
            int b4 = (var >> 24) & 0xff;
            byte[] varbyte = {(byte)b1,(byte)b2,(byte)b3,(byte)b4};
            return new String(varbyte);
        }
        return null;
    }

    public void addInt(int var)
    {
        this.packet += ""+var;
    }
    
    public int getInt(int bytec)
    {
        String thestring = this.packet.substring(0, bytec);
        String hex_data_s = "";
        for(int i = bytec-1; i >=0; i--){

        int data = thestring.getBytes()[i];
        if(data<0){
            data += 256;
        }
        String hex_data = Integer.toHexString(data);
        if (hex_data.length()==1)
        {
            hex_data_s += "0"+hex_data;
        } else {
            hex_data_s += hex_data;
        }
        }

        this.packet = this.packet.substring(bytec);

        return Integer.parseInt(hex_data_s,16);
    }

    public void addByte(byte b1)
    {
        try
        {
        byte[] packbyte = {b1};
        this.packet += new String(packbyte,"ISO8859-1");
        }
        catch (Exception e) {
        }
    }

    public void addByte2(byte b1, byte b2)
    {
        byte[] packbyte = {b1, b2};
        this.packet += new String(packbyte);
    }

    public void addByte4(byte b1, byte b2, byte b3, byte b4)
    {
        byte[] packbyte = {b1, b2, b3, b4};
        this.packet += new String(packbyte);
    }

    public void addByteArray(byte[] bytearr)
    {
        try
        {
        this.packet += new String(bytearr,"ISO8859-1");
        }
        catch (Exception e) {
        }
    }
    
    public String getPacket()
    {
        return this.packet;
    }

    public void clean() {
        this.header = "";
        this.packet = "";
    }
}
