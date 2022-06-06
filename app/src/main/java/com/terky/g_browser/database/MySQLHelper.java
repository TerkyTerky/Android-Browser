package com.terky.g_browser.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class MySQLHelper {

    public static String getTitle(){
        final String CLS = "com.mysql.jdbc.Driver";
        final String URL = "jdbc:mysql://101.132.222.83:3306/android";
        final String user = "root";
        final String password = "woshishabi";
        String title = null;
        
        try{
            Class.forName(CLS);
            Connection connection = DriverManager.getConnection(URL,user,password);
            String sql = "select count(1) as s from android";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()){
                title = resultSet.getString("s");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return title;
    }
}
