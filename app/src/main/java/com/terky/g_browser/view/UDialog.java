package com.terky.g_browser.view;

import android.content.*;
import android.widget.*;
import android.graphics.*;
import android.view.*;
import android.util.*;
import android.app.*;

public class UDialog {
    public static void showSelectableDialog(Context context, String msg) {
        TextView tv=new TextView(context);
        tv.setText(msg);
        Point size=new Point();
        WindowManager wm=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(size);
        tv.setWidth(size.x * 3 / 4);
        tv.setHeight(size.y / 3);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        // 用tv代替message解决文字不可选择问题
        tv.setTextIsSelectable(true);
        new AlertDialog.Builder(context)
                .setView(tv)
                .show();
    }
}