package com.terky.g_browser.utils;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MysqlUtil {


    public static Connection getConnect() throws ClassNotFoundException {
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
        return connection;
    }

    public static void close(Statement state, Connection connect){
        if (state != null){
            try {
                state.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connect != null){
            try {
                connect.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public static void reClose(ResultSet rs, Statement state, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (state != null) {
            try {
                state.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}


