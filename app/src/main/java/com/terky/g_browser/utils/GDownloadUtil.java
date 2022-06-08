package com.terky.g_browser.utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

public class GDownloadUtil implements DownloadListener {
    Context context;

    public GDownloadUtil(Context context) {
        this.context = context;
    }

    /**
     * WeBView 开始下载事件
     *
     * @param url                资源URL
     * @param userAgent          代理
     * @param contentDisposition 内容描述
     * @param mimetype           资源类型
     * @param contentLength      内容长度
     */
    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        String fineName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        showDownloadDialog(url, fineName, getLengthString(contentLength));
    }

    @SuppressLint("DefaultLocale")
    String getLengthString(long len) {
        float div; //divisor
        if ((div = 1024 * 1024 * 1024) < len) {
            return String.format("%1.2f GB", len / div);
        } else if ((div = 1024 * 1024) < len) {
            return String.format("%1.2f MB", len / div);
        } else if ((div = 1024) < len) {
            return String.format("%1.2f KB", len / div);
        }
        return len + " B";
    }

    private void downloadFile(String strUrl, File destFile) {
        Uri uri = Uri.parse(strUrl);
        DownloadManager.Request req = new DownloadManager.Request(uri);
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        req.setDestinationUri(Uri.fromFile(destFile));
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            dm.enqueue(req);
        } else {
            Toast.makeText(context, "Error: DownLoadManager.Request", Toast.LENGTH_SHORT).show();
        }
    }

    public void downloadPicture(String strUrl) {
        String fileName = URLUtil.guessFileName(strUrl, null,
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        strUrl.substring(strUrl.lastIndexOf('.') + 1)));
        showDownloadDialog(strUrl, fileName, null);
    }

    /**
     * 下载对话框
     *
     * @param strUrl    url
     * @param fileName  目标文件名
     * @param strLength 长度字符串
     */
    private void showDownloadDialog(final String strUrl, final String fileName, String strLength) {
        final EditText et = new EditText(context);
        final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        if (strLength == null) strLength = "";
        et.setText(fileName);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("下载提示")
                .setMessage("将下载文件到：" + dir + File.separator + " (大小：" + strLength + ")")
                .setView(et)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!et.getText().toString().equals("")) {
                            downloadFile(strUrl, new File(dir, et.getText().toString()));
                        } else {
                            downloadFile(strUrl, new File(dir, fileName));
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}
