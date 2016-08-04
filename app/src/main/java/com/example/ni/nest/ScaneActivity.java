package com.example.ni.nest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ni.nest.Scanner.CameraController;
import com.example.ni.nest.Scanner.DisplayUtils;
import com.example.ni.nest.Scanner.Preview;
import com.example.ni.nest.Scanner.ViewFinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScaneActivity extends AppCompatActivity {

    public static final String SCAN_OK = "scan_ok";
    public static final long SCAN_DELAY = 2000;
    private static final String TAG = "ScaneActivity----->";
    private static Set<BarcodeFormat> PRODUCT_FORMATS;
    private static Set<BarcodeFormat> INDUSTRIAL_FORMATS;

    static {
        PRODUCT_FORMATS = EnumSet.of(BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED);
        INDUSTRIAL_FORMATS = EnumSet.of(BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR);
    }

    private TextView mTitle;
    private Preview mPreview;
    private ViewFinderView mMaskView;
    private Button mButton;

    private int mAction;
    private CameraController mCameraController;
    private Camera.Size mPreviewSize;
    private boolean mCameraOpenSuccess;
    private Rect mCropRect = new Rect();
    private MultiFormatReader mMultiFormatReader;
    private byte[] data;
    private int[] rgbArray;
    private boolean cancelDecode = false;


    private Handler mHandler;
    private View.OnClickListener mButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };
    private CameraController.Callback mCameraControllerCallback = new CameraController.Callback() {
        @Override
        public void onCameraStartPreview() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMaskView.startScanAnimation();
                    // when call this directly, auto Focus seems not work, don't know why
                    mCameraController.setAutoFocus();
                }
            });
        }

        @Override
        public void onCameraStopPreview() {
            mCameraController.cancelAutoFocus();
            mCameraController.clearOneShortPreviewCallback();
            mMaskView.stopScanAnimation();
        }

        @Override
        public void onAutoFocus(boolean success) {
            if (success) {
                mCameraController.setOneShotPreviewCallback();
            } else {
                mCameraController.setAutoFocus();
            }
        }

        @Override
        public void onPreviewFrame(byte[] source) {
            new DecodeTask().execute(source);
        }

        @Override
        public void afterSurfaceChange() {
            initDataAfterPreviewStarted();
        }

        private void initDataAfterPreviewStarted() {
            mPreviewSize = mCameraController.getPreviewSize();
            float scale = mPreview.getDisplayToPreviewScaleRatio();
            Rect windowRect = mMaskView.getWindowRect();
            int a = (int) (windowRect.left / scale - 0.5);
            int b = (int) (windowRect.top / scale - 0.5);
            int c = (int) (windowRect.right / scale + 0.5);
            int d = (int) (windowRect.bottom / scale + 0.5);
            if (DisplayUtils.getScreenOrientation(ScaneActivity.this) == Configuration.ORIENTATION_PORTRAIT) {
                mCropRect.set(b, mPreviewSize.height - c, d, mPreviewSize.height - a);
            } else {
                mCropRect.set(a, b, c, d);
            }
        }
    };
    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (SCAN_OK.equals(intent.getAction())) {
                String text = intent.getStringExtra("scan_data");
                Intent resultIntent = new Intent();
                Bundle bundle = new Bundle();
                resultIntent.putExtras(bundle);
                ScaneActivity.this.setResult(RESULT_OK, resultIntent);
                ScaneActivity.this.finish();
            }
        }
    };

    private static Bitmap YUV420spToBitmap(byte[] yuv, int width, int height, Rect cropRect) {
        YuvImage yuvImage = new YuvImage(yuv, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (cropRect == null) {
            cropRect = new Rect(0, 0, width, height);
        }
        yuvImage.compressToJpeg(cropRect, 80, baos);
        byte[] data = baos.toByteArray();
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
        return b;
    }

    private static void L(String msg) {
        Log.d(TAG, msg);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scane);
        registerReceiver(mScanReceiver, new IntentFilter(SCAN_OK));
        mHandler = new Handler(getMainLooper());

        mAction = getIntent().getIntExtra(Cons.ARG_ACTION, Cons.ACTION_CAPTURE);


        mPreview = (Preview) findViewById(R.id.preview);
        mMaskView = (ViewFinderView) findViewById(R.id.view_finder_view);
        mMaskView.setMoney("");
        mButton = (Button) findViewById(R.id.switch_paymode);
        mButton.setOnClickListener(mButtonOnClickListener);


        mCameraController = new CameraController(this, mPreview);
        mCameraController.registerCallback(mCameraControllerCallback);
        mCameraOpenSuccess = mCameraController.openCamera();

        // init barcode reader
        mMultiFormatReader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        // Add specific formats
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);
        formats.addAll(PRODUCT_FORMATS);
        formats.addAll(INDUSTRIAL_FORMATS);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        mMultiFormatReader.setHints(hints);
    }

    private void T(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setResultAndFinish() {
        setResultAndFinish(false, null);
    }

    private void setResultAndFinish(boolean ok, String scanResult) {
        if (ok) {
            Intent data = new Intent();
            data.putExtra(Cons.ARG_ACTION, mAction);
            data.putExtra(Cons.ARG_SCAN_RESULT, scanResult);
            L("Scan String: " + scanResult);
            setResult(RESULT_OK, data);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancelDecode = true;
        setResultAndFinish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResultAndFinish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        L("on resume running");
        super.onResume();
        if (mCameraOpenSuccess) {
            mCameraController.startPreviewSafe();
        }
    }

    @Override
    protected void onPause() {
        L("on pause running");
        if (mCameraOpenSuccess) {
            mCameraController.stopPreview();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        L("on destroy running");
        unregisterReceiver(mScanReceiver);
        mCameraController.closeCamera();
        super.onDestroy();
    }

    private class DecodeTask extends AsyncTask<byte[], Void, String> {
        @Override
        protected void onPostExecute(String b) {
            super.onPostExecute(b);
            if (cancelDecode) {
                return;
            }
            if (b == null) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mCameraController != null) {
                            mCameraController.setAutoFocus();
                        }
                    }
                }, SCAN_DELAY);
                return;
            }
            setResultAndFinish(true, b);
        }

        @Override
        protected String doInBackground(byte[]... params) {
            // we only need luminance part of NV21
            String result = null;
            byte[] source = params[0];
            int sourceWidth = mPreviewSize.width;
            int sourceHeight = mPreviewSize.height;
            Bitmap b = YUV420spToBitmap(source, sourceWidth, sourceHeight, mCropRect);
            int dataWidth = mCropRect.width();
            int dataHeight = mCropRect.height();
            if (DisplayUtils.getScreenOrientation(ScaneActivity.this) == Configuration.ORIENTATION_PORTRAIT) {
                // rotate 270
                Matrix matrix = new Matrix();
                matrix.postRotate(270);
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                dataHeight = mCropRect.width();
                dataWidth = mCropRect.height();
            }
            if (cancelDecode) {
                return null;
            }
            if (rgbArray == null) {
                rgbArray = new int[dataWidth * dataHeight];
            }
            b.getPixels(rgbArray, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
            LuminanceSource luminance = new RGBLuminanceSource(dataWidth, dataHeight, rgbArray);
            BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(luminance));
            try {
                result = mMultiFormatReader.decodeWithState(bb).getText();
            } catch (NotFoundException e) {
                e.printStackTrace();
            } finally {
                mMultiFormatReader.reset();
            }
            return result;
        }
    }
}
