package com.terky.g_browser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.terky.g_browser.utils.ToolsPopWindow;

public class MainActivity extends AppCompatActivity {

    //LOG标签
    private static final String DEG_TAG = "webBrowser";

    //webView相关
    private WebView webHolder;
    private WebSettings settings;
    private WebViewClient viewClient;
    private WebChromeClient chromeClient;

    //地址栏编辑框
    private EditText webUrlStr;
    private TextView webUrlTitle;

    //搜索栏按钮
    private Button webUrlGoto;
    private Button webUrlCancel;
    private Button webUrlFavorites;
    //工具栏按钮组
    private Button preButton;
    private Button nextButton;
    private Button toolsButton;
    private Button windowsButton;
    private Button homeButton;

    //工具栏按钮显示界面
    private ToolsPopWindow toolsPopWindow;

    //地址栏布局管理器
    private LinearLayout webUrlLayoutInput;
    private LinearLayout webUrlLayoutShow;
    //收藏历史界面
    //private RelativeLayout favoritesAndHistory;

    //地址栏显示标签
    private boolean inputShow = false;

    //URL地址
    private String url = "";

    //URL网页标题
    private String title = "";

    //事件监听器
    private ButtonClickedListener buttonClickedListener;
    private WebUrlStrChangedListener urlStrChangedListener;
    private WebViewTouchListener webViewTouchListener;
    private OnOutOfFocusLinstener onOutOfFocusLinstener;

    //进度条
    private ProgressBar webProgressBar;

    //手势
    private GestureDetector mGestureDetector;
    private GestureListener gestureListener;

    //书签管理
    private BookmarkManager favoriteManager;

    //计时
    private static boolean isExit = false;
    private static Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_main);

        //组件初始化
        this.webHolder = (WebView) this.findViewById(R.id.web_holder);
        this.settings = this.webHolder.getSettings();
        this.viewClient = new OwnerWebView();
        this.chromeClient = new OwnerChromeClient();

        this.webUrlStr = (EditText) this.findViewById(R.id.web_url_input);
        this.webUrlTitle = (TextView) this.findViewById(R.id.web_url_show_title);

        this.webUrlGoto = (Button) this.findViewById(R.id.web_url_goto);
        this.webUrlCancel = (Button) this.findViewById(R.id.web_url_cancel);
        this.webUrlFavorites = (Button) this.findViewById(R.id.web_url_show_favorite);
        this.preButton = (Button) this.findViewById(R.id.pre_button);
        this.nextButton = (Button) this.findViewById(R.id.next_button);
        this.toolsButton = (Button) this.findViewById(R.id.tools_button);
        this.windowsButton = (Button) this.findViewById(R.id.window_button);
        this.homeButton = (Button) this.findViewById(R.id.home_button);

        this.toolsPopWindow = new ToolsPopWindow(this, this.getWindowManager().getDefaultDisplay().getWidth()-30,
                this.getWindowManager().getDefaultDisplay().getHeight()/3);

        this.webUrlLayoutInput = (LinearLayout) this.findViewById(R.id.web_url_layout);
        this.webUrlLayoutShow = (LinearLayout) this.findViewById(R.id.web_url_layout_normal);
        //this.favoritesAndHistory = (RelativeLayout) this.findViewById(R.id.favoritesAndHisotry);

        this.webProgressBar = (ProgressBar) this.findViewById(R.id.web_progress_bar);

        this.buttonClickedListener = new ButtonClickedListener();
        this.urlStrChangedListener = new WebUrlStrChangedListener();
        this.webViewTouchListener = new WebViewTouchListener();

        this.gestureListener = new GestureListener();
        this.mGestureDetector = new GestureDetector(this, gestureListener);

        this.favoriteManager = new BookmarkManager(getApplicationContext());

        //设置参数
        this.settings.setDefaultTextEncodingName("UTF-8");
        this.settings.setJavaScriptEnabled(true);

        this.webHolder.setWebViewClient(this.viewClient);
        this.webHolder.setWebChromeClient(this.chromeClient);
        this.preButton.setEnabled(false);
        this.nextButton.setEnabled(false);

        this.webProgressBar.setVisibility(View.GONE);

        //添加监听
        this.webUrlStr.setOnFocusChangeListener(this.onOutOfFocusLinstener);
        this.webUrlStr.addTextChangedListener(this.urlStrChangedListener);

        this.webHolder.setOnTouchListener(this.webViewTouchListener);
        this.webUrlTitle.setOnClickListener(this.buttonClickedListener);

        this.webUrlGoto.setOnClickListener(this.buttonClickedListener);
        this.webUrlCancel.setOnClickListener(this.buttonClickedListener);
        this.webUrlFavorites.setOnClickListener(this.buttonClickedListener);
        this.preButton.setOnClickListener(this.buttonClickedListener);
        this.nextButton.setOnClickListener(this.buttonClickedListener);
        this.toolsButton.setOnClickListener(this.buttonClickedListener);
        this.windowsButton.setOnClickListener(this.buttonClickedListener);
        this.homeButton.setOnClickListener(this.buttonClickedListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * WebViewClient自定义类
     * 覆盖函数：
     * 1.	shouldOverrideUrlLoading
     * 2.	onReceivedError
     * 3.	onPageFinished
     * */
    private class OwnerWebView extends WebViewClient{

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.d(DEG_TAG, "errorCode:"+errorCode+",description:"+description+",failingUrl:"+failingUrl);
            if(errorCode==WebViewClient.ERROR_HOST_LOOKUP){
                //找不到页面，调用百度搜搜
                url = "http://www.baidu.com/baidu?word=" + url;
                Log.d(DEG_TAG, "errorRedirect:"+url);
                webHolder.loadUrl(url);
            }else if(errorCode==WebViewClient.ERROR_UNSUPPORTED_SCHEME){
                //不支持协议
                Log.d(DEG_TAG, "equals javascript:"+failingUrl.toString().equals("javascript:;"));
                //if(failingUrl.trim().equals("javascript:;")){
                //不支持javascript

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("警告")
                        .setMessage("不支持javascript")
                        .create()
                        .show();

                //}
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            webUrlLayoutInput.setVisibility(View.GONE);
            webUrlLayoutShow.setVisibility(View.GONE);
            inputShow = false;
            webUrlStr.setText(url);
            changeStatueOfWebToolsButton();
        }

    }

    /**
     * WebChromeClient自定义继承类
     * 覆盖如下方法
     * 1.	onProgressChanged
     * 		用来解决进度条显示问题
     * */
    private class OwnerChromeClient extends WebChromeClient{

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            //MainActivity.this.setProgress(newProgress * 100);
            if(newProgress==100){
                webProgressBar.setVisibility(View.GONE);
            }else{
                webProgressBar.setVisibility(View.VISIBLE);
                webProgressBar.setProgress(newProgress);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            MainActivity.this.title = title;
            webUrlTitle.setText(title);
        }

    }


    /**
     * OnClickListener自定义继承类
     * 用来解决按钮监听
     * */
    private class ButtonClickedListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            if(v.getId()==R.id.web_url_goto){
                Log.d(DEG_TAG,"url:"+url);
                webHolder.loadUrl(url);
            }else if(v.getId()==R.id.web_url_cancel){
                webUrlStr.setText("");
            }else if(v.getId()==R.id.pre_button){
                if(webHolder.canGoBack()){
                    //后退
                    webHolder.goBack();
                }
            }else if(v.getId()==R.id.next_button){
                if(webHolder.canGoForward()){
                    //前进
                    webHolder.goForward();
                }
            }else if(v.getId()==R.id.tools_button){
                //展现工具视图
                LayoutInflater toolsInflater = LayoutInflater.from(getApplicationContext());
                View toolsView = toolsInflater.inflate(R.layout.tabactivity_tools, null);
                toolsPopWindow.showAtLocation(toolsView, Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, toolsButton.getHeight()+20);
                Button refresh = (Button) toolsPopWindow.getView(R.id.tools_normal_refresh);
                Button favorites = (Button) toolsPopWindow.getView(R.id.tools_normal_favorites);
                refresh.setOnClickListener(this);
                favorites.setOnClickListener(this);
            }else if(v.getId()==R.id.window_button){

            }else if(v.getId()==R.id.home_button){
                webHolder.loadUrl("http://www.baidu.com");
            }else if(v.getId()==R.id.web_url_show_favorite){
                //添加书签
                Log.d(DEG_TAG, "title:"+title+",url:"+url);
                favoriteManager.addFavorite(title, url);
                favoriteManager.getAllFavorites();
            }else if(v.getId()==R.id.web_url_show_title){
                //点击标题栏，显示地址输入框架
                Log.d(DEG_TAG, "webtitle button down");
                changeStatueOfWebUrlLayout(true);
            }else if(v.getId()==R.id.tools_normal_refresh){
                //刷新
                if(!(url.equals("")&&url.equals("http://"))){
                    webHolder.loadUrl(url);
                }
            }else if(v.getId()==R.id.tools_normal_favorites){
                //收藏
				/*favoritesAndHistory.setVisibility(View.VISIBLE);
				favoritesAndHistory.setFocusable(true);*/
                startActivity(new Intent(MainActivity.this, BookmarkAndHistoryActivity.class));
            }
        }

    }

    /**
     * OnFocusChangeListener自定义继承类
     * 覆盖方法如下：
     * 1.	onFocusChange
     * */
    private class OnOutOfFocusLinstener implements OnFocusChangeListener{

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            //地址输入失去焦点
            Log.d(DEG_TAG, "hasFocus:"+hasFocus);
            if(!webUrlStr.hasFocus()){
                Log.d(DEG_TAG, "地址栏输入:lose focus");
                changeStatueOfWebUrlLayout(false);

            }
        }

    }

    /**
     * TextWatcher自定义继承类
     * 覆盖方法如下：
     * 1.	afterTextChanged
     * 2.	beforeTextChanged
     * 3.	onTextChanged
     * 		实现更改地址的时候进行地址合法性检测
     * */
    private class WebUrlStrChangedListener implements TextWatcher{

        @Override
        public void afterTextChanged(Editable editable) {
        }

        @Override
        public void beforeTextChanged(CharSequence charsequence, int i, int j,
                                      int k) {

        }

        @Override
        public void onTextChanged(CharSequence charsequence, int i, int j, int k) {
            url = charsequence.toString();
            if(!(url.startsWith("http://")||url.startsWith("https://"))){
                url = "http://"+url;
            }
            Log.d(DEG_TAG,"onchangeText:"+url);
            if(URLUtil.isNetworkUrl(url)&&URLUtil.isValidUrl(url)){
                changeStatueOfWebGoto(true);
            }else{
                changeStatueOfWebGoto(false);
            }
        }

    }

    /**
     * OnTouchListener自定义继承类
     * 解决将手势交给GestureDetector类解决
     * */
    private class WebViewTouchListener implements OnTouchListener{

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(v.getId()==R.id.web_holder){
                //Log.i(DEG_TAG, "info :webViewTouched");
                //Log.i(DEG_TAG, "event:"+event.describeContents());
                return mGestureDetector.onTouchEvent(event);
            }
            return false;
        }

    }

    /**
     * GestureDetector.OnGestureListener自定义继承类
     * 解决各种手势的相对应策略
     * 1.	向上滑动webView到顶触发事件，显示地址栏
     * 2.	向下滑动webView触发时间，隐藏地址栏
     * */
    private class GestureListener implements GestureDetector.OnGestureListener{

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            //Log.i(DEG_TAG, "fling event");
            //Log.d(DEG_TAG, "webHolder scaleY:"+webHolder.getScrollY());
            if(webHolder.getScrollY()==0){
                //滑倒顶部
                //Log.d(DEG_TAG, "已经滑倒顶部");
                webUrlLayoutShow.setVisibility(View.VISIBLE);
                if(inputShow){
                    webUrlLayoutInput.setVisibility(View.GONE);
                    inputShow = false;
                }
            }
            if(webHolder.getScrollY()>0){
                //Log.d(DEG_TAG, "已经滑倒底部");
				/*Log.d(DEG_TAG, "contentHeight:"+webHolder.getContentHeight()
						+",hight:"+webHolder.getHeight()
						+",scale:"+webHolder.getScale()
						+",ScrollY:"+webHolder.getScrollY());*/
                webUrlLayoutShow.setVisibility(View.GONE);
                if(inputShow){
                    webUrlLayoutInput.setVisibility(View.GONE);
                    inputShow = false;
                }
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

    }
	/*private class WebViewScrollListener implements OnScaleGestureListener{

	}*/


    /**
     * 更改进入
     * */
    private void changeStatueOfWebGoto(boolean flag){
        if(flag){
            webUrlGoto.setVisibility(View.VISIBLE);
            webUrlCancel.setVisibility(View.GONE);
        }else{
            webUrlGoto.setVisibility(View.GONE);
            webUrlCancel.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 设置工具栏回溯历史是否可用
     * */
    private void changeStatueOfWebToolsButton(){
        if(webHolder.canGoBack()){
            //设置可使用状态
            preButton.setEnabled(true);
        }else{
            //设置禁止状态
            preButton.setEnabled(false);
        }
        if(webHolder.canGoForward()){
            //设置可使用状态
            nextButton.setEnabled(true);
        }else{
            //设置禁止状态
            nextButton.setEnabled(false);
        }
    }

    /**
     * 设置工具栏转换
     * */
    private void changeStatueOfWebUrlLayout(boolean show){
        Log.d(DEG_TAG,"工具栏转换");
        if(show){
            webUrlLayoutShow.setVisibility(View.GONE);
            webUrlLayoutInput.setVisibility(View.VISIBLE);
            inputShow = true;
            webUrlStr.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            //显示软键盘
            if(!imm.isActive()){
                Log.d(DEG_TAG, "软键盘已经关闭");
            }else{
                Log.d(DEG_TAG, "软键盘开启");
            }
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
        }else{
            webUrlLayoutShow.setVisibility(View.VISIBLE);
            webUrlLayoutInput.setVisibility(View.GONE);
            inputShow = false;
        }
    }

    @Override
    public void onBackPressed() {
        //判断是否可后退，是则后退，否则按两次退出程序
        if(webHolder.canGoBack()){
            webHolder.goBack();
        }else{
            if(!isExit){
                isExit = true;
                Toast.makeText(getApplicationContext(), "再按一次退出程序",
                        Toast.LENGTH_SHORT).show();
                handler.sendEmptyMessageDelayed(0,2000);
            }else{
                finish();
                System.exit(0);
            }
        }
    }


}
