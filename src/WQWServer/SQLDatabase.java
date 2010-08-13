package WQWServer;


import java.io.*;
import java.util.*;
import java.sql.*;

/**
 * @author XVII
 * Interacts with the SQL Database
 */

public class SQLDatabase {

    protected String owner;
    protected Properties sqldata = new Properties();
    protected Connection con;
    protected Statement st;
    protected String ip, port, user, pass, database;


    public SQLDatabase(String createdby) {
        this.owner = createdby;
    }


    protected void debug(String msg) {
        Main.debug("["+owner+"]", msg);
    }

     /**
      * Loads the configs out of "configs/mysql.conf"
      */
    private void loadconfigs()
    {
        try
        {
        FileInputStream fin = new FileInputStream("configs/mysql.conf");
        sqldata.load(fin);
        fin.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        ip = sqldata.getProperty("MySQL_ip");
        port = sqldata.getProperty("MySQL_port");
        user = sqldata.getProperty("MySQL_id");
        pass = sqldata.getProperty("MySQL_pw");
        database = sqldata.getProperty("MySQL_db");
    }

    public void start()
    {
        loadconfigs();

         /**
          * Sets up the basic connection
          */
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://"+ip+":"+port+"/"+database, user,pass);
            st = con.createStatement();
            if (!con.equals(null))
                debug("SQL Connection started successfully");
        }
        catch (Exception ex)
        {
            debug("Exception in SQL Database: " + ex.getMessage());
        }
    }

    public ResultSet doquery(String query)
    {
        ResultSet rs = null;
        try
        {
            rs = st.executeQuery(query);
        }
        catch (Exception ex)
        {
            debug("Exception in do query: " + ex.getMessage());
        }
        return rs;
    }

    public void doupdate(String query)
    {
        try
        {
            st.executeUpdate(query);
        }
        catch (Exception ex)
        {
            debug("Exception in do update: " + ex.getMessage());
        }
    }


}
