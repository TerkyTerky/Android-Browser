package com.terky.g_browser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.terky.g_browser.database.BookmarkDAO;
import com.terky.g_browser.entity.Bookmark;
import com.terky.g_browser.utils.QrUtil;
import com.terky.g_browser.view.MarqueeTextView;
import com.terky.g_browser.view.GDialog;
import com.terky.g_browser.web.GWebView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements TextView.OnEditorActionListener {

    private static final int CODE_SCANNING = 0, CODE_SCAN_LOCAL_PICTURE = 1;
    private static final String BOOKMARK_URL = "url", BOOKMARK_TITLE = "title";
    private long lastBackPressedTime = 0;
    private boolean bRefresh = false;
    private String findText = "";
    private GWebView mWeb;
    private EditText etUrl;
    private MarqueeTextView tvUrl;
    private View lyTools;
    // Preferences data
    private static final String SHARED_PREFERENCES = "main.sp";
    private static final List<HashMap<String, String>> bookmarks = new ArrayList<>();
    private static final List<HashMap<String, String>> history = new ArrayList<>();
    private static final List<HashMap<String, String>> cloudBookmarks = new ArrayList<>();
    private String homeUrl;
    private boolean hideTools;

    //储存设置，书签等的相关标志
    static class DataFlag {
        static final String BOOKMARK = "bookmark:";
        static final String HISTORY = "history:";
        static final String SETTING = "setting:";
        static final String HOME = "home:";
        static final String Hide_Tools = "show_tools:";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        validatePermission();
        initData();

        // load web page
        if (mWeb.restoreState(savedInstanceState) != null) return;
        if (getIntent().getData() != null) {
            mWeb.loadUrl(getIntent().getData().toString());
            return;
        }
        homeUrl = "https://www.baidu.com/";
        mWeb.loadUrl(homeUrl);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mWeb.loadUrl(intent.getDataString());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mWeb.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWeb.restoreState(savedInstanceState);
    }

    /**
     * 退出前保存数据
     */
    @Override
    protected void onStop() {
        saveSharedPreferences();
        super.onStop();
    }

    /**
     * 检查权限
     */
    private void validatePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        String[] ps = {
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        for (String p : ps) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(ps, 3956);
                return;
            }
        }
    }

    /**
     * 初始化相关变量
     */
    private void initData() {
        //bind view
        lyTools = findViewById(R.id.ly_tool);
        mWeb = findViewById(R.id.wv_main);
        etUrl = findViewById(R.id.et_url);
        tvUrl = findViewById(R.id.tv_url);
        CheckBox cbJs = findViewById(R.id.cb_javascript);
        CheckBox cbImg = findViewById(R.id.cb_image);
        final ProgressBar pb = findViewById(R.id.pb);
        final Button btRefresh = findViewById(R.id.btn_refresh);

        //bind events
        mWeb.setFindListener(new WebView.FindListener() {
            @Override
            public void onFindResultReceived(int ordinal, int numMatches,
                                             boolean b) {
                TextView tv = findViewById(R.id.tv_find_num);
                ordinal = numMatches == 0 ? 0 : ordinal + 1;
                String s = ordinal + "/" + numMatches;
                tv.setText(s);
            }
        });
        mWeb.setStateListener(new GWebView.StateListener() {
            @Override
            public void onStateChanged(WebView v, int stateType, Object... args) {
                switch (stateType) {
                    case STATE_STARTED:
                        bRefresh = false;
                        btRefresh.setBackgroundResource(R.drawable.ic_cancel);
                        pb.setVisibility(View.VISIBLE);
                        etUrl.setText((String) args[0]);
                        if (mWeb.getIsDarkMode()) {
                            mWeb.loadDark();
                        }
                        break;
                    case STATE_FINISHED:
                        bRefresh = true;
                        btRefresh.setBackgroundResource(R.drawable.ic_refresh);
                        pb.setVisibility(View.GONE);
                        break;
                    case STATE_PROGRESS:
                        pb.setProgress((int) args[0]);
                        if (mWeb.getIsDarkMode()) {
                            mWeb.loadDark();
                        }
                        break;
                    case STATE_FIND_START:
                        findText = args[0].toString();
                        mWeb.findAllAsync(findText);
                        findViewById(R.id.ly_find).setVisibility(View.VISIBLE);
                        ((EditText) findViewById(R.id.et_find)).setText(findText);
                        break;
                    case STATE_RECEIVED_TITLE:
                        tvUrl.setText(args[0].toString());
                        tvUrl.setVisibility(View.VISIBLE);
                        etUrl.setVisibility(View.GONE);
                        break;
                    case STATE_RECEIVED_ICON:
                        Bitmap bmp = (Bitmap) args[0];
                        BitmapDrawable bd = new BitmapDrawable(getResources(), bmp);
                        int offset = 2;
                        int end = ((View) tvUrl.getParent()).getHeight();
                        bd.setBounds(offset, offset, end, end);
                        tvUrl.setCompoundDrawables(bd, null, null, null);
                        tvUrl.setVisibility(View.VISIBLE);
                        etUrl.setVisibility(View.GONE);
                        break;
                }
            }
        });
        CompoundButton.OnCheckedChangeListener listener
                = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                int id = v.getId();
                if (id == R.id.cb_javascript) {
                    mWeb.getSettings().setJavaScriptEnabled(isChecked);
                } else if (R.id.cb_image == id) {
                    mWeb.getSettings().setBlockNetworkImage(!isChecked);
                }
            }
        };
        cbJs.setOnCheckedChangeListener(listener);
        cbImg.setOnCheckedChangeListener(listener);
        etUrl.setOnEditorActionListener(this);

        // init data
        readSharedPreferences();
        cbJs.setChecked(mWeb.getSettings().getJavaScriptEnabled());
        cbImg.setChecked(!mWeb.getSettings().getBlockNetworkImage());
        if (hideTools) {
            lyTools.setVisibility(View.GONE);
            findViewById(R.id.btn_show_toolbar).setBackgroundResource(
                    R.drawable.ic_unfold_more);
        }
    }

    /**
     * 保存未知类型数据
     *
     * @param editor The SharedPreferences.edit
     * @param key    Data key
     * @param value  Data value
     */
    private void putObj2Pref(SharedPreferences.Editor editor, String key,
                             Object value) {
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (int) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (long) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (float) value);
        }
    }

    /**
     * 保存设置
     */
    private void saveSharedPreferences() {
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFERENCES,
                0).edit();
        editor.clear();
        // Save WebView settings
        for (String opSetting : GWebView.OPTIONS) {
            Object result = mWeb.invokeSettingsMethod(
                    "get" + opSetting, false, null);
            putObj2Pref(editor, DataFlag.SETTING + opSetting, result);
        }
        for (String opt : GWebView.OPTIONS_EXTRA) {
            Object result = mWeb.invokeSettingsMethod(
                    "get" + opt, false, null);
            putObj2Pref(editor, DataFlag.SETTING + opt, result);
        }
        // Save bookmarks
        for (HashMap<String, String> item : bookmarks) {
            editor.putString(DataFlag.BOOKMARK +
                            item.get(BOOKMARK_URL), item.get(BOOKMARK_TITLE));
        }
        // Save history
        for (HashMap<String,String> item : history) {
            editor.putString(DataFlag.HISTORY +
                            item.get(BOOKMARK_URL), item.get(BOOKMARK_TITLE));
        }
        // Save homeUrl
        editor.putString(DataFlag.HOME, homeUrl);
        // Save tools visibility
        editor.putBoolean(DataFlag.Hide_Tools, hideTools);
        editor.apply();
    }

    /**
     * 读取设置
     */
    private void readSharedPreferences() {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, 0);
        for (Map.Entry<String, ?> item : preferences.getAll().entrySet()) {
            if (item.getKey().startsWith(DataFlag.SETTING)) {
                // WebView settings
                String opSetting =
                        item.getKey().substring(DataFlag.SETTING.length());
                mWeb.invokeSettingsMethod(
                        "set" + opSetting, true, item.getValue());
            } else if (item.getKey().startsWith(DataFlag.BOOKMARK)) {
                // Bookmarks
                HashMap<String, String> bm = new HashMap<>();
                bm.put(BOOKMARK_URL,
                        item.getKey().substring(DataFlag.BOOKMARK.length()));
                bm.put(BOOKMARK_TITLE, (String) item.getValue());
                bookmarks.add(bm);
            } else if (item.getKey().startsWith(DataFlag.HISTORY)){
                // History
                HashMap<String,String> histRecord = new HashMap<>();
                histRecord.put(BOOKMARK_URL, item.getKey());
                histRecord.put(BOOKMARK_TITLE, (String) item.getValue());
                history.add(histRecord);
            }
            else if (item.getKey().startsWith(DataFlag.HOME)) {
                // Home url
                homeUrl = (String) item.getValue();
            } else if (item.getKey().startsWith(DataFlag.Hide_Tools)) {
                // Show tools
                hideTools = (boolean) item.getValue();
            }
        }
    }

    /**
     * 显示消息
     *
     * @param msg 要显示的消息
     */
    private void showMessage(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 编辑动作事件，这里主要处理编辑URL后回车事件
     *
     * @param v        编辑控件
     * @param actionId ...
     * @param event    ...
     * @return 是否已处理，若为否，则系统继续处理
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // 只处理etUrl编辑事件
        if (v.getId() != R.id.et_url) return false;
        String url = v.getText().toString();
        String urlL = url.toLowerCase();
        if (urlL.trim().equals("")) return true;
        if (!urlL.startsWith("http://") && !urlL.startsWith("https://")
                && !urlL.startsWith("file:///")) {
            url = "https://" + url;
        }
        mWeb.loadUrl(url);
        mWeb.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        // 隐藏软键盘
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
        return false;
    }

    /**
     * 显示设置对话框
     */
    private void showSettings() {
        final View dialogView = LayoutInflater.from(this).inflate(
                R.layout.layout_settings, null);
        //添加并初始化settings选项
        LinearLayout ly = dialogView.findViewById(R.id.ly_boolean_settings);
        for (final String settingsName : GWebView.OPTIONS) {
            CheckBox cb = new CheckBox(dialogView.getContext());
            cb.setText(settingsName);
            String methodName = "get" + settingsName;
            boolean bl = (boolean) mWeb.invokeSettingsMethod(
                    methodName, false, null);
            cb.setChecked(bl);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    mWeb.invokeSettingsMethod("set" + settingsName,
                            true, isChecked);
                }
            });
            ly.addView(cb);
        }
        //textZoom
        final EditText etTextZoom = dialogView.findViewById(R.id.et_text_zoom);
        etTextZoom.setText(String.valueOf(mWeb.getSettings().getTextZoom()));
        //生成对话框
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView).create();
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.btn_settings_clear_cache) {
                    mWeb.clearCache(true);
                    showMessage("清理缓存完毕。");
                } else if (id == R.id.btn_settings_load_default) {
                    mWeb.loadDefaultSettings();
                    etTextZoom.setText(String.valueOf(100));
                    //还原主页
                    homeUrl = "about:blank";
                    dialog.dismiss();
                    showSettings();
                } else if (id == R.id.btn_Settings_ok) {
                    dialog.dismiss();
                }
            }
        };
        dialogView.findViewById(R.id.btn_settings_clear_cache).setOnClickListener(listener);
        dialogView.findViewById(R.id.btn_settings_load_default).setOnClickListener(listener);
        dialogView.findViewById(R.id.btn_Settings_ok).setOnClickListener(listener);
        // 运用设置
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                int z;
                try {
                    z = Integer.parseInt(etTextZoom.getText().toString());
                } catch (Exception e) {
                    z = 0;
                }
                if (z >= 10 && z <= 1000) {
                    mWeb.getSettings().setTextZoom(z);
                }
            }
        });
        dialog.show();
        // 设置最大高度
        dialogView.addOnLayoutChangeListener(
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        int h = getResources().getDisplayMetrics().heightPixels;
                        h = Math.min(h * 2 / 3, v.getHeight());
                        dialogView.getLayoutParams().height = h;
                    }
                });
    }

    /**
     * 显示书签列表
     */
    private void showBookmarks() {
        // 书签列表
        // 读取列表
        final SimpleAdapter listAdapter = new SimpleAdapter(
                                                    getApplicationContext(),
                                                    bookmarks, R.layout.layout_bookmark_item,
                                                    new String[]{BOOKMARK_TITLE, BOOKMARK_URL},
                                                    new int[]{R.id.tv_bm_title, R.id.tv_bm_url});
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.bookmarks, null);
        ListView lv = dialogView.findViewById(R.id.lv_bookmarks);
        lv.setAdapter(listAdapter);
        // 创建书签面板
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView).create();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position
                    , long id) {
                mWeb.loadUrl(bookmarks.get(position).get(BOOKMARK_URL));
                dialog.dismiss();
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent,
                                           final View view,
                                           final int position, long id) {
                final PopupMenu menu = new PopupMenu(getApplicationContext(), view);
                menu.getMenu().add("delete");
                menu.getMenu().add("cancel");
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getTitle().equals("delete")) {
                            bookmarks.remove(position);
                            listAdapter.notifyDataSetChanged();
                        }
                        return true;
                    }
                });
                menu.show();
                return true;
            }
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (v.getId() == R.id.btn_bookmarks_clear) {
                    bookmarks.clear();
                    listAdapter.notifyDataSetChanged();
                }
            }
        };
        dialogView.findViewById(R.id.btn_bookmarks_clear).setOnClickListener(listener);
        dialogView.findViewById(R.id.btn_bookmarks_ok).setOnClickListener(listener);
        dialog.show();
        // 设置最大高度
        dialogView.addOnLayoutChangeListener(
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        int h = getResources().getDisplayMetrics().heightPixels;
                        h = Math.min(h * 2 / 3, v.getHeight());
                        dialogView.getLayoutParams().height = h;
                    }
                });
    }

    /*显示历史记录*/
    public void showHistory(){
        final SimpleAdapter listAdapter = new SimpleAdapter(
                getApplicationContext(),
                history, R.layout.layout_history_item,
                new String[]{BOOKMARK_TITLE, BOOKMARK_URL},
                new int[]{R.id.tv_history_title, R.id.tv_history_url});
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.history, null);
        ListView lv = dialogView.findViewById(R.id.lv_history);
        lv.setAdapter(listAdapter);
        // 创建历史面板
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView).create();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position
                    , long id) {
                mWeb.loadUrl(history.get(position).get(BOOKMARK_URL));
                dialog.dismiss();
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent,
                                           final View view,
                                           final int position, long id) {
                final PopupMenu menu = new PopupMenu(getApplicationContext(), view);
                menu.getMenu().add("delete");
                menu.getMenu().add("cancel");
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getTitle().equals("delete")) {
                            history.remove(position);
                            listAdapter.notifyDataSetChanged();
                        }
                        return true;
                    }
                });
                menu.show();
                return true;
            }
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (v.getId() == R.id.btn_history_clear) {
                    history.clear();
                    listAdapter.notifyDataSetChanged();
                }
            }
        };
        dialogView.findViewById(R.id.btn_history_clear).setOnClickListener(listener);
        dialogView.findViewById(R.id.btn_history_ok).setOnClickListener(listener);
        dialog.show();

        // 设置最大高度
        dialogView.addOnLayoutChangeListener(
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        int h = getResources().getDisplayMetrics().heightPixels;
                        h = Math.min(h * 2 / 3, v.getHeight());
                        dialogView.getLayoutParams().height = h;
                    }
                });

    }

    /**
     * 显示其他设置项
     *
     * @param v “otherButton"对象
     */
    private void showOtherSetting(View v) {
        final PopupMenu menu = new PopupMenu(this, v);
        menu.getMenuInflater().inflate(R.menu.menu_other, menu.getMenu());
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_other_dark_mode) {
                    boolean isDark = !mWeb.getIsDarkMode();
                    mWeb.setIsDarkMode(isDark);
                    View v = findViewById(R.id.ly_main);
                    int bc = isDark ?
                            getResources().getColor(R.color.dark_mode_bg) :
                            getResources().getColor(R.color.color_bg_stroke);
                    int fc = isDark ?
                            getResources().getColor(R.color.dark_mode_for) :
                            getResources().getColor(R.color.dark);
                    etUrl.setTextColor(fc);
                    tvUrl.setTextColor(fc);
                    v.setBackgroundColor(bc);
                    if (!mWeb.getSettings().getJavaScriptEnabled())
                        showMessage(getResources().getString(R.string.need_js));
                    mWeb.reload();
                } else if (id == R.id.menu_other_edit_home) {
                    setHomePage();
                } else if (id == R.id.menu_other_download_manager) {
                    Intent it = new Intent();
                    it.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
                    startActivity(it);
                } else if (id == R.id.menu_other_settings) {
                    showSettings();
                } else if (id == R.id.menu_other_about) {
                    TextView tv = new TextView(MainActivity.this);
                    tv.setTextIsSelectable(true);
                    tv.setAutoLinkMask(Linkify.WEB_URLS);
                    int padding = 4;
                    tv.setPadding(padding, padding, padding, padding);
                    tv.setText(R.string.about_content);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.about)
                            .setView(tv)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else if (id == R.id.menu_other_exit) {
                    finish();
                }
                return false;
            }
        });
        menu.show();
    }

    /**
     * 设置主页
     */
    private void setHomePage() {
        final EditText et = new EditText(this);
        et.setText(mWeb.getUrl());
        new AlertDialog.Builder(this).setTitle("Set Home Page")
                .setView(et)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        homeUrl = et.getText().toString();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void showScanningDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        Button btnCamera = new Button(this);
        Button btnPicture = new Button(this);
        btnCamera.setText(R.string.scan_with_camera);
        btnPicture.setText(R.string.scan_local_picture);
        btnCamera.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        Intent it = new Intent(MainActivity.this,
                                ScanningActivity.class);
                        startActivityForResult(it, CODE_SCANNING);
                    }
                });
        btnPicture.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        Intent it = new Intent();
                        it.setAction(Intent.ACTION_GET_CONTENT);
                        it.setType("image/*");
                        startActivityForResult(it, CODE_SCAN_LOCAL_PICTURE);
                    }
                });
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(btnCamera);
        ll.addView(btnPicture);
        dialog.setView(ll);
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        String s = null;
        if (requestCode == CODE_SCANNING) {
            s = data.getStringExtra("msg");
        } else if (requestCode == CODE_SCAN_LOCAL_PICTURE
                && data.getData() != null) {
            try {
                s = QrUtil.decodeQRCode(BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(data.getData())));
            } catch (Exception e) {
                s = e.toString();
            }
        }
        handleScanningResult(s);
    }

    private void handleScanningResult(String result) {
        if (result == null) {
            result = "scanning result:\n\nnull";
        } else {
            result = "scanning result:\n\n" + result;
        }
        GDialog.showSelectableDialog(this, result);
    }

    // had invoked by style.buttonMainUIButton
    public void onMainUiClick(View v) throws ClassNotFoundException {

        int id = v.getId();

        Bookmark bk = new Bookmark();

        if (id == R.id.btn_add_bookmark) {
            //以url为键，因为title可能会有重复
            HashMap<String, String> bm = new HashMap<>();
            bm.put(BOOKMARK_TITLE, mWeb.getTitle());
            bm.put(BOOKMARK_URL, mWeb.getUrl());
            System.out.println(mWeb.getUrl());
            bk.setWebName(mWeb.getTitle());
            bk.setUri(mWeb.getUrl());

            BookmarkDAO.add(bk);
            bookmarks.add(bm);

            HashMap<String,String> ht = new HashMap<>();
            ht.put(BOOKMARK_TITLE, mWeb.getTitle());
            ht.put(BOOKMARK_URL, mWeb.getUrl());

            history.add(ht);
            showMessage("添加书签成功。");
        } else if (id == R.id.btn_show_toolbar) {
            int visibility = hideTools ? View.VISIBLE : View.GONE;
            int bkResource = hideTools ? R.drawable.ic_unfold_less :
                    R.drawable.ic_unfold_more;
            lyTools.setVisibility(visibility);
            v.setBackgroundResource(bkResource);
            hideTools = !hideTools;
        } else if (id == R.id.btn_home) {
            mWeb.loadUrl(homeUrl);
        } else if (id == R.id.btn_back) {
            if (mWeb.canGoBack()) {
                mWeb.goBack();
            }
        } else if (id == R.id.btn_forward) {
            if (mWeb.canGoForward()) {
                mWeb.goForward();
            }
        } else if (id == R.id.btn_refresh) {
            if (bRefresh) {
                mWeb.reload();
            } else {
                mWeb.stopLoading();
            }
        } else if (id == R.id.btn_scanning) {
            showScanningDialog();
        } else if (id == R.id.btn_bookmark) {
            showBookmarks();
        } else if (id == R.id.btn_history){
            showHistory();
        }
        else if (id == R.id.btn_other) {
            showOtherSetting(v);
        } else if (id == R.id.tv_url) {
            v.setVisibility(View.GONE);
            etUrl.setVisibility(View.VISIBLE);
        }
    }

    public void onFindClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_find_next ||
                id == R.id.btn_find_prev) {
            String s = ((EditText) findViewById(R.id.et_find)).getText().toString();
            if (s.equals(findText)) {
                mWeb.findNext(id == R.id.btn_find_next);
            } else {
                findText = s;
                mWeb.findAllAsync(findText);
            }
        } else if (id == R.id.btn_find_cancel) {
            mWeb.clearMatches();
            findViewById(R.id.ly_find).setVisibility(View.GONE);
        }
    }

    /**
     * 设备返回键事件处理
     */
    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        if (mWeb.canGoBack()) {
            mWeb.goBack();
            return;
        }
        if (currentTime - lastBackPressedTime > 3000) {
            lastBackPressedTime = currentTime;
            showMessage("3秒内再按一次退出程序！");
            return;
        }
        super.onBackPressed();
    }
}
