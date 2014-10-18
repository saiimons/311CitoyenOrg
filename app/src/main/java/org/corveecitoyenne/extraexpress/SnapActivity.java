package org.corveecitoyenne.extraexpress;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.keith.mb.MaterialButton;

import java.util.List;


public class SnapActivity extends Activity {
    private static final String TAG = SnapActivity.class.getSimpleName();
    private Camera mCamera;
    private MaterialButton mFlashButton, mSnapButton;
    private CameraPreview mCameraPreview;
    private Camera.PictureCallback mJpegCallback;
    private Camera.AutoFocusCallback mFocusCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            //w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        mCameraPreview = (CameraPreview) findViewById(R.id.camera_preview);
        mFlashButton = (MaterialButton) findViewById(R.id.flash_button);
        mSnapButton = (MaterialButton) findViewById(R.id.snap_button);
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        Camera.Parameters params = mCamera.getParameters();

        // Configure image format. RGB_565 is the most common format.
        List<Integer> formats = params.getSupportedPictureFormats();
        if (formats.contains(PixelFormat.RGB_565))
            params.setPictureFormat(PixelFormat.RGB_565);
        else
            params.setPictureFormat(ImageFormat.JPEG);

        // Choose the biggest picture size supported by the hardware
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size size = params.getPictureSize();
        size.width = 0;
        size.height = 0;
        for (Camera.Size zeSize : sizes) {
            if (zeSize.width <= 640/*1920*/ && zeSize.width > size.width) {
                size = zeSize;
            }
        }
        params.setPictureSize(size.width, size.height);

        List<String> flashModes = params.getSupportedFlashModes();
        if (flashModes != null && flashModes.size() > 0)
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

        // Action mode take pictures of fast moving objects
        List<String> sceneModes = params.getSupportedSceneModes();
        if (sceneModes != null && sceneModes.contains(Camera.Parameters.SCENE_MODE_ACTION))
            params.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
        else
            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

        // if you choose FOCUS_MODE_AUTO remember to call autoFocus() on
        // the Camera object before taking a picture
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        mCamera.setParameters(params);
        mCameraPreview.setCamera(mCamera);
        updateFlashButton();
        mFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    Log.d(TAG, "AUTO-FOCUS");
                    mCamera.takePicture(null, null /*new Camera.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] data, Camera camera) {
                                    Log.d(TAG, "RAW CALLBACK");
                                }
                            }*/, mJpegCallback);
                }
            }
        };
        mJpegCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mCamera.startPreview();
                Log.d(TAG, "JPEG CALLBACK");
                Intent i = new Intent(SnapActivity.this, SubmitActivity.class);
                i.putExtra(SubmitActivity.EXTRA_PICTURE, data);
                startActivity(i);
            }
        };
        mFlashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Camera.Parameters params = mCamera.getParameters();
                String mode = params.getFlashMode();
                Log.d(TAG, "" + mode);
                if (Camera.Parameters.FLASH_MODE_ON.equals(mode)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                } else if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(mode)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
                mCamera.setParameters(params);
                updateFlashButton();
            }
        });

        mSnapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.autoFocus(mFocusCallback);

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    private void updateFlashButton() {
        int id = R.drawable.flash_off;
        String mode = mCamera.getParameters().getFlashMode();
        if (Camera.Parameters.FLASH_MODE_ON.equals(mode)) {
            id = R.drawable.flash_on;
        } else if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)) {
            id = R.drawable.flash_off;
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(mode)) {
            id = R.drawable.flash_auto;
        }
        mFlashButton.setIcon(id);
    }

}
