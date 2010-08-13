package WQWServer;

/**
 * @author XVII
 * Misc Functions
 */

public class Functions {
    //Returns the string, stops at the first nullbyte it encounters
    protected String removenullbyte(String thestring)
    {
        byte[] stringbyte = thestring.getBytes();
        int a = 0;
        while(stringbyte[a] != 0x00){
            a++;
        }
        return thestring.substring(0, a);
    }

    //Converts a byte to an integer
    protected int bytetoint(String thestring, int bytec)
    {
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

        return Integer.parseInt(hex_data_s,16);
    }

    //Replaces a character at a position
    protected String replaceCharAt(String s, int pos, char c) {
        return s.substring(0,pos) + c + s.substring(pos+1);
    }
}

