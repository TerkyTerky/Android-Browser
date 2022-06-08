package com.terky.g_browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import com.terky.g_browser.R;

public class GVideoView extends RelativeLayout implements View.OnClickListener, View.OnTouchListener {

    private final ImageButton ib;
    RelativeLayout rlCover;

    public GVideoView(Context context) {
        this(context, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    public GVideoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // 利用布局文件填充View实例
        inflate(context, R.layout.layout_fullscreen, this);
        rlCover = findViewById(R.id.rl_cover);
        rlCover.setOnTouchListener(this);
        ib = findViewById(R.id.ib_lock);
        ib.setOnClickListener(this);
    }

    private boolean locked = false;

    private final MHandler mHandler = new MHandler(this);

    // 锁屏拦截
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == rlCover.getId()) {
            ib.setVisibility(View.VISIBLE);
            startTime = System.currentTimeMillis();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.sendEmptyMessage(HIDE_INT);
                }
            }, 3000);
            return locked;
        }
        return false;
    }

    // 定时隐藏‘锁按钮’
    private final int HIDE_INT = 0;
    private long startTime = 0;

    private static class MHandler extends Handler {
        WeakReference<GVideoView> wr;

        public MHandler(GVideoView uvv) {
            wr = new WeakReference<>(uvv);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            GVideoView uvv = wr.get();
            if (uvv == null) {
                return;
            }
            if (msg.what == uvv.HIDE_INT) {
                if (System.currentTimeMillis() - uvv.startTime >= 3000) {
                    uvv.ib.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == ib.getId()) {
            locked = !locked;
            int imgId = locked ? R.drawable.ic_lock : R.drawable.ic_unlock;
            ib.setImageResource(imgId);
        }
    }
}
