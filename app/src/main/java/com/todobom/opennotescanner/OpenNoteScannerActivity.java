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
import android.os.HandlerThread;
import android.os.Message;
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
import android.widget.Toast;

import com.todobom.opennotescanner.helpers.CustomOpenCVLoader;
import com.todobom.opennotescanner.helpers.OpenNoteMessage;
import com.todobom.opennotescanner.helpers.PreviewFrame;
import com.todobom.opennotescanner.helpers.ScannedDocument;
import com.todobom.opennotescanner.views.OpenNoteCameraView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class OpenNoteScannerActivity extends Activity
        implements NavigationView.OnNavigationItemSelectedListener , CameraBridgeViewBase.CvCameraViewListener2 , Camera.PictureCallback {
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

    private boolean safeToTakePicture;
    private Button scanDocButton;
    private Point[] previewPoints;
    private Size previewSize;
    private HandlerThread mImageThread;
    private ImageProcessor mImageProcessor;

    public boolean isImageProcessorBusy() {
        return imageProcessorBusy;
    }

    public void setImageProcessorBusy(boolean imageProcessorBusy) {
        this.imageProcessorBusy = imageProcessorBusy;
    }

    private boolean imageProcessorBusy=true;

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

        scanDocButton = (Button) findViewById(R.id.scanDocButton);
        RelativeLayout.LayoutParams paramsBt = (RelativeLayout.LayoutParams) scanDocButton.getLayoutParams();
        paramsBt.leftMargin = width - width / 10 - width / 20;
        paramsBt.topMargin = height / 2 - width / 20;
        paramsBt.height = width / 10;
        paramsBt.width = width / 10;

        scanDocButton.setLayoutParams(paramsBt);

        scanDocButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (scanClicked) {
                    requestPicture();
                    scanDocButton.setBackgroundTintList(ColorStateList.valueOf(0xFF00FFFF));

                } else {
                    scanClicked = true;
                    Toast.makeText(getApplicationContext(), R.string.scanningToast, Toast.LENGTH_LONG).show();
                    v.setBackgroundTintList(ColorStateList.valueOf(0x7FFF00FF));
                }
            }
        });

        final Button colorModeButton = (Button) findViewById(R.id.colorModeButton);
        colorModeButton.setRotation(-90);

        colorModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                colorMode = !colorMode;
                v.setBackgroundTintList(ColorStateList.valueOf(colorMode ? 0xFFFFFFFF : 0x7FFFFFFF));

                sendImageProcessorMessage("colorMode" , colorMode );

                Toast.makeText(getApplicationContext(), colorMode?R.string.colorMode:R.string.bwMode, Toast.LENGTH_SHORT).show();

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
                Toast.makeText(getApplicationContext(), autoMode?R.string.autoMode:R.string.manualMode, Toast.LENGTH_SHORT).show();
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
                }
                break;
            }

            case RESUME_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    enableCameraView();
                }
                break;
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

        if (mImageThread == null ) {
            mImageThread = new HandlerThread("Worker Thread");
            mImageThread.start();
        }

        if (mImageProcessor == null) {
            mImageProcessor = new ImageProcessor(mImageThread.getLooper(), new Handler() , this);
            imageProcessorBusy = false;
        }

    }


    private OpenNoteCameraView mOpenCvCameraView;
    private Mat mIntermediateMat;
    private Mat mRgba;
    private Mat mGray;
    private Mat mCanned;
    private Mat mDocument = null;

    private boolean scanClicked = false;
    private boolean scanDoubleClicked = false;

    private boolean colorMode = false;
    double colorGain = 1.5;       // contrast
    double colorBias = 0;         // bright
    int colorThresh = 150;        // threshold

    private boolean autoMode = false;
    private boolean flashMode = false;

    private String currentQR = "";
    private boolean qrOk = false;

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

        mOpenCvCameraView.setMaxPreviewResolution();
        mOpenCvCameraView.setMaxPictureResolution();
        Camera.Size camResolution = mOpenCvCameraView.getResolution();

        int camHeight = camResolution.height;
        int camWidth = camResolution.width;

        mRgba = new Mat(camHeight, camWidth , CvType.CV_8UC4);
        mIntermediateMat = new Mat(camHeight, camWidth, CvType.CV_8UC4);
        mGray = new Mat(camHeight, camWidth, CvType.CV_8UC4);
        mCanned = new Mat(camHeight, camWidth, CvType.CV_8UC1);

        safeToTakePicture = true;

    }

    public void onCameraViewStopped() {
        mRgba.release();
        mIntermediateMat.release();
        mGray.release();
        mCanned.release();

        // mOpenCvCameraView.setFlash(false);

    }

    private class ResetShutterColor implements Runnable {
        @Override
        public void run() {
            scanDocButton.setBackgroundTintList(ColorStateList.valueOf(0xFF00FFFF));
        }
    }

    private ResetShutterColor resetShutterColor = new ResetShutterColor();

    public boolean requestPicture() {
        if (safeToTakePicture) {
            runOnUiThread(resetShutterColor);
            safeToTakePicture = false;
            mOpenCvCameraView.takePicture(this);
            return true;
        }
        return false;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        android.hardware.Camera.Size pictureSize = camera.getParameters().getPictureSize();

        Log.d(TAG, "onPictureTaken - received image " + pictureSize.width + "x" + pictureSize.height);

        Mat mat = new Mat(new Size(pictureSize.width, pictureSize.height), CvType.CV_8U);
        mat.put(0, 0, data);

        imageProcessorBusy = true;
        sendImageProcessorMessage("pictureTaken", mat);

        shootSound();

        // restart preview
        camera.startPreview();
        camera.setPreviewCallback(mOpenCvCameraView);

        scanClicked = false;
        safeToTakePicture = true;

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Log.d(TAG,"received frame width: "+inputFrame.rgba().size().width);

        mRgba = inputFrame.rgba();

        if ( ! imageProcessorBusy && ( autoMode || scanClicked ) )  {
            imageProcessorBusy = true;
            Mat copyFrame = new Mat(mRgba.size(), CvType.CV_8UC4);
            mRgba.copyTo(copyFrame);
            sendImageProcessorMessage("previewFrame", new PreviewFrame( copyFrame , autoMode ) );
        }
        return mRgba;
    }

    public void sendImageProcessorMessage(String messageText , Object obj ) {
        Log.d(TAG,"sending message to ImageProcessor: "+messageText+" - "+obj.toString());
        Message msg = mImageProcessor.obtainMessage();
        msg.obj = new OpenNoteMessage(messageText, obj );
        mImageProcessor.sendMessage(msg);
    }

    public void saveDocument(ScannedDocument scannedDocument) {

        Mat doc = (scannedDocument.processed != null) ? scannedDocument.processed : scannedDocument.original;

        Intent intent = getIntent();
        String fileName;
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
        Mat endDoc = new Mat(Double.valueOf(doc.size().width).intValue(),
                Double.valueOf(doc.size().height).intValue(), CvType.CV_8UC4);

        Core.flip(doc.t(), endDoc, 1);

        /*
        if (colorMode) {
            Imgproc.cvtColor(endDoc, endDoc, Imgproc.COLOR_RGB2BGR, 4);
        } else {
            Imgproc.cvtColor(endDoc, endDoc, Imgproc.COLOR_GRAY2BGR, 4);
        }
        */

        Imgcodecs.imwrite(fileName, endDoc);

        animateDocument(fileName,scannedDocument);

        Log.d(TAG, "wrote: " + fileName);
        endDoc.release();
        if (isIntent) {
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    class AnimationRunnable implements Runnable {

        private Size imageSize;
        private Point[] previewPoints =null;
        public Size previewSize = null;
        public String fileName = null;
        public int width;
        public int height;

        public AnimationRunnable(String filename, ScannedDocument document) {
            this.fileName = filename;
            this.imageSize = document.processed.size();

            if (document.quadrilateral != null) {
                this.previewPoints = document.previewPoints;
                this.previewSize = document.previewSize;
            }
        }

        public double hipotenuse( Point a , Point b) {
            return Math.sqrt( Math.pow(a.x - b.x , 2 ) + Math.pow(a.y - b.y , 2 ));
        }

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

            double imageWidth = imageSize.width;
            double imageHeight = imageSize.height;

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();

            if (previewPoints != null) {
                double documentTopWidth = hipotenuse(previewPoints[0], previewPoints[1]);
                double documentLeftHeight = hipotenuse(previewPoints[0], previewPoints[3]);
                double documentRightHeight = hipotenuse(previewPoints[1], previewPoints[2]);
                double documentBottomWidth = hipotenuse(previewPoints[3], previewPoints[0]);

                double documentWidth = Math.max(documentTopWidth, documentBottomWidth);
                double documentHeight = Math.max(documentLeftHeight, documentRightHeight);

                Log.d(TAG, "device: " + width + "x" + height + " image: " + imageWidth + "x" + imageHeight + " document: " + documentWidth + "x" + documentHeight);


                Log.d(TAG, "previewPoints[0] x=" + previewPoints[0].x + " y=" + previewPoints[0].y);
                Log.d(TAG, "previewPoints[1] x=" + previewPoints[1].x + " y=" + previewPoints[1].y);
                Log.d(TAG, "previewPoints[2] x=" + previewPoints[2].x + " y=" + previewPoints[2].y);
                Log.d(TAG, "previewPoints[3] x=" + previewPoints[3].x + " y=" + previewPoints[3].y);

                double xratio = width / previewSize.width;
                double yratio = height / previewSize.height;

                params.topMargin = (int) (previewPoints[0].y * yratio);
                params.leftMargin = (int) (previewPoints[0].x * xratio);
                params.width = (int) (documentWidth * xratio);
                params.height = (int) (documentHeight * yratio);
            } else {
                params.topMargin = 0;
                params.leftMargin = 0;
                params.width = width;
                params.height = height;
            }

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

    private void animateDocument(String filename, ScannedDocument quadrilateral) {

        AnimationRunnable runnable = new AnimationRunnable(filename,quadrilateral);
        runOnUiThread(runnable);

    }

    private void shootSound()
    {
        AudioManager meng = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

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



    private void previewDocument() {
        Mat docCorner=null;
        Size s = mDocument.size();
        try {
            docCorner = mRgba.submat(0, Double.valueOf(s.height).intValue(), 0, Double.valueOf(s.width).intValue());
            mDocument.copyTo(docCorner);
        } catch (CvException e) {
            Log.d(TAG, "stacktrace: " + Arrays.toString(e.getStackTrace()));
        };

        if (docCorner != null)
            docCorner.release();

    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return false;
    }
}
