package com.terky.g_browser;

import org.junit.Test;

import static org.junit.Assert.*;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public  void getConnect() throws ClassNotFoundException {
        Connection connection = null;

        try{
            String driver = "com.mysql.jdbc.Driver";
            Class.forName(driver);

            String url = "jdbc:mysql://101.132.222.83:3306/CurriDesign?characterEncoding=utf-8";
            String user = "root";
            String password = "woshishabi";

            connection = DriverManager.getConnection(url, user, password);
            Log.e("getConnect","success");
        }catch (ClassNotFoundException e){
            Log.e("getConnect", e.getMessage(), e);
            e.printStackTrace();
        }catch (SQLException ex){
            Log.e("getConnect", ex.getMessage(), ex);
            ex.printStackTrace();
        }
    }
}