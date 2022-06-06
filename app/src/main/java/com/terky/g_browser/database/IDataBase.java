package com.terky.g_browser.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public interface IDataBase {
    boolean addFavorite(SQLiteDatabase sqLiteDatabase, String name, String url);
    boolean deleteFavorite(SQLiteDatabase sqLiteDatabase, String id);
    boolean modifyFavorite(SQLiteDatabase sqLiteDatabase, String id, String name, String url);
    Cursor getAllFavorites(SQLiteDatabase sqLiteDatabase);
    boolean multiplyFavorite(SQLiteDatabase sqLiteDatabase, String url);
    void transactionAround(boolean readOnly, CallBack callback);

}
