package com.terky.g_browser.web;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 辅助WebView处理各种通知与请求事件
 **/
public class UWebViewClient extends WebViewClient {

    private final UWebView uwv;
    private UWebView.StateListener mListener;

    public UWebViewClient(UWebView uwv) {
        this.uwv = uwv;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if ((mListener = uwv.getStateListener()) != null) {
            mListener.onStateChanged(uwv,
                    mListener.STATE_STARTED, url, favicon);
        }

    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if ((mListener = uwv.getStateListener()) != null) {
            mListener.onStateChanged(uwv, mListener.STATE_FINISHED, url);
        }
    }

    /**
     * 解决重定向问题
     *
     * @param view WebView
     * @param url  将要加载的Url
     * @return false则标记为未处理。
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("http:") ||
                url.startsWith("https:") ||
                url.startsWith("file:")) {
            return false;
        } else { // 处理自定义协议
            if (!uwv.getAllowCallOtherApp()) {
                return true;
            }
            try {
                Intent it = new Intent();
                it.setAction(Intent.ACTION_VIEW);
                it.setData(Uri.parse(url));
                uwv.getContext().startActivity(it);
            } catch (Exception ignored) {
                // 变量名为ignored可以消除空catch块警告
            }
            return true;
        }
    }
}

