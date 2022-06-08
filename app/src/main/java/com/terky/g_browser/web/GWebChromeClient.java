package com.terky.g_browser.web;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.terky.g_browser.view.GVideoView;
import com.terky.g_browser.R;

/**
 * 辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
 **/
public class GWebChromeClient extends WebChromeClient {

    private final GWebView uwv;
    private Activity activity;
    private GWebView.StateListener mListener;

    public GWebChromeClient(GWebView uwv) {
        this.uwv = uwv;
        Context c = uwv.getContext();
        if (c instanceof Activity) {
            activity = (Activity) c;
            return;
        }
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) {
                activity = (Activity) c;
                return;
            }
            c = ((ContextWrapper) c).getBaseContext();
        }
    }

    /**
     * 显示进度
     *
     * @param view        WebView
     * @param newProgress 新的进度（0~100）
     */
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if ((mListener = uwv.getStateListener()) != null) {
            mListener.onStateChanged(uwv,
                    mListener.STATE_PROGRESS, newProgress);
        }
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        if ((mListener = uwv.getStateListener()) != null) {
            mListener.onStateChanged(uwv,
                    GWebView.StateListener.STATE_RECEIVED_TITLE, title);
        }
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        super.onReceivedIcon(view, icon);
        if ((mListener = uwv.getStateListener()) != null) {
            mListener.onStateChanged(uwv,
                    mListener.STATE_RECEIVED_ICON, icon);
        }
    }

    //全屏播放视频需要实现onShowCustomView(...)和onHideCustomView()
    View normalLayout;

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback);
        normalLayout = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        setFullScreen(true);
        View videoView = new GVideoView(activity);
        activity.setContentView(videoView);
        FrameLayout fl = activity.findViewById(R.id.fl_video_container);
        fl.addView(view);
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        setFullScreen(false);
        activity.setContentView(normalLayout);
    }

    private void setFullScreen(boolean bl) {
        int orientationFlag = bl ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        int keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        Window win = activity.getWindow();
        if (bl) {
            win.setFlags(keepScreenOnFlag, keepScreenOnFlag);
            win.setFlags(fullScreenFlag, fullScreenFlag);
        } else {
            win.clearFlags(keepScreenOnFlag);
            win.clearFlags(fullScreenFlag);
        }
        activity.setRequestedOrientation(orientationFlag);
    }
}
