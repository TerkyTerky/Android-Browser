package com.terky.g_browser.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.HashMap;

public class GHelper extends SQLiteOpenHelper implements IDataBase {

    private static final String DEG_TAG ="webBrowser_GHelper";

    public GHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQLStr.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    @Override
    public boolean addFavorite(SQLiteDatabase sqLiteDatabase, String name, String url) throws SQLException {
        ContentValues favorite = new ContentValues();
        favorite.put("name", name);
        favorite.put("url", url);
        long id = sqLiteDatabase.insert("favorite", null, favorite);
        return id != -1;
    }

    @Override
    public boolean deleteFavorite(SQLiteDatabase sqLiteDatabase, String id) {
        Log.d(DEG_TAG, "deleteId:"+id);
        int number = sqLiteDatabase.delete("favorite", "id=?", new String[]{id});
        Log.d(DEG_TAG, "delete_result:"+number);
        return number != 0;
    }

    @Override
    public boolean modifyFavorite(SQLiteDatabase sqLiteDatabase, String id, String name, String url) {
        ContentValues favorite = new ContentValues();
        favorite.put("name", name);
        favorite.put("url", url);
        Log.d(DEG_TAG, "id:"+id+",name:"+name+",url:"+url);
        int number = sqLiteDatabase.update("favorite", favorite, "id=?", new String[]{id});
        Log.d(DEG_TAG, "number:"+number);
        return number != 0;
    }

    @Override
    @SuppressLint("Range")
    public Cursor getAllFavorites(SQLiteDatabase sqLiteDatabase) {
        String[] returnColmuns = new String[]{
                "id as _id",
                "name",
                "url"
        };
        Cursor result = sqLiteDatabase.query("favorite", returnColmuns, null, null, null, null, "id");
        while(result.moveToNext()){
            String id = String.valueOf(result.getInt(result.getColumnIndex("_id")));
            String name = result.getString(result.getColumnIndex("name"));
            String url = result.getString(result.getColumnIndex("url"));
            Log.d(DEG_TAG, "id:"+id+",name:"+name+",url:"+url);
        }
        return result;
    }

    @Override
    @SuppressLint("Range")
    public boolean multiplyFavorite(SQLiteDatabase sqLiteDatabase, String url) {
        Cursor result = sqLiteDatabase.query("favorite", null, "url=?", new String[]{url}, null, null, null);
        while(result.moveToNext()){
            Log.d(DEG_TAG, "multiply:[id:"+String.valueOf(result.getInt(result.getColumnIndex("id"))
                    +",name:"+result.getString(result.getColumnIndex("name"))
                    +",url:"+result.getString(result.getColumnIndex("url"))));
        }
        if(result.getCount()>0){
            result.close();
            return true;
        }else{
            result.close();
            return false;
        }
    }

    @Override
    public void transactionAround(boolean readOnly, CallBack callback) {
        SQLiteDatabase sqLiteDatabase = null;
        if(readOnly){
            sqLiteDatabase = this.getReadableDatabase();
        }else{
            sqLiteDatabase = this.getWritableDatabase();
        }
        sqLiteDatabase.beginTransaction();
        callback.doSomething(sqLiteDatabase);
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
    }
}