package com.todobom.opennotescanner.views;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.io.FileOutputStream;
import java.util.List;

public class OpenNoteCameraView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "Tutorial3View";
    private String mPictureFileName;

    public OpenNoteCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public boolean isEffectSupported(String effect) {
        List<String> effectList = getEffectList();
        for(String str: effectList) {
            if(str.trim().contains(effect))
                return true;
        }
        return false;
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public List<Size> getPictureResolutionList() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    public void setMaxPictureResolution() {
        int maxWidth=0;
        Size curRes=null;
        for ( Size r: getPictureResolutionList() ) {
            Log.d(TAG,"supported picture resolution: "+r.width+"x"+r.height);
            if (r.width>maxWidth) {
                maxWidth=r.width;
                curRes=r;
            }
        }

        if (curRes!=null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPictureSize(curRes.width, curRes.height);
            mCamera.setParameters(parameters);
            Log.d(TAG, "selected picture resolution: " + curRes.width + "x" + curRes.height);
        }

        return;
    }



    public void setMaxPreviewResolution() {
        int maxWidth=0;
        Size curRes=null;

        mCamera.lock();

        for ( Size r: getResolutionList() ) {
            if (r.width>maxWidth) {
                Log.d(TAG,"supported preview resolution: "+r.width+"x"+r.height);
                maxWidth=r.width;
                curRes=r;
            }
        }

        if (curRes!=null) {
            setResolution(curRes);
            Log.d(TAG, "selected preview resolution: " + curRes.width + "x" + curRes.height);
        }

        return;
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
        Log.d(TAG,"resolution: "+resolution.width+" x "+resolution.height);
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }


    public void setFlash(boolean stateFlash) {
        /* */
        Camera.Parameters par = mCamera.getParameters();
        par.setFlashMode(stateFlash ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(par);
        Log.d(TAG,"flash: " + (stateFlash?"on":"off"));
        // */
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    public void takePicture(PictureCallback callback) {
        Log.i(TAG, "Taking picture");

        mCamera.takePicture(null, null, callback);

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }
}