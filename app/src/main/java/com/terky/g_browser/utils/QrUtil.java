package com.terky.g_browser.utils;

import android.graphics.*;
import com.google.zxing.*;
import com.google.zxing.common.*;
import java.util.*;

public class QrUtil
{


    public static String decodeQRCode(Bitmap bmp) {
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        Result result;
        RGBLuminanceSource source = null;
        try {
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int[] pixels = new int[width * height];
            bmp.getPixels(pixels, 0, width, 0, 0, width, height);
            source = new RGBLuminanceSource(width, height, pixels);
            result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            if (source != null) {
                try {
                    result = new MultiFormatReader().decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)), hints);
                    return result.getText();
                } catch (Throwable e2) {
                    e2.printStackTrace();
                }
            }
            return null;
        }
    }

    public static Bitmap generateQRBitmap(String content) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        int w = 400, h = 400;
        try {
            BitMatrix result = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, w, h, hints);
            int[] pixels = new int[w * h];
            int cb = Color.BLACK, cw = Color.WHITE;
            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    //根据二维矩阵数据创建数组
                    pixels[offset + x] = result.get(x, y) ? cb : cw;
                }
            }
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bmp.setPixels(pixels, 0, w, 0, 0, w, h);
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }

    // 将本地图片文件转换成可解码二维码的 Bitmap。为了避免图片太大，这里对图片进行了压缩。
	/*
	 private static Bitmap getDecodeAbleBitmap(String picturePath) {
	 try {
	 BitmapFactory.Options options = new BitmapFactory.Options();
	 options.inJustDecodeBounds = true;
	 BitmapFactory.decodeFile(picturePath, options);
	 int sampleSize = options.outHeight / 400;
	 if (sampleSize <= 0) {
	 sampleSize = 1;
	 }
	 options.inSampleSize = sampleSize;
	 options.inJustDecodeBounds = false;
	 return BitmapFactory.decodeFile(picturePath, options);
	 } catch (Exception e) {
	 return null;
	 }
	 }
	 */
}
