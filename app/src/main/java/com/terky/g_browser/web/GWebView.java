package com.terky.g_browser.web;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.terky.g_browser.utils.QrUtil;
import com.terky.g_browser.utils.GDownloadUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;

import com.terky.g_browser.R;
import com.terky.g_browser.view.GDialog;

public class GWebView extends WebView implements View.OnLongClickListener {

    // user agent
    public static final String STR_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36";

    // Settings options, boolean option
    public static final String[] OPTIONS = new String[] {
            // 登录邮箱用到
            "AcceptThirdPartyCookies",
            // 缩放
            "DisplayZoomControls",
            "LoadWithOverviewMode",
            // network
            "BlockNetworkLoads",
            // other settings
            "MediaPlaybackRequiresUserGesture",
            "UsePcFlag",
            "AllowCallOtherApp"
    };
    // Other options
    public static final String[] OPTIONS_EXTRA = new String[]{
            "JavaScriptEnabled",
            "BlockNetworkImage",
            "TextZoom"
    };

    private GDownloadUtil downloadUtil;
    private final String SHARED_PREF = "web_view.sp";
    private boolean allowCallOtherApp = false;// 是否允许调用外部应用
    private boolean enabledTranslationJS;
    private boolean enabledFind = true;
    private boolean mIsDarkMode = false;

    public void setIsDarkMode(boolean isDarkMode) {
        mIsDarkMode = isDarkMode;
    }

    public boolean getIsDarkMode() {
        return mIsDarkMode;
    }

    public boolean getEnabledFind() {
        return enabledFind;
    }

    public void setEnabledFind(boolean enabledFind) {
        this.enabledFind = enabledFind;
    }

    public boolean getAllowCallOtherApp() {
        return allowCallOtherApp;
    }

    public void setAllowCallOtherApp(boolean allowCallOtherApp) {
        this.allowCallOtherApp = allowCallOtherApp;
    }

    // 构造函数
    public GWebView(Context context) {
        this(context, null);
    }

    public GWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        readPreferences();
    }

    /**
     * 初始化WebView
     */
    @SuppressLint("AddJavascriptInterface")
    private void initView() {
        downloadUtil = new GDownloadUtil(getContext());
        setDownloadListener(downloadUtil);
        setOnLongClickListener(this);
        setWebChromeClient(new GWebChromeClient(this));
        setWebViewClient(new GWebViewClient(this));
        WebSettings sets = this.getSettings();
        File cacheDir = getContext().getExternalCacheDir();
        if (cacheDir != null) {
            sets.setAppCachePath(cacheDir.getAbsolutePath());
        } else {
            showToast("Valid cache path");
        }
        // must be supplied a valid path to {setAppCachePath}
        sets.setAppCacheEnabled(true);
        sets.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        sets.setUseWideViewPort(true);
        sets.setDomStorageEnabled(true);
        sets.setDatabasePath(getContext().getExternalFilesDir(null) + "ub.db");
        sets.setDatabaseEnabled(true);
        sets.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);//减少流量
        loadDefaultSettings();
        // 手势缩放
        sets.setBuiltInZoomControls(true);
    }

    // 加载偏好设置
    private void readPreferences() {
        SharedPreferences sp = getContext()
                .getSharedPreferences(SHARED_PREF, 0);
        enabledTranslationJS = sp.getBoolean(
                "enabledTranslationJS", false);
    }

    // 保存偏好设置
    private void savePreferences() {
        SharedPreferences sp = getContext()
                .getSharedPreferences(SHARED_PREF, 0);
        sp.edit()
                .putBoolean("enabledTranslationJS", enabledTranslationJS)
                .apply();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void loadDefaultSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        }
        WebSettings sets = getSettings();
        //缩放
        sets.setTextZoom(100);
        sets.setDisplayZoomControls(false);
        sets.setLoadWithOverviewMode(false);
        //network
        sets.setBlockNetworkLoads(false);
        // other settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            sets.setMediaPlaybackRequiresUserGesture(true);
            sets.setUserAgentString(WebSettings.getDefaultUserAgent(getContext()));
        }
        allowCallOtherApp = false;
    }

    /**
     * 设置序列
     *
     * @param methodName ...
     * @param hasParam   whether has param;
     * @param param      ...
     * @return ...
     */
    public Object invokeSettingsMethod(String methodName, boolean hasParam, Object param) {
        Class<?> pType = null;
        if (param instanceof Boolean) {
            pType = boolean.class;
        } else if (param instanceof Integer) {
            pType = int.class;
        } else if (param instanceof String) {
            pType = String.class;
        }
        WebSettings settings = this.getSettings();
        // 自定义属性
        switch (methodName) {
            case "setUsePcFlag":
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    boolean bl = (boolean) param;
                    String userAgentString = bl ?
                            STR_AGENT : WebSettings.getDefaultUserAgent(getContext());
                    settings.setUserAgentString(userAgentString);
                }
                break;
            case "getUsePcFlag":
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    return !settings.getUserAgentString().equals(
                            WebSettings.getDefaultUserAgent(getContext()));
                }
                break;
            case "getAllowCallOtherApp":
                return getAllowCallOtherApp();
            case "setAllowCallOtherApp":
                setAllowCallOtherApp((boolean) param);
                break;
            case "setAcceptThirdPartyCookies":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(
                            this, (boolean) param);
                }
                break;
            case "getAcceptThirdPartyCookies":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return CookieManager.getInstance().acceptThirdPartyCookies(this);
                }
                break;
            default:
                // 系统属性
                try {
                    if (hasParam) {
                        Method method = settings.getClass().getMethod(methodName, pType);
                        return method.invoke(settings, param);
                    } else {
                        Method method = settings.getClass().getMethod(methodName);
                        return method.invoke(settings);
                    }
                } catch (Exception e) {
                    Log.e("InvokeError", methodName);
                    e.printStackTrace();
                }
        }
        return null;
    }

    /**
     * LongClick download picture
     *
     * @param v ...
     * @return ...
     */
    @Override
    public boolean onLongClick(View v) {
        final HitTestResult htr = this.getHitTestResult();
        if (htr.getExtra() == null || htr.getType() != HitTestResult.IMAGE_TYPE &&
                htr.getType() != HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            return false;
        }
        Context c = getContext();
        Button btn1 = new Button(c);
        Button btn2 = new Button(c);
        btn1.setText(R.string.download_picture);
        btn2.setText(R.string.scan_qr);
        final AlertDialog dialog = new AlertDialog.Builder(c).create();
        btn1.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View p1) {
                        dialog.dismiss();
                        downloadUtil.downloadPicture(htr.getExtra());
                    }
                });
        btn2.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View p1) {
                        dialog.dismiss();
                        new ScanInternetPicture(GWebView.this).execute(htr.getExtra());
                    }
                });
        LinearLayout ll = new LinearLayout(c);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(btn1);
        ll.addView(btn2);
        dialog.setView(ll);
        dialog.show();
        return true;
    }

    // 扫描网络图片
    private static class ScanInternetPicture extends AsyncTask<String, Integer, String> {
        WeakReference<GWebView> wr;

        public ScanInternetPicture(GWebView uwv) {
            wr = new WeakReference<>(uwv);
        }

        @Override
        protected String doInBackground(String[] urls) {
            String s;
            try {
                InputStream is = new URL(urls[0]).openStream();
                s = QrUtil.decodeQRCode(BitmapFactory.decodeStream(is));
                is.close();
            } catch (IOException e) {
                s = e.toString();
            }
            return s;
        }

        @Override
        protected void onPostExecute(String result) {
            GWebView uwv = wr.get();
            if (uwv != null) {
                result = "scanning result:\n\n" + result;
                GDialog.showSelectableDialog(uwv.getContext(), result);
            }
        }
    }

    /**
     * 编辑模式
     *
     * @param callback 。。。
     * @param type     。。。
     * @return 。。。
     */
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        ActionMode mode = super.startActionMode(callback, type);
        customizeActionMode(mode);
        return mode;
    }

    // 同上
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        ActionMode mode = super.startActionMode(callback);
        customizeActionMode(mode);
        return mode;
    }

    // 定制编辑模式
    private void customizeActionMode(final ActionMode mode) {
        final Menu menu = mode.getMenu();
        final String strTrans = getResources().getString(R.string.translation);
        String strFind = getResources().getString(R.string.find);
        // 添加翻译菜单
        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                MenuItem copyItem = null;
                for (int i = 0; i < menu.size(); i++) {
                    if (menu.getItem(i).getTitle().toString().equalsIgnoreCase(
                            getResources().getString(android.R.string.copy))) {
                        copyItem = menu.getItem(i);
                        break;
                    }
                }
                final ClipboardManager cm = (ClipboardManager) getContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm == null) {
                    showToast("ERROR: Could not get ClipboardService!");
                    mode.finish();
                    return true;
                }
                final ClipData cd = cm.getPrimaryClip();
                if (copyItem != null) {
                    menu.performIdentifierAction(copyItem.getItemId(), 0);
                } else {
                    mode.finish();
                }
                final MenuItem cItem = copyItem;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CharSequence selTxt = null;
                        if (cItem != null && cm.getPrimaryClip() != null) {
                            selTxt = cm.getPrimaryClip().getItemAt(0).getText();
                        }
                        if (selTxt == null) {
                            selTxt = "";
                        }
                        if (cd == null) {
                            cm.setPrimaryClip(ClipData.newPlainText(null, null));
                        } else {
                            cm.setPrimaryClip(cd);
                        }
                        if (item.getTitle().toString().equals(strTrans)) {
                            showTranslation(selTxt);
                        } else {
                            stateListener.onStateChanged(GWebView.this,
                                    stateListener.STATE_FIND_START, selTxt);
                        }
                    }
                }, 100);
                return false;

            }
        };
        menu.add(strTrans).setOnMenuItemClickListener(listener);
        if (getEnabledFind()) {
            menu.add(strFind).setOnMenuItemClickListener(listener);
        }
    }

    /**
     * Translation window
     *
     * @param sourceText what need to translate.
     */
    private void showTranslation(CharSequence sourceText) {
        String url = "https://m.youdao.com/dict?le=eng&q=" + sourceText;
        Context context = getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.translation, null);
        final GWebView uwv = v.findViewById(R.id.uwv_translation);
        uwv.setEnabledFind(false);
        uwv.getSettings().setBlockNetworkImage(!enabledTranslationJS);
        uwv.getSettings().setJavaScriptEnabled(enabledTranslationJS);
        uwv.loadUrl(url);
        final CheckBox cb = v.findViewById(R.id.cb_translation_sj);
        cb.setChecked(enabledTranslationJS);
        cb.setOnCheckedChangeListener(
                new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton p1, boolean p2) {
                        uwv.getSettings().setJavaScriptEnabled(p2);
                        uwv.getSettings().setBlockNetworkImage(!p2);
                        uwv.reload();
                    }
                });
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(v).create();
        Button btnOK = v.findViewById(R.id.btn_translation_ok);
        btnOK.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v1) {
                        dialog.dismiss();
                    }
                });
        dialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface p1) {
                        if (cb.isChecked() != enabledTranslationJS) {
                            enabledTranslationJS = !enabledTranslationJS;
                            savePreferences();
                        }
                    }
                });
        dialog.show();
        // 设置高度必须在dialog.show()后面，否正出错
        int sh = getResources().getDisplayMetrics().heightPixels;
        v.getLayoutParams().height = sh / 2;
    }


    public void loadDark() {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dark)));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);//这里自动去掉换行符，如果还有换行符就不行
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String js = "javascript:(function (){" +
                "var par = document.getElementsByTagName('head')[0];" +
                "var sty = document.createElement('style');" +
                "sty.type='text/css';" +
                "sty.innerHTML='" + sb + "';" +
                "par.appendChild(sty);" +
                "})();";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript(js, null);
        } else {
            loadUrl(js);
        }
    }

    void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }


    StateListener stateListener = null;

    public void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    public StateListener getStateListener() {
        return stateListener;
    }

    public interface StateListener {
        // 状态监听接口
        int STATE_STARTED = 0;
        int STATE_FINISHED = 1;
        int STATE_PROGRESS = 2;
        int STATE_FIND_START = 3;
        int STATE_RECEIVED_TITLE = 4;
        int STATE_RECEIVED_ICON = 5;

        void onStateChanged(WebView v, int stateType, Object... args);
    }
}
