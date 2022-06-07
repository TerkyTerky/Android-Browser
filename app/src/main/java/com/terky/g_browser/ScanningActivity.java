package com.terky.g_browser;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

import android.hardware.Camera;

import com.terky.g_browser.utils.QrUtil;

public class ScanningActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanning);

        validatePermission();
        FrameLayout fl = findViewById(R.id.fl_preview);
        ScanningView preview = new ScanningView(this);
        fl.addView(preview);
    }

    private void validatePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        String[] ps = {
                Manifest.permission.CAMERA
        };
        for (String p : ps) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(ps, 1506);
                break;
            }
        }
    }
}

class ScanningView extends SurfaceView
        implements SurfaceHolder.Callback {

    private final SurfaceHolder mHolder;
    private Camera mCamera;

    public ScanningView(Context c) {
        super(c);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    byte[] data;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //surface第一次创建时回调
        //打开相机
        mCamera = Camera.open();
        Camera.Parameters params = mCamera.getParameters();
        mCamera.setDisplayOrientation(90);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data, Camera camera) {
                ScanningView.this.data = data;
            }
        });
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (mCamera == null) {
                    cancel();
                    return;
                }
                Camera.Size mSize = mCamera.getParameters().getPreviewSize();
                YuvImage yuvimage = new YuvImage(
                        data, ImageFormat.NV21, mSize.width, mSize.height, null);
                ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(0, 0, mSize.width, mSize.height), 100, bAOS);
                byte[] bs = bAOS.toByteArray();
                // 将imageByte转换成bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bmp = BitmapFactory.decodeByteArray(bs, 0, bs.length, options);
                // 解码
                String result = QrUtil.decodeQRCode(bmp);
                if (TextUtils.isEmpty(result)) {
                    return;
                }
                cancel();
                Context c = getContext();
                Activity act = c instanceof Activity ? (Activity) c : null;
                while (c instanceof ContextWrapper) {
                    c = ((ContextWrapper) c).getBaseContext();
                    if (c instanceof Activity) {
                        act = (Activity) c;
                        break;
                    }
                }
                if (act != null) {
                    Intent it = act.getIntent();
                    it.putExtra("msg", result);
                    act.setResult(Activity.RESULT_OK, it);
                    act.finish();
                }
            }
        }, new Date(System.currentTimeMillis() + 3000), 500);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //surface变化的时候回调(格式/大小)
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //surface销毁的时候回调
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

	/*
	 添加偏好设置
	 如分辨率、闪光灯、对焦等。
	 通过当前界面的相机camera对象获取起设置的参数getParameters()
	 预览分辨率
	 parameters.getSupportedPreviewSizes()
	 预览格式
	 具体参照ImageFormat或者自己Google
	 parameters.getSupportedPreviewFormats()
	 照片分辨率
	 parameters.getSupportedPictureSizes()
	 图片格式
	 具体参照ImageFormat或者自己Google
	 parameters.getSupportedPictureFormats()
	 视频分辨率
	 parameters.getSupportedVideoSizes()
	 对焦模式
	 parameters.getSupportedFocusModes()
	 曝光补偿
	 parameters.getMinExposureCompensation()
	 parameters.getMaxExposureCompensation()
	 闪光灯模式
	 parameters.getSupportedFlashModes()获取相机支持的闪光灯模式
	 白平衡
	 parameters.getSupportedWhiteBalance()获取相机支持的白平衡
	 场景
	 parameters.getSupportedSceneModes()获取相机支持的场景
	 */
}
