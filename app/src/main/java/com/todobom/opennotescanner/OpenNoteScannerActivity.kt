package com.todobom.opennotescanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.*
import android.hardware.Camera.*
import android.media.AudioManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.permissionx.guolindev.PermissionX
import com.todobom.opennotescanner.helpers.*
import com.todobom.opennotescanner.helpers.ScanTopicDialogFragment.SetTopicDialogListener
import com.todobom.opennotescanner.views.HUDCanvasView
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfInt
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class OpenNoteScannerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        SurfaceHolder.Callback, PictureCallback, PreviewCallback, SetTopicDialogListener, SensorEventListener {
    var mDocumentAspectRatio: Double = 0.0
    private val mHideHandler = Handler()
    private lateinit var mContentView: View
    private val mHidePart2Runnable = Runnable { // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
    private lateinit var mControlsView: View
    private val mShowPart2Runnable = Runnable { // Delayed display of UI elements
        val actionBar = actionBar
        actionBar?.show()
        mControlsView.visibility = View.VISIBLE
    }
    private var mVisible = false
    private val mHideRunnable = Runnable { hide() }
    private var mediaActionSound: MediaActionSound? = null
    private var safeToTakePicture = false
    private lateinit var scanDocButton: Button
    private lateinit var mImageThread: HandlerThread
    private lateinit var mImageProcessor: ImageProcessor
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null
    private lateinit var mThis: OpenNoteScannerActivity
    private var mFocused = false
    var hUD: HUDCanvasView? = null
        private set
    private lateinit var mWaitSpinner: View
    private lateinit var mFabToolbar: FABToolbarLayout
    private var mBugRotate = false
    private lateinit var mSharedPref: SharedPreferences
    private val mDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss")
    private var scanTopic: String? = null
    private var mat: Mat? = null
    private lateinit var tracker: Tracker

    private lateinit var sensorManager: SensorManager
    lateinit var accelerometerReading: FloatArray
    lateinit var magnetometerReading: FloatArray

    fun setImageProcessorBusy(imageProcessorBusy: Boolean) {
        this.imageProcessorBusy = imageProcessorBusy
    }

    fun setAttemptToFocus(attemptToFocus: Boolean) {
        this.attemptToFocus = attemptToFocus
    }

    private var imageProcessorBusy = true
    private var attemptToFocus = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accelerometerReading = FloatArray(3);
        magnetometerReading = FloatArray(3);

        mThis = this
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        if (mSharedPref.getBoolean("isFirstRun", true) && !mSharedPref.getBoolean("usage_stats", false)) {
            statsOptInDialog()
        }
        tracker = (application as OpenNoteScannerApplication).tracker
        TrackHelper.track().screen("/OpenNoteScannerActivity").title("Main Screen").with(tracker)
        setContentView(R.layout.activity_open_note_scanner)
        mVisible = true
        mControlsView = findViewById(R.id.fullscreen_content_controls)
        mContentView = findViewById(R.id.surfaceView)
        hUD = findViewById(R.id.hud)
        mWaitSpinner = findViewById(R.id.wait_spinner)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener { toggle() }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        scanDocButton = findViewById<Button>(R.id.scanDocButton)
        scanDocButton.setOnClickListener { v: View ->
            if (scanClicked) {
                requestPicture()
                scanDocButton.backgroundTintList = null
                waitSpinnerVisible()
            } else {
                scanClicked = true
                Toast.makeText(applicationContext, R.string.scanningToast, Toast.LENGTH_LONG).show()
                v.backgroundTintList = ColorStateList.valueOf(0x7F60FF60)
            }
        }
        val colorModeButton = findViewById<ImageView>(R.id.colorModeButton)
        colorModeButton.setOnClickListener { v: View ->
            colorMode = !colorMode
            (v as ImageView).setColorFilter(if (colorMode) -0x1 else -0x5f0f60)
            sendImageProcessorMessage("colorMode", colorMode)
            Toast.makeText(applicationContext, if (colorMode) R.string.colorMode else R.string.bwMode, Toast.LENGTH_SHORT).show()
        }
        val filterModeButton = findViewById<ImageView>(R.id.filterModeButton)
        filterModeButton.setOnClickListener { v: View ->
            filterMode = !filterMode
            (v as ImageView).setColorFilter(if (filterMode) -0x1 else -0x5f0f60)
            sendImageProcessorMessage("filterMode", filterMode)
            Toast.makeText(applicationContext, if (filterMode) R.string.filterModeOn else R.string.filterModeOff, Toast.LENGTH_SHORT).show()
        }
        val flashModeButton = findViewById<ImageView>(R.id.flashModeButton)
        flashModeButton.setOnClickListener { v: View ->
            mFlashMode = setFlash(!mFlashMode)
            (v as ImageView).setColorFilter(if (mFlashMode) -0x1 else -0x5f0f60)
        }
        val autoModeButton = findViewById<ImageView>(R.id.autoModeButton)
        autoModeButton.setOnClickListener { v: View ->
            autoMode = !autoMode
            (v as ImageView).setColorFilter(if (autoMode) -0x1 else -0x5f0f60)
            Toast.makeText(applicationContext, if (autoMode) R.string.autoMode else R.string.manualMode, Toast.LENGTH_SHORT).show()
        }
        val settingsButton = findViewById<ImageView>(R.id.settingsButton)
        settingsButton.setOnClickListener { v: View ->
            val intent = Intent(v.context, SettingsActivity::class.java)
            startActivity(intent)
        }
        val galleryButton = findViewById<FloatingActionButton>(R.id.galleryButton)
        galleryButton.setOnClickListener { v: View ->
            val intent = Intent(v.context, GalleryGridActivity::class.java)
            startActivity(intent)
        }
        mFabToolbar = findViewById(R.id.fabtoolbar)
        val fabToolbarButton = findViewById<FloatingActionButton>(R.id.fabtoolbar_fab)
        fabToolbarButton.setOnClickListener { mFabToolbar.show() }
        findViewById<View>(R.id.hideToolbarButton).setOnClickListener { mFabToolbar.hide() }
    }

    fun setFlash(stateFlash: Boolean): Boolean {
        val camera = mCamera ?: return false

        val flashModes = camera.parameters.supportedFlashModes
        if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            val parameters = camera.parameters
            parameters.flashMode = if (stateFlash) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
            camera.parameters = parameters
            Log.d(TAG, "flash: " + if (stateFlash) "on" else "off")
            return stateFlash
        }

        Log.d(TAG, "flash not available")
        return false
    }

    private fun grantPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // new version will use MediaStore or SAF, does not need permissions
            listOf(Manifest.permission.CAMERA)
        } else {
            // TODO: can we move this to use SAF / MediaStore too ?
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }

        PermissionX.init(this)
            .permissions(permissionsToRequest)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, getString(R.string.permission_explain_request_reason_all), getString(R.string.ok))
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(deniedList, getString(R.string.permission_forward_reason_all), getString(R.string.ok))
            }
            .explainReasonBeforeRequest()
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    enableCameraView()
                } else {
                    // PermissionX will always prompt or redirect to settings if permissions are not granted.
                }
            }
    }

    fun turnCameraOn() {
        mSurfaceView = findViewById<SurfaceView>(R.id.surfaceView).also { surfaceView ->
            mSurfaceHolder = surfaceView.holder.also { holder ->
                holder.addCallback(this)
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
            }
            surfaceView.visibility = SurfaceView.VISIBLE
        }
    }

    fun enableCameraView() {
        if (mSurfaceView == null) {
            turnCameraOn()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        val actionBar = actionBar
        actionBar?.hide()
        mControlsView.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    @SuppressLint("InlinedApi")
    private fun show() {
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    public override fun onResume() {
        super.onResume()

        grantPermissions()

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                    this,
                    magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
            )
        }

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        Log.d(TAG, "resuming")
        for (build in Build.SUPPORTED_ABIS) {
            Log.d(TAG, "myBuild $build")
        }

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        //TODO these should go in the variable's creation
        mImageThread = HandlerThread("Worker Thread")
        mImageThread.start()
        mImageProcessor = ImageProcessor(mImageThread.looper, this)
        setImageProcessorBusy(false)
    }

    fun waitSpinnerVisible() {
        runOnUiThread { mWaitSpinner.visibility = View.VISIBLE }
    }

    fun waitSpinnerInvisible() {
        runOnUiThread { mWaitSpinner.visibility = View.GONE }
    }

    private var mSurfaceView: SurfaceView? = null
    private var scanClicked = false
    private var colorMode = false
    private var filterMode = true
    private var autoMode = false
    private var mFlashMode = false
    public override fun onPause() {
        super.onPause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        // FIXME: check disableView()

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)

        mediaActionSound?.release()
        mediaActionSound = null
    }

    val resolutionList: List<Camera.Size>
        get() {
            val camera = mCamera ?: return emptyList()
            return camera.parameters.supportedPreviewSizes
        }
    val maxPreviewResolution: Camera.Size?
        get() {
            val camera = mCamera ?: return null

            var maxWidth = 0
            var curRes: Camera.Size? = null
            camera.lock()
            for (r in resolutionList) {
                if (r.width > maxWidth) {
                    Log.d(TAG, "supported preview resolution: " + r.width + "x" + r.height)
                    maxWidth = r.width
                    curRes = r
                }
            }
            return curRes
        }
    val pictureResolutionList: List<Camera.Size>
        get() {
            val camera = mCamera ?: return emptyList()
            return camera.parameters.supportedPictureSizes
        }

    fun getMaxPictureResolution(previewRatio: Float): Camera.Size? {
        var maxPixels = 0
        var ratioMaxPixels = 0
        var currentMaxRes: Camera.Size? = null
        var ratioCurrentMaxRes: Camera.Size? = null
        for (r in pictureResolutionList) {
            val pictureRatio = r.width.toFloat() / r.height
            Log.d(TAG, "supported picture resolution: " + r.width + "x" + r.height + " ratio: " + pictureRatio)
            val resolutionPixels = r.width * r.height
            if (resolutionPixels > ratioMaxPixels && pictureRatio == previewRatio) {
                ratioMaxPixels = resolutionPixels
                ratioCurrentMaxRes = r
            }
            if (resolutionPixels > maxPixels) {
                maxPixels = resolutionPixels
                currentMaxRes = r
            }
        }
        val matchAspect = mSharedPref.getBoolean("match_aspect", true)
        if (ratioCurrentMaxRes != null && matchAspect) {
            Log.d(TAG, "Max supported picture resolution with preview aspect ratio: "
                    + ratioCurrentMaxRes.width + "x" + ratioCurrentMaxRes.height)
            return ratioCurrentMaxRes
        }
        return currentMaxRes
    }

    private fun findBestCamera(): Int {
        var cameraId = -1
        //Search for the back facing camera
        //get the number of cameras
        val numberOfCameras = Camera.getNumberOfCameras()
        //for every camera check
        Log.i(TAG, "Number of available cameras: $numberOfCameras")
        for (i in 0 until numberOfCameras) {
            val info = CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i
                break
            }
            cameraId = i
        }
        return cameraId
    }

    fun setFocusParameters() {
        val camera = mCamera ?: return

        val parameters = camera.parameters
        val supportedFocusModes = camera.parameters.getSupportedFocusModes()
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        } else if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            // fallback
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            val targetFocusRect = Rect(-500, -500, 500, 500)
            val focusList: MutableList<Camera.Area> = ArrayList()
            val focusArea = Camera.Area(targetFocusRect, 1000)
            focusList.add(focusArea)
            parameters.focusAreas = focusList
            parameters.meteringAreas = focusList
        } else {
            mFocused = true
            Log.d(TAG, "autofocus not available")
            return
        }

        try {
            camera.setAutoFocusMoveCallback { start, _ ->
                mFocused = !start
                Log.d(TAG, "focusMoving: $mFocused")
            }
        } catch (e: Exception) {
            Log.d(TAG, "failed setting AutoFocusMoveCallback")
        }

        Log.d(TAG, "enabling autofocus")
        camera.parameters = parameters
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val camera = try {
            val cameraId = findBestCamera()
            Camera.open(cameraId) as Camera
        } catch (e: RuntimeException) {
            return
        }
        mCamera = camera

        val param  = camera.getParameters()
        param.pictureFormat = ImageFormat.JPEG
        val pSize = maxPreviewResolution
        param.setPreviewSize(pSize!!.width, pSize.height)
        val previewRatio = pSize.width.toFloat() / pSize.height
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        val displayWidth = Math.min(size.y, size.x)
        val displayHeight = Math.max(size.y, size.x)
        val displayRatio = displayHeight.toFloat() / displayWidth
        var previewHeight = displayHeight
        val surfaceView = mSurfaceView
        val ud = hUD
        if (displayRatio > previewRatio && surfaceView != null && ud != null) {
            val surfaceParams = surfaceView.layoutParams
            previewHeight = (size.y.toFloat() / displayRatio * previewRatio).toInt()
            surfaceParams.height = previewHeight
            surfaceView.layoutParams = surfaceParams
            ud.layoutParams.height = previewHeight
        }
        val hotAreaSpaceWidth: Int
        val hotAreaSpaceHeight: Int

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val docPageFormat: String? = sharedPref.getString("document_page_format", "0")
        this.mDocumentAspectRatio = 0.0
        if (docPageFormat == "0.0001") {
            val customPageWidth: Float = sharedPref.getString("custom_pageformat_width", "0")!!.toFloat()
            val customPageHeight: Float = sharedPref.getString("custom_pageformat_height", "0")!!.toFloat()
            if (customPageWidth > 0 && customPageHeight > 0) {
                this.mDocumentAspectRatio = (customPageHeight / customPageWidth).toDouble()
            }
        } else {
            this.mDocumentAspectRatio = docPageFormat!!.toFloat().toDouble()
        }

        val hotArea = Utils.getHotArea(pSize.width, pSize.height, this)

        hotAreaSpaceWidth = hotArea!![1]
        hotAreaSpaceHeight = hotArea!![0]

        val angleNorthWest = findViewById<ImageView>(R.id.nw_angle)
        val paramsNW = angleNorthWest.layoutParams as RelativeLayout.LayoutParams
        paramsNW.leftMargin = hotAreaSpaceWidth - paramsNW.width
        paramsNW.topMargin = hotAreaSpaceHeight - paramsNW.height
        angleNorthWest.layoutParams = paramsNW
        val angleNorthEast = findViewById<ImageView>(R.id.ne_angle)
        val paramsNE = angleNorthEast.layoutParams as RelativeLayout.LayoutParams
        paramsNE.leftMargin = displayWidth - hotAreaSpaceWidth
        paramsNE.topMargin = hotAreaSpaceHeight - paramsNE.height
        angleNorthEast.layoutParams = paramsNE
        val angleSouthEast = findViewById<ImageView>(R.id.se_angle)
        val paramsSE = angleSouthEast.layoutParams as RelativeLayout.LayoutParams
        paramsSE.leftMargin = displayWidth - hotAreaSpaceWidth
        paramsSE.topMargin = previewHeight - hotAreaSpaceHeight
        angleSouthEast.layoutParams = paramsSE
        val angleSouthWest = findViewById<ImageView>(R.id.sw_angle)
        val paramsSW = angleSouthWest.layoutParams as RelativeLayout.LayoutParams
        paramsSW.leftMargin = hotAreaSpaceWidth - paramsSW.width
        paramsSW.topMargin = previewHeight - hotAreaSpaceHeight
        angleSouthWest.layoutParams = paramsSW
        val maxRes = getMaxPictureResolution(previewRatio)
        if (maxRes != null) {
            param.setPictureSize(maxRes.width, maxRes.height)
            Log.d(TAG, "max supported picture resolution: " + maxRes.width + "x" + maxRes.height)
        }

        camera.setParameters(param)
        mBugRotate = mSharedPref.getBoolean("bug_rotate", false)
        if (mBugRotate) {
            camera.setDisplayOrientation(270)
        } else {
            camera.setDisplayOrientation(90)
        }
        mImageProcessor.setBugRotate(mBugRotate)

        setFocusParameters()

        // some devices doesn't call the AutoFocusMoveCallback - fake the
        // focus to true at the start
        mFocused = true
        safeToTakePicture = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        refreshCamera()
    }

    private fun refreshCamera() {
        val camera = mCamera ?: return

        try {
            camera.stopPreview()
        } catch (e: Exception) {
        }
        try {
            camera.setPreviewDisplay(mSurfaceHolder)
            camera.startPreview()
            camera.setPreviewCallback(this)
        } catch (e: Exception) {
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val camera = mCamera ?: return
        camera.stopPreview()
        camera.setPreviewCallback(null)
        camera.release()
        mCamera = null
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        val pictureSize = camera.parameters.previewSize
        Log.v(TAG, "onPreviewFrame - received image " + pictureSize.width + "x" + pictureSize.height
                + " focused: " + mFocused + " imageprocessor: " + if (imageProcessorBusy) "busy" else "available")
        if (mFocused && !imageProcessorBusy) {
            setImageProcessorBusy(true)
            val yuv = Mat(Size(pictureSize.width.toDouble(), pictureSize.height * 1.5), CvType.CV_8UC1)
            yuv.put(0, 0, data)
            val mat = Mat(Size(pictureSize.width.toDouble(), pictureSize.height.toDouble()), CvType.CV_8UC4)
            Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2RGBA_NV21, 4)
            yuv.release()
            sendImageProcessorMessage("previewFrame", PreviewFrame(mat, autoMode, !(autoMode || scanClicked)))
        }
    }

    fun invalidateHUD() {
        runOnUiThread { hUD!!.invalidate() }
    }

    private inner class ResetShutterColor : Runnable {
        override fun run() {
            scanDocButton.backgroundTintList = null
        }
    }

    private val resetShutterColor = ResetShutterColor()
    fun requestPicture(): Boolean {
        val camera = mCamera ?: return false

        if (safeToTakePicture) {
            runOnUiThread(resetShutterColor)
            safeToTakePicture = false
            try {
                camera.takePicture(null, null, mThis)
            }  catch (_: java.lang.Exception) {
                Log.e(TAG, "failed to take picture")
            }
            return true
        }
        return false
    }

    override fun onPictureTaken(data: ByteArray, camera: Camera) {
        shootSound()
        setFocusParameters()
        Log.d(TAG, "onPictureTaken - received ${data.size} bytes")

        val encodedMat = MatOfByte(*data) // or MatOfByte(data)
        val decodedMat: Mat
        try {
            decodedMat = Imgcodecs.imdecode(encodedMat, Imgcodecs.IMREAD_UNCHANGED)
            if (decodedMat.empty()) {
                Log.e(TAG, "Failed to decode image from data byte array.")
                refreshCamera() // Or some other error recovery
                safeToTakePicture = true
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while decoding image: ${e.message}")
            refreshCamera()
            safeToTakePicture = true
            return
        } finally {
            encodedMat.release() // Release the temporary MatOfByte
        }

        Log.d(TAG, "Decoded image: ${decodedMat.width()}x${decodedMat.height()}, type: ${CvType.typeToString(decodedMat.type())}")

        // Store the decoded Mat to be used by issueProcessingOfTakenPicture
        mat = decodedMat

        if (mSharedPref.getBoolean("custom_scan_topic", false)) {
            val fm = supportFragmentManager
            val scanTopicDialogFragment = ScanTopicDialogFragment()
            scanTopicDialogFragment.show(fm, getString(R.string.scan_topic_dialog_title))
            // Note: if custom_scan_topic is true, issueProcessingOfTakenPicture
            // will be called later by onFinishTopicDialog. 'this.mat' must hold the decoded image.
        } else {
            issueProcessingOfTakenPicture()
        }
    }

    override fun onFinishTopicDialog(inputText: String?) {
        scanTopic = inputText
        issueProcessingOfTakenPicture()
    }

    private fun issueProcessingOfTakenPicture() {
        setImageProcessorBusy(true)
        sendImageProcessorMessage("pictureTaken", mat)
        scanClicked = false
        safeToTakePicture = true
    }

    fun sendImageProcessorMessage(messageText: String, obj: Any?) {
        Log.v(TAG, "sending message to ImageProcessor: $messageText - $obj")
        val msg = mImageProcessor.obtainMessage()
        msg.obj = OpenNoteMessage(messageText, obj)
        mImageProcessor.sendMessage(msg)
    }

    fun saveDocument(scannedDocument: ScannedDocument) {
        val doc = scannedDocument.processed ?: scannedDocument.original

        val intent = intent
        val isIntentCapture = intent.action == "android.media.action.IMAGE_CAPTURE"
        val outputUriFromIntent = if (isIntentCapture) {
            intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
        } else {
            null
        }

        val imageSuffix = if (mSharedPref.getBoolean("save_png", false)) ".png" else ".jpg"
        val mimeType = if (imageSuffix == ".png") "image/png" else "image/jpeg"

        val encodingParams = MatOfInt()
        if (imageSuffix == ".jpg") {
            encodingParams.fromArray(Imgcodecs.IMWRITE_JPEG_QUALITY, mSharedPref.getInt("jpeg_quality", 95)) // Example: get quality from prefs
        } else {
            encodingParams.fromArray(Imgcodecs.IMWRITE_PNG_COMPRESSION, mSharedPref.getInt("png_compression", 6)) // Example: get compression from prefs
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        var displayName = "DOC-$timeStamp$imageSuffix"
        if (scanTopic != null) {
            displayName = "$scanTopic-$displayName"
        }
        val customFolderName = mSharedPref.getString("storage_folder", "OpenNoteScanner") ?: "OpenNoteScanner"

        var savedFileUri: Uri? = null
        var preQFilePath: String? = null // pre android Q file path

        try {
            // if intent has no target uri, we just handle it as if it was a normal document scanned from the app
            if (isIntentCapture && outputUriFromIntent != null) {
                // this does not create any MediaStore entries, caller has to do that
                // I also need to find the use case of calling the app via an intent, to better understand how to handle this case
                savedFileUri = outputUriFromIntent
            } else {
                // Saving to gallery (MediaStore)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending until written
                    if (customFolderName.isNotBlank()) {
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + customFolderName)
                    } else {
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    savedFileUri = contentResolver.insert(imageCollection, contentValues)
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val targetDir = if (customFolderName.isNotBlank()) {
                        File(picturesDir, customFolderName)
                    } else {
                        picturesDir
                    }

                    if (!targetDir.exists()) {
                        if (!targetDir.mkdirs()) {
                            Log.e(TAG, "Failed to create directory: ${targetDir.absolutePath}")
                            // Fallback to default pictures directory if folder creation fails
                            preQFilePath = File(picturesDir, displayName).absolutePath
                        } else {
                            preQFilePath = File(targetDir, displayName).absolutePath
                        }
                    } else {
                        preQFilePath = File(targetDir, displayName).absolutePath
                    }
                    contentValues.put(MediaStore.Images.Media.DATA, preQFilePath)
                    // For pre-Q, insert into the legacy external content URI
                    savedFileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                }
            }

            if (savedFileUri == null) {
                // insert failed
                if (isIntentCapture) {
                    setResult(RESULT_CANCELED)
                    finish()
                }
                return
            }

            savedFileUri.let { uri ->
                contentResolver.openOutputStream(uri)?.use { out ->
                    val endDoc = Mat()
                    Core.flip(doc.t(), endDoc, 1)

                    // Convert Mat to byte array
                    val matOfByte = org.opencv.core.MatOfByte()
                    val successEncode = Imgcodecs.imencode(imageSuffix, endDoc, matOfByte, encodingParams)
                    endDoc.release() // Release the temporary transformed Mat

                    if (!successEncode) {
                        throw IOException("Failed to encode Mat to $imageSuffix")
                    }
                    val imageBytes = matOfByte.toArray()
                    matOfByte.release()

                    out.write(imageBytes)
                    Log.d(TAG, "Successfully wrote image data.")
                } ?: throw IOException("Failed to open created Media File.")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isIntentCapture) {
                val updateDetails = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0) // Mark as complete
                }
                contentResolver.update(savedFileUri, updateDetails, null, null)
            }

            if (mimeType == "image/jpeg") {
                try {
                    contentResolver.openFileDescriptor(savedFileUri, "rw")?.use { pfd ->
                        val exif = ExifInterface(pfd.fileDescriptor)
                        val nowFormatted = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) // Using a more EXIF-friendly date format
                        exif.setAttribute(ExifInterface.TAG_DATETIME, nowFormatted)
                        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, nowFormatted)
                        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, nowFormatted)
                        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "OpenNoteScanner " + BuildConfig.VERSION_NAME + " https://goo.gl/2JwEPq")
                        exif.setAttribute("UserComment", "Generated using Open Note Scanner")
                        exif.saveAttributes()
                        Log.d(TAG, "Exif data written to MediaStore URI.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing Exif to MediaStore URI: ${e.message}", e)
                }
            }

            if (isIntentCapture) {
                if (outputUriFromIntent != null) {
                    setResult(RESULT_OK, intent)
                } else {
                    setResult(RESULT_OK)
                }
                finish()
            } else {
                Log.d(TAG, "Document saved to MediaStore: $savedFileUri")
                animateDocument(savedFileUri, scannedDocument)

                TrackHelper.track().event("Picture", "PictureTaken").with(tracker)
                refreshCamera()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving document: ${e.message}", e)
            if (savedFileUri != null) {
                try {
                    contentResolver.delete(savedFileUri, null, null)
                    Log.w(TAG, "Attempted to delete MediaStore entry due to error: $savedFileUri")
                    // For Pre-Q, if preQFilePath is not null, also attempt to delete the physical file
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && preQFilePath != null) {
                        val physicalFile = File(preQFilePath)
                        if (physicalFile.exists()) {
                            if (physicalFile.delete()) {
                                Log.w(TAG, "Deleted physical file (pre-Q) due to error: $preQFilePath")
                            } else {
                                Log.e(TAG, "Failed to delete physical file (pre-Q) due to error: $preQFilePath")
                            }
                        }
                    }
                } catch (deleteEx: Exception) {
                    Log.e(TAG, "Error during cleanup of MediaStore entry or file: ${deleteEx.message}")
                }
            }
            if (isIntentCapture) {
                setResult(RESULT_CANCELED)
                finish()
            } else {
                // TODO: show error
                refreshCamera()
            }
        } finally {
            encodingParams.release()
        }
    }

    private fun animateDocument(documentUri: Uri, quadrilateral: ScannedDocument) {
        val runnable = AnimationRunnable(this, documentUri, quadrilateral)
        runOnUiThread(runnable)
    }

    private fun shootSound() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val volume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        if (volume != 0) {
            if (mediaActionSound == null) {
                mediaActionSound = MediaActionSound()
                // Optional: Preload the sound for faster playback the first time.
                // This is useful if you call shootSound frequently.
                // mediaActionSound?.load(MediaActionSound.SHUTTER_CLICK)
            }
            mediaActionSound?.play(MediaActionSound.SHUTTER_CLICK)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return false
    }

    private fun statsOptInDialog() {
        val statsOptInDialog = AlertDialog.Builder(this)
        statsOptInDialog.setTitle(getString(R.string.stats_optin_title))
        statsOptInDialog.setMessage(getString(R.string.stats_optin_text))
        statsOptInDialog.setPositiveButton(R.string.answer_yes) { dialog: DialogInterface, which: Int ->
            mSharedPref.edit().putBoolean("usage_stats", true).apply()
            mSharedPref.edit().putBoolean("isFirstRun", false).apply()
            dialog.dismiss()
        }
        statsOptInDialog.setNegativeButton(R.string.answer_no) { dialog: DialogInterface, which: Int ->
            mSharedPref.edit().putBoolean("usage_stats", false).apply()
            mSharedPref.edit().putBoolean("isFirstRun", false).apply()
            dialog.dismiss()
        }
        statsOptInDialog.setNeutralButton(R.string.answer_later) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        statsOptInDialog.create().show()
    }

    companion object {
        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
        private const val CREATE_PERMISSIONS_REQUEST_CAMERA = 1
        private const val MY_PERMISSIONS_REQUEST_WRITE = 3
        private const val RESUME_PERMISSIONS_REQUEST_CAMERA = 11
        private const val TAG = "OpenNoteScannerActivity"
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // TODO("Not yet implemented")
    }
}