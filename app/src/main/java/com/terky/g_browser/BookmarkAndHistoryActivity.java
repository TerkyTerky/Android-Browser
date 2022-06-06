package com.terky.g_browser;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.terky.g_browser.utils.ItemLongClickPopWindow;

public class BookmarkAndHistoryActivity extends AppCompatActivity {
    private static final String DEG_TAG = "webbrowser_FavAndHisActivity";

    //收藏历史按钮
    private TextView favorites;
    private TextView history;

    //收藏历史的内容
    private ListView favoritesContent;
    private ListView historyContent;

    //popupwindow弹窗
    private ItemLongClickPopWindow itemLongClickedPopWindow;

    //书签管理
    private BookmarkManager bookmarkManager;

    //Cursor
    private Cursor favoritesCursor;

    //Adapter
    private ListAdapter favorietesAdapter;

    //监听
    private ButtonClickedListener buttonClickedListener;
    private ListViewOnItemLongListener itemLongListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_favoritesandhistory);

        //初始化
        this.favorites = (TextView) this.findViewById(R.id.favorites);
        this.history = (TextView) this.findViewById(R.id.history);

        this.favoritesContent = (ListView) this.findViewById(R.id.favoritesAndHisotry_content_favorite);
        this.historyContent = (ListView) this.findViewById(R.id.favoritesAndHisotry_content_history);

        this.itemLongListener = new ListViewOnItemLongListener();

        //添加监听
        this.favorites.setOnClickListener((View.OnClickListener) this.buttonClickedListener);
        this.history.setOnClickListener((View.OnClickListener) this.buttonClickedListener);

        this.favoritesContent.setOnItemLongClickListener((AdapterView.OnItemLongClickListener) this.itemLongListener);

        //初始化数据
        this.initData();
    }

    /**
     * 初始化ListView中的数据
     * */
    @SuppressWarnings("deprecation")
    private void initData() {
        //获取书签管理
        this.bookmarkManager = new BookmarkManager(this);
        this.favoritesCursor = this.bookmarkManager.getAllFavorites();
        this.favorietesAdapter = new SimpleCursorAdapter(getApplicationContext(),
                R.layout.list_item, this.favoritesCursor,
                new String[]{"_id","name","url"},
                new int[]{R.id.item_id, R.id.item_name,R.id.item_url});
        this.favoritesContent.setAdapter(this.favorietesAdapter);
    }

    /**
     * 长按单项事件
     * 覆盖如下方法
     * 1.	onItemLongClick
     * */
    private class ListViewOnItemLongListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                       int position, long id) {
            Log.d(DEG_TAG, "long item cliced");
            if(parent.getId()==R.id.favoritesAndHisotry_content_favorite){
                itemLongClickedPopWindow = new ItemLongClickPopWindow(BookmarkAndHistoryActivity.this, 200, 200);
                itemLongClickedPopWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.favandhis_activity));
                itemLongClickedPopWindow.showAsDropDown(view, view.getWidth()/2, -view.getHeight()/2);
                TextView modifyFavorite = (TextView) itemLongClickedPopWindow.getView(R.id.item_longclicked_modifyFavorites);
                TextView deleteFavorite = (TextView) itemLongClickedPopWindow.getView(R.id.item_longclicked_deleteFavorites);
                ItemClickedListener itemClickedListener = new ItemClickedListener(view);
                modifyFavorite.setOnClickListener((View.OnClickListener) itemClickedListener);
                deleteFavorite.setOnClickListener((View.OnClickListener) itemClickedListener);
            }else if(parent.getId()==R.id.favoritesAndHisotry_content_history){

            }
            return false;
        }

    }

    private class ButtonClickedListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if(v.getId()==R.id.favorites){

            }else if(v.getId()==R.id.history){

            }
        }

    }


    private class ItemClickedListener implements View.OnClickListener {

        private String item_id;
        private String item_name;
        private String item_url;

        public ItemClickedListener(View item){
            this.item_id = ((TextView) item.findViewById(R.id.item_id)).getText().toString();
            this.item_name = ((TextView) item.findViewById(R.id.item_name)).getText().toString();
            this.item_url = ((TextView) item.findViewById(R.id.item_url)).getText().toString();
        }

        @Override
        public void onClick(View view) {
            //取消弹窗
            itemLongClickedPopWindow.dismiss();
            if(view.getId()==R.id.item_longclicked_modifyFavorites){
                //弹出修改窗口
                LayoutInflater modifyFavoritesInflater = LayoutInflater.from(BookmarkAndHistoryActivity.this);
                View modifyFavoritesView = modifyFavoritesInflater.inflate(R.layout.dialog_modify, null);
                final TextView item_name_input = (TextView) modifyFavoritesView.findViewById(R.id.dialog_name_input);
                final TextView item_url_input = (TextView) modifyFavoritesView.findViewById(R.id.dialog_url_input);
                item_name_input.setText(item_name);
                item_url_input.setText(item_url);
                new AlertDialog.Builder(BookmarkAndHistoryActivity.this)
                        .setTitle("编辑书签")
                        .setView(modifyFavoritesView)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(DEG_TAG, "id:"+item_id+",name:"+item_name+",url:"+item_url);
                                if(bookmarkManager.modifyFavorite(item_id, item_name_input.getText().toString(),
                                        item_url_input.getText().toString())){
                                    Toast.makeText(BookmarkAndHistoryActivity.this, "修改成功", Toast.LENGTH_LONG).show();
                                    initData();
                                    favoritesContent.invalidate();
                                }else{
                                    Toast.makeText(BookmarkAndHistoryActivity.this, "修改失败", Toast.LENGTH_LONG).show();
                                }
                            }

                        }).setNegativeButton("取消", null)
                        .create()
                        .show();
            }else if(view.getId()==R.id.item_longclicked_deleteFavorites){
                new AlertDialog.Builder(BookmarkAndHistoryActivity.this)
                        .setTitle("删除书签")
                        .setMessage("是否要删除\""+item_name+"\"这个书签？")
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(bookmarkManager.deleteFavorite(item_id)){
                                    //删除成功
                                    Toast.makeText(BookmarkAndHistoryActivity.this, "删除成功", Toast.LENGTH_LONG).show();
                                    initData();
                                    favoritesContent.invalidate();
                                }else{
                                    Toast.makeText(BookmarkAndHistoryActivity.this, "删除失败", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create()
                        .show();
            }

        }

    }

    @Override
    protected void onDestroy() {
        if (this.favoritesCursor != null) {
            this.favoritesCursor.close();
        }
        super.onDestroy();
    }


}
