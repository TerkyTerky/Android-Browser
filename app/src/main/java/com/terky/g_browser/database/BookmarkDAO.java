package com.terky.g_browser.database;

import android.util.Log;

import com.terky.g_browser.entity.Bookmark;
import com.terky.g_browser.utils.MysqlUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookmarkDAO {
    //新增
    public static void add(Bookmark bookmark) throws ClassNotFoundException {
        final String sql = "insert into Bookmark(webname,url)values('" + bookmark.getWebName() + "','" + bookmark.getUri() + "')";
        new Thread(new Runnable() {
            @Override
            public void run() {
                Connection conn = null;
                try {
                    conn = MysqlUtil.getConnect();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                Statement state = null;
                boolean f = false;
                int a = 0;
                try {
                    state = conn.createStatement();
                    a = state.executeUpdate(sql);
                } catch (Exception e) {
                    Log.e("add->", e.getMessage(), e);
                    e.printStackTrace();
                } finally {
                    MysqlUtil.close(state, conn);
                }
                if (a > 0) {
                    f = true;
                }
            }
        }).start();

    }

    //删除
    public static boolean delete(Bookmark bookmark) throws ClassNotFoundException {
        String sql = "delete from user where id=" + bookmark.getWebName();
        Connection conn = MysqlUtil.getConnect();
        Statement state = null;
        boolean f = false;
        int a = 0;
        try {
            state = conn.createStatement();
            a = state.executeUpdate(sql);
        } catch (Exception e) {
            Log.e("delete->", e.getMessage(), e);
            e.printStackTrace();
        } finally {
            MysqlUtil.close(state, conn);
        }
        if (a > 0) {
            f = true;
        }
        return f;
    }

    //修改
    public static boolean update(Bookmark bookmark) throws ClassNotFoundException {

        String sql = "update Bookmark set "
                   + "webname='" + bookmark.getWebName()
                   + "', url='" + bookmark.getUri()
                   + "' where webname='" + bookmark.getWebName() + "'";
        Connection conn = MysqlUtil.getConnect();
        Statement state = null;
        boolean f = false;
        int a = 0;
        try {
            state = conn.createStatement();
            a = state.executeUpdate(sql);
        } catch (Exception e) {
            Log.e("update->", e.getMessage(), e);
            e.printStackTrace();
        } finally {
            MysqlUtil.close(state, conn);
        }
        if (a > 0) {
            f = true;
        }
        return f;
    }

    //获取列表
    public static List<Bookmark> getList(String id) throws ClassNotFoundException {

        //结果存放集合
        List<Bookmark> list = new ArrayList<>();
        //MySQL 语句
        String sql = "select * from user where id =" + id;
        Connection conn = MysqlUtil.getConnect();
        Statement state = null;
        ResultSet rs = null;
        boolean f = false;
        int a = 0;
        try {
            state = conn.createStatement();
            rs = state.executeQuery(sql);
            while (rs.next()) {
                Bookmark bookmark = new Bookmark();
                bookmark.setWebName(rs.getString("webname"));
                bookmark.setUri(rs.getString("url"));
                list.add(bookmark);

            }
        } catch (Exception e) {
            Log.e("update->", e.getMessage(), e);
            e.printStackTrace();
        } finally {
            MysqlUtil.reClose(rs, state, conn);
        }
        if (a > 0) {
            f = true;
        }
        return list;
    }
}
