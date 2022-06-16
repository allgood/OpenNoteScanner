package com.todobom.opennotescanner.helpers

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.view.WindowManager
import com.todobom.opennotescanner.OpenNoteScannerActivity
import java.io.File
import java.util.*
import java.util.regex.Pattern
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

class Utils(
        private val _context: Context
) {
    private val mSharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context)

    /*
       * Reading file paths from SDCard
       */
    val filePaths: ArrayList<String>
        get() {
            val filePaths = ArrayList<String>()
            val directory = File(
                    Environment.getExternalStorageDirectory()
                            .toString() + File.separator + mSharedPref.getString("storage_folder", "OpenNoteScanner"))

            // check for directory
            if (directory.isDirectory) {
                // getting list of file paths
                val listFiles = directory.listFiles()
                Arrays.sort(listFiles) { f1, f2 -> f2.name.compareTo(f1.name) }

                // Check for count
                if (listFiles.size > 0) {

                    // loop through all files
                    for (i in listFiles.indices) {

                        // get file path
                        val filePath = listFiles[i].absolutePath

                        // check for supported file extension
                        if (isSupportedFile(filePath)) {
                            // Add image path to array list
                            filePaths.add(filePath)
                        }
                    }
                }
            }
            return filePaths
        }

    /*
     * Check supported file extensions
     *
     * @returns boolean
     */
    private fun isSupportedFile(filePath: String): Boolean {
        val ext = filePath.substring(filePath.lastIndexOf(".") + 1,
                filePath.length)
        return AppConstant.FILE_EXTN.contains(ext.toLowerCase(Locale.getDefault()))
    }// Older device

    /*
       * getting screen width
       */
    val screenWidth: Int
        get() {
            val columnWidth: Int
            val wm = _context
                    .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            val point = Point()
            try {
                display.getSize(point)
            } catch (ignore: NoSuchMethodError) { // Older device
                point.x = display.width
                point.y = display.height
            }
            columnWidth = point.x
            return columnWidth
        }

    companion object {
        @JvmStatic
        val maxTextureSize: Int
            get() {
                // Safe minimum default size
                val IMAGE_MAX_BITMAP_DIMENSION = 2048

                // Get EGL Display
                val egl = EGLContext.getEGL() as EGL10
                val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

                // Initialise
                val version = IntArray(2)
                egl.eglInitialize(display, version)

                // Query total number of configurations
                val totalConfigurations = IntArray(1)
                egl.eglGetConfigs(display, null, 0, totalConfigurations)

                // Query actual list configurations
                val configurationsList = arrayOfNulls<EGLConfig>(totalConfigurations[0])
                egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations)
                val textureSize = IntArray(1)
                var maximumTextureSize = 0

                // Iterate through all the configurations to located the maximum texture size
                for (i in 0 until totalConfigurations[0]) {
                    // Only need to check for width since opengl textures are always squared
                    egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize)

                    // Keep track of the maximum texture size
                    if (maximumTextureSize < textureSize[0]) maximumTextureSize = textureSize[0]
                }

                // Release
                egl.eglTerminate(display)

                // Return largest texture size found, or default
                return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION)
            }

        @JvmStatic
        fun isMatch(s: String?, pattern: String?): Boolean {
            return try {
                val patt = Pattern.compile(pattern)
                val matcher = patt.matcher(s)
                matcher.matches()
            } catch (e: RuntimeException) {
                false
            }
        }

        fun decodeSampledBitmapFromUri(path: String?, reqWidth: Int, reqHeight: Int): Bitmap? {
            var bm: Bitmap? = null
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            bm = BitmapFactory.decodeFile(path, options)
            return bm
        }

        fun calculateInSampleSize(
                options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {

            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                inSampleSize = if (width > height) {
                    Math.round(height.toFloat() / reqHeight.toFloat())
                } else {
                    Math.round(width.toFloat() / reqWidth.toFloat())
                }
            }
            return inSampleSize
        }

        fun addImageToGallery(filePath: String?, context: Context) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.MediaColumns.DATA, filePath)
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }

        @JvmStatic
        fun removeImageFromGallery(filePath: String, context: Context) {
            context.contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA
                            + "='"
                            + filePath
                            + "'", null)
        }

        @JvmStatic
        fun isPackageInstalled(context: Context, packagename: String): Boolean {
            val pm = context.packageManager
            var app_installed = false
            app_installed = try {
                val info = pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES)
                val label = info.applicationInfo.loadLabel(pm) as String
                label != null
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
            return app_installed
        }


        @JvmStatic
        fun getDocumentArea(width: Int, height: Int, mainActivity: OpenNoteScannerActivity): IntArray? {
            val documentArea = IntArray(4)

            // attention: axis are swapped
            val imageRatio = width.toFloat() / height.toFloat()
            val bottomPos: Int
            val topPos: Int
            val leftPos: Int
            val rightPos: Int

            var documentAspectRatio = mainActivity.mDocumentAspectRatio

            if (documentAspectRatio == 0.0) {
                throw Exception("do not use getDocumentArea without an aspect ratio")
            } else if (imageRatio >= documentAspectRatio) {
                val documentWidth: Float = (height - height/10).toFloat()
                val documentHeight: Float = (documentWidth.toFloat() * documentAspectRatio).toFloat()

                topPos = height / 20
                bottomPos = height - topPos
                leftPos = ((width - documentHeight) / 2).toInt()
                rightPos = width - leftPos
            } else {
                val documentHeight: Float = (width - width/5).toFloat()
                val documentWidth: Float = (documentHeight / documentAspectRatio).toFloat()

                leftPos = width/10
                rightPos = width - leftPos
                topPos = ((height - documentWidth) / 2).toInt()
                bottomPos = height - topPos
            }

            documentArea[0] = leftPos
            documentArea[1] = topPos
            documentArea[2] = rightPos
            documentArea[3] = bottomPos
            return documentArea
        }

        @JvmStatic
        fun getHotArea(width: Int, height: Int, mainActivity: OpenNoteScannerActivity): IntArray? {
            var hotArea = IntArray(4)

            // attention: axis are swapped
            val imageRatio = width.toFloat() / height.toFloat()
            val bottomPos: Int
            val topPos: Int
            val leftPos: Int
            val rightPos: Int

            var documentAspectRatio = mainActivity.mDocumentAspectRatio

            if (documentAspectRatio == 0.0) {
                val baseMeasure = height / 4
                bottomPos = height - baseMeasure
                topPos = baseMeasure
                leftPos = width / 2 - baseMeasure
                rightPos = width / 2 + baseMeasure
                hotArea[0] = leftPos
                hotArea[1] = topPos
                hotArea[2] = rightPos
                hotArea[3] = bottomPos
                return hotArea
            }

            hotArea = Utils.getDocumentArea(width, height, mainActivity)!!
            val hotOffset = height/10

            hotArea[0] += hotOffset
            hotArea[1] += hotOffset
            hotArea[2] -= hotOffset
            hotArea[3] -= hotOffset
            return hotArea;
        }
    }

}