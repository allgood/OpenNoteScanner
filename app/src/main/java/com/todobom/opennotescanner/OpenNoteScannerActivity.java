package com.todobom.opennotescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.todobom.opennotescanner.helpers.CustomOpenCVLoader;
import com.todobom.opennotescanner.views.OpenNoteCameraView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class OpenNoteScannerActivity extends Activity
        implements NavigationView.OnNavigationItemSelectedListener , CameraBridgeViewBase.CvCameraViewListener2 {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private static final int CREATE_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE = 3;

    private static final int RESUME_PERMISSIONS_REQUEST_CAMERA = 11;

    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.

            getWindow().getDecorView().setSystemUiVisibility(
                    // mContentView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            /*
                      View.SYSTEM_UI_FLAG_LOW_PROFILE
            */
        }
    };

    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private static final String TAG = "OpenNoteScannerActivity";
    private MediaPlayer _shootMP = null;

    private HashMap<String,Long> pageHistory = new HashMap<String,Long>();
    private boolean haveCameraPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_open_note_scanner);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.HelloOpenCvView);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        checkCreatePermissions();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getRealSize(size);
        int width = Math.max(size.x, size.y);
        int height = Math.min(size.x, size.y);

        int hotAreaHeight = height / 4;
        int hotAreaWidth = width / 2 - hotAreaHeight;

        ImageView qTopLeft = (ImageView) findViewById(R.id.quadranttl);
        RelativeLayout.LayoutParams paramsTl = (RelativeLayout.LayoutParams) qTopLeft.getLayoutParams();
        paramsTl.leftMargin = 0;
        paramsTl.topMargin = 0;
        paramsTl.height = hotAreaHeight;
        paramsTl.width = hotAreaWidth;
        qTopLeft.setLayoutParams(paramsTl);

        ImageView qTopRight = (ImageView) findViewById(R.id.quadranttr);
        RelativeLayout.LayoutParams paramsTr = (RelativeLayout.LayoutParams) qTopRight.getLayoutParams();
        paramsTr.leftMargin = width - hotAreaWidth;
        paramsTr.topMargin = 0;
        paramsTr.height = hotAreaHeight;
        paramsTr.width = hotAreaWidth;
        qTopRight.setLayoutParams(paramsTr);

        ImageView qBottomRight = (ImageView) findViewById(R.id.quadrantbr);
        RelativeLayout.LayoutParams paramsBr = (RelativeLayout.LayoutParams) qBottomRight.getLayoutParams();
        paramsBr.leftMargin = width - hotAreaWidth;
        paramsBr.topMargin = hotAreaHeight * 3;
        paramsBr.height = hotAreaHeight;
        paramsBr.width = hotAreaWidth;
        qBottomRight.setLayoutParams(paramsBr);

        ImageView qBottomLeft = (ImageView) findViewById(R.id.quadrantbl);
        RelativeLayout.LayoutParams paramsBl = (RelativeLayout.LayoutParams) qBottomLeft.getLayoutParams();
        paramsBl.leftMargin = 0;
        paramsBl.topMargin = hotAreaHeight * 3;
        paramsBl.height = hotAreaHeight;
        paramsBl.width = hotAreaWidth;
        qBottomLeft.setLayoutParams(paramsBl);

        Button scanDocButton = (Button) findViewById(R.id.scanDocButton);
        RelativeLayout.LayoutParams paramsBt = (RelativeLayout.LayoutParams) scanDocButton.getLayoutParams();
        paramsBt.leftMargin = width - width / 10 - width / 20;
        paramsBt.topMargin = height / 2 - width / 20;
        paramsBt.height = width / 10;
        paramsBt.width = width / 10;
        scanDocButton.setLayoutParams(paramsBt);

        scanDocButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                scanClicked = true;
            }
        });

        final Button colorModeButton = (Button) findViewById(R.id.colorModeButton);
        colorModeButton.setRotation(-90);

        colorModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                colorMode = !colorMode;
                v.setBackgroundTintList(ColorStateList.valueOf(colorMode ? 0xFFFFFFFF : 0x7FFFFFFF));

                // mOpenCvCameraView.setEffect((nativeMono&&!colorMode)?"mono":"none");
            }
        });

        final Button flashModeButton = (Button) findViewById(R.id.flashModeButton);
        flashModeButton.setRotation(-90);

        flashModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                flashMode = !flashMode;
                v.setBackgroundTintList(ColorStateList.valueOf(flashMode ? 0xFFFFFFFF : 0x7FFFFFFF));
                mOpenCvCameraView.setFlash(flashMode);
            }
        });


        final Button autoModeButton = (Button) findViewById(R.id.autoModeButton);
        autoModeButton.setRotation(-90);

        autoModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                autoMode = !autoMode;
                v.setBackgroundTintList(ColorStateList.valueOf(autoMode ? 0xFFFFFFFF : 0x7FFFFFFF));
            }
        });

        final Button galleryButton = (Button) findViewById(R.id.galleryButton);
        galleryButton.setRotation(-90);

        galleryButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext() , GalleryGridActivity.class);
                startActivity(intent);
            }
        });

    }

    private void checkResumePermissions() {
        if (ContextCompat.checkSelfPermission( this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    RESUME_PERMISSIONS_REQUEST_CAMERA);

        } else {
            enableCameraView();
        }
    }

    private void checkCreatePermissions() {

        if (ContextCompat.checkSelfPermission( this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE);

        }

    }


    public void turnCameraOn() {
        mOpenCvCameraView = (OpenNoteCameraView) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        haveCameraPermission = true;

    }

    public void enableCameraView() {
        if (mOpenCvCameraView == null) {
            turnCameraOn();
        }
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CREATE_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    turnCameraOn();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            case RESUME_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    enableCameraView();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    checkResumePermissions();
                }
                break;
                default: {
                    Log.d(TAG, "opencvstatus: "+status);
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };




    @Override
    public void onResume() {
        super.onResume();

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        Log.d(TAG, "resuming");

        flashMode=false;
        findViewById(R.id.colorModeButton).setBackgroundTintList(ColorStateList.valueOf(0x7FFFFFFF));

        for ( String build: Build.SUPPORTED_ABIS) {
            Log.d(TAG,"myBuild "+ build);
        }

        CustomOpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);

    }


    private OpenNoteCameraView mOpenCvCameraView;
    private Mat mIntermediateMat;
    private Mat mRgba;
    private Mat mGray;
    private Mat mCanned;
    private Mat mDocument = null;

    private boolean scanClicked = false;

    private boolean colorMode = false;
    double colorGain = 1.5;       // contrast
    double colorBias = 0;         // bright
    int colorThresh = 150;        // threshold

    private boolean autoMode = false;
    private boolean flashMode = false;

    private String currentQR = "";
    private boolean qrOk = false;

    private boolean nativeMono = false;

    private Point[] documentPoints = null;

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {

        mOpenCvCameraView.setMaxResolution();
        Camera.Size camResolution = mOpenCvCameraView.getResolution();

        int camHeight = camResolution.height;
        int camWidth = camResolution.width;

        mRgba = new Mat(camHeight, camWidth , CvType.CV_8UC4);
        mIntermediateMat = new Mat(camHeight, camWidth, CvType.CV_8UC4);
        mGray = new Mat(camHeight, camWidth, CvType.CV_8UC4);
        mCanned = new Mat(camHeight, camWidth, CvType.CV_8UC1);

        nativeMono = mOpenCvCameraView.isEffectSupported("mono");
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
        mGray.release();
        mCanned.release();

        // mOpenCvCameraView.setFlash(false);

    }

    private static boolean isMatch(String s, String pattern) {
        try {
            Pattern patt = Pattern.compile(pattern);
            Matcher matcher = patt.matcher(s);
            return matcher.matches();
        } catch (RuntimeException e) {
            return false;
        }
    }


    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Log.d(TAG,"received frame width: "+inputFrame.rgba().size().width);

        mRgba = inputFrame.rgba();

        Result[] results = {};

        if (autoMode) {
            try {
                results = zxing();
            } catch (ChecksumException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        qrOk = false;

        for (Result result: results) {
            String qrText = result.getText();
            if ( isMatch(qrText,"^P.. V.. S[0-9]+") && checkQR(currentQR)) {
                qrOk = true;
                currentQR = qrText;
                break;
            } else {
                Log.d(TAG,"qrcode ignored "+qrText);
            }
        }

        if ( qrOk || scanClicked ) {
            if (colorMode) {
                mRgba.copyTo(mIntermediateMat);
                // Imgproc.GaussianBlur(mRgba, mIntermediateMat, new Size(5, 5), 0);
            } else {
                Imgproc.cvtColor(mRgba, mIntermediateMat, Imgproc.COLOR_RGBA2GRAY, 4);
            }
        }

        int width = mRgba.width();
        int height = mRgba.height();

        for (Result result : results) {
            Log.d(TAG, "qrcode: " + result.getText());
            ResultPoint[] rp = result.getResultPoints();

            Point lpi = null;

            for (int i = 0; i < rp.length; i += 1) {
                Point pi = new Point();
                pi.y = rp[i].getY();
                pi.x = rp[i].getX() + width/2 + height/4;

                if (lpi != null) {
                    Imgproc.line(mRgba, lpi, pi, new Scalar(255, 0, 0), 10);
                }
                lpi = pi;
            }

        }

        if ( (qrOk || scanClicked ) && detectDocument()) {
            saveDocument();

            pageHistory.put(currentQR,new Date().getTime()/1000);

            Log.d(TAG,"qrcode scanned "+currentQR);

        }

        return mRgba;
    }

    private boolean checkQR(String qrCode) {

        return ! ( pageHistory.containsKey(qrCode) &&
                pageHistory.get(qrCode) > new Date().getTime()/1000-15) ;

    }

    private void saveDocument() {
        Intent intent = getIntent();
        String fileName = null;
        boolean isIntent = false;
        if (intent.getAction().equals("android.media.action.IMAGE_CAPTURE")) {
            Uri fileUri = ((Uri) intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT));
            fileName = fileUri.getPath();
            isIntent = true;
        } else {
            File folder = new File(Environment.getExternalStorageDirectory().toString()
                    + "/OpenNoteScanner");
            if (!folder.exists()) {
                folder.mkdir();
                Log.d(TAG, "wrote: created folder "+folder.getPath());
            }
            fileName = Environment.getExternalStorageDirectory().toString()
                    + "/OpenNoteScanner/DOC-"
                    + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
                    + ".jpg";
        }
        Mat endDoc = new Mat(new Double(mDocument.size().width).intValue(),
                new Double(mDocument.size().height).intValue(), CvType.CV_8UC4);

        Core.flip(mDocument.t(), endDoc, 1);

        if (colorMode) {
            Imgproc.cvtColor(endDoc, endDoc, Imgproc.COLOR_RGB2BGR, 4);
        } else {
            Imgproc.cvtColor(endDoc, endDoc, Imgproc.COLOR_GRAY2BGR, 4);
        }
        Imgcodecs.imwrite(fileName, endDoc);

        animateDocument(fileName);

        shootSound();
        Log.d(TAG, "wrote: " + fileName);
        endDoc.release();
        if (isIntent) {
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    class MyRunnable implements Runnable {

        public String fileName = null;
        public int top;
        public int left;
        public int width;
        public int height;

        public double hipotenuse( Point a , Point b) {
            return Math.sqrt( Math.pow(a.x - b.x , 2 ) + Math.pow(a.y - b.y , 2 ));
        };

        @Override
        public void run() {
            final ImageView imageView = (ImageView) findViewById(R.id.scannedAnimation);


            Bitmap bitmap = BitmapFactory.decodeFile(fileName);
            ExifInterface exif = null;
            int orientation = ExifInterface.ORIENTATION_UNDEFINED;
            try {
                exif = new ExifInterface(fileName);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    break;
                default:
                    matrix.postRotate(270);
                    break;
            }

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            imageView.setImageBitmap(bitmap);

            Display display = getWindowManager().getDefaultDisplay();
            android.graphics.Point size = new android.graphics.Point();
            display.getRealSize(size);
            int width = Math.max(size.x, size.y);
            int height = Math.min(size.x, size.y);

            Size imageSize = mRgba.size();
            double imageWidth = imageSize.width;
            double imageHeight = imageSize.height;

            double documentTopWidth = hipotenuse( documentPoints[0], documentPoints[1]);
            double documentLeftHeight = hipotenuse( documentPoints[0], documentPoints[3]);
            double documentRightHeight = hipotenuse( documentPoints[1], documentPoints[2]);
            double documentBottomWidth = hipotenuse( documentPoints[3], documentPoints[0]);

            double documentWidth = Math.max(documentTopWidth,documentBottomWidth);
            double documentHeight = Math.max(documentLeftHeight,documentRightHeight);

            Log.d(TAG, "device: "+width+"x"+height+" image: "+imageWidth+"x"+imageHeight+" document: "+documentWidth+"x"+documentHeight);


            Log.d(TAG, "documentPoints[0] x="+documentPoints[0].x+" y="+documentPoints[0].y);
            Log.d(TAG, "documentPoints[1] x="+documentPoints[1].x+" y="+documentPoints[1].y);
            Log.d(TAG, "documentPoints[2] x="+documentPoints[2].x+" y="+documentPoints[2].y);
            Log.d(TAG, "documentPoints[3] x="+documentPoints[3].x+" y="+documentPoints[3].y);


            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
            params.topMargin = (int) (documentPoints[0].y/imageHeight*height);
            params.leftMargin = (int) (documentPoints[0].x/imageWidth*width);
            params.width = (int) (documentWidth*width/imageWidth);
            params.height = (int) (documentHeight*height/imageHeight);

            imageView.setVisibility(View.VISIBLE);

            TranslateAnimation translateAnimation = new TranslateAnimation(
                    0, width, 0, height);

            ScaleAnimation scaleAnimation = new ScaleAnimation(1, 0, 1, 0);

            AnimationSet animationSet = new AnimationSet(true);

            animationSet.addAnimation(scaleAnimation);
            animationSet.addAnimation(translateAnimation);

            animationSet.setDuration(300);
            animationSet.setInterpolator(new AccelerateInterpolator());

            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    imageView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });


            imageView.startAnimation(animationSet);

        }
    }

    private void animateDocument(String filename) {

        MyRunnable runnable = new MyRunnable();
        runnable.fileName = filename;

        runOnUiThread(runnable);

    }

    private void shootSound()
    {
        AudioManager meng = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

        if (volume != 0)
        {
            if (_shootMP == null) {
                _shootMP = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            }
            if (_shootMP != null) {
                _shootMP.start();
            }
        }
    }

    private boolean detectDocument() {

        ArrayList<MatOfPoint> contours = findDocument(mRgba);

        documentPoints = null;

        int count = 0;
        for ( MatOfPoint c: contours ) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f , true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx , 0.02*peri , true );

            Point[] points = approx.toArray();

            // select biggest 4 angles polygon
            if ( points.length == 4 ) {
                ArrayList<MatOfPoint> lmp = new ArrayList<MatOfPoint>();

                documentPoints = sortPoints(points);

                if (insideArea(documentPoints,mRgba.size())) {

                    lmp.add(c);
                    Imgproc.drawContours(mRgba, lmp, -1, new Scalar(0, 255, 0), 5);

                    /* */
                    Log.d(TAG, points[0].toString() + " , " + points[1].toString() + " , " + points[2].toString() + " , " + points[3].toString());

                    fourPointTransform(mIntermediateMat, documentPoints);
                    enhanceDocument(mDocument);

                    scanClicked = false;
                    return true;
                }
            }
        }

        return false;

    }

    private boolean insideArea(Point[] rp, Size size) {

        int width = new Double(size.width).intValue();
        int height = new Double(size.height).intValue();
        int baseMeasure = height/4;

        int bottomPos = height-baseMeasure;
        int topPos = baseMeasure;
        int leftPos = width/2-baseMeasure;
        int rightPos = width/2+baseMeasure;

        return (
                   rp[0].x <= leftPos && rp[0].y <= topPos
                && rp[1].x >= rightPos && rp[1].y <= topPos
                && rp[2].x >= rightPos && rp[2].y >= bottomPos
                && rp[3].x <= leftPos && rp[3].y >= bottomPos

                );
    }


    private void enhanceDocument( Mat src ) {
        if (colorMode) {
            src.convertTo(src,-1, colorGain , colorBias);
            Imgproc.threshold(src, src, colorThresh, 255, Imgproc.THRESH_BINARY);
        } else {
            Imgproc.adaptiveThreshold(src,src,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,15,15);
        }
    }

    private void fourPointTransform( Mat src , Point[] pts ) {
        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);
        int maxWidth = new Double(dw).intValue();


        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);
        int maxHeight = new Double(dh).intValue();

        if (mDocument != null) {
            mDocument.release();
            mDocument = null;
        }

        mDocument = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, mDocument, m, mDocument.size());

    }

    private void previewDocument() {
        Mat docCorner=null;
        Size s = mDocument.size();
        try {
            docCorner = mRgba.submat(0, new Double(s.height).intValue(), 0, new Double(s.width).intValue());
            mDocument.copyTo(docCorner);
        } catch (CvException e) {
            Log.d(TAG, "stacktrace: " + e.getStackTrace().toString());
        };

        if (docCorner != null)
            docCorner.release();

    }

    private Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<Point>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return new Double(lhs.y + lhs.x).compareTo(new Double(rhs.y + rhs.x));
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return new Double(lhs.y - lhs.x).compareTo(new Double(rhs.y - rhs.x));
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private ArrayList<MatOfPoint> findDocument(Mat src) {

        Imgproc.cvtColor(src, mGray, Imgproc.COLOR_RGBA2GRAY, 4);
        Imgproc.Canny(mGray, mCanned, 75, 200);

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(mCanned, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        hierarchy.release();

        Collections.sort(contours, new Comparator<MatOfPoint>() {

            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return new Double(Imgproc.contourArea( rhs )).compareTo(new Double(Imgproc.contourArea(lhs)));
            }
        });

        return contours;
    }

    private QRCodeMultiReader qrCodeMultiReader = new QRCodeMultiReader();

    public Result[] zxing() throws ChecksumException, FormatException {

        int w = mRgba.width();
        int h = mRgba.height();

        Mat southEast = mRgba.submat(0, h/4, w/2 + h/4, w);

        Bitmap bMap = Bitmap.createBitmap(southEast.width(), southEast.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(southEast, bMap);
        southEast.release();
        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result result = null;
        ResultPoint[] rps = null;

        Result[] results = {};
        try {
            results = qrCodeMultiReader.decodeMultiple(bitmap);
        }
        catch (NotFoundException e) {
        }

        return results;

    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return false;
    }
}
