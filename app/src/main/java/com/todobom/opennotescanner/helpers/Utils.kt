package com.todobom.opennotescanner.helpers

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import com.todobom.opennotescanner.OpenNoteScannerActivity
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

class Utils(
        private val _context: Context
) {
    private val mSharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context)

    val fileUris: ArrayList<Uri>
        get() {
            val imageUris = ArrayList<Uri>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA // For pre-Q compatibility and direct paths, FIXME: switch to only URI
            )

            val appFolderName = mSharedPref.getString("storage_folder", "OpenNoteScanner")
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            } else {
                "${MediaStore.Images.Media.DATA} LIKE ?"
            }
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // post-Q, partial path matching
                arrayOf("%${Environment.DIRECTORY_PICTURES}/$appFolderName/%")
            } else {
                // pre-Q, direct path matching
                val legacyPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath +
                        File.separator + appFolderName
                arrayOf("$legacyPath/%")
            }

            // Sort order
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC" // Or DATE_ADDED, DISPLAY_NAME ASC

            _context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    imageUris.add(contentUri)
                }
            }
            return imageUris
        }

    companion object {

        private const val TAG = "OpenNoteScanner-Utils"

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
        fun isMatch(s: String, pattern: String): Boolean {
            return try {
                val patt = Pattern.compile(pattern)
                val matcher = patt.matcher(s)
                matcher.matches()
            } catch (e: RuntimeException) {
                false
            }
        }

        fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
            var inputStream: InputStream? = null
            try {
                // First decode with inJustDecodeBounds=true to check dimensions
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "decodeSampledBitmapFromUri: Could not open InputStream for URI: $uri")
                    return null
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                // calculate downsample factor
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

                options.inJustDecodeBounds = false
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "decodeSampledBitmapFromUri: Could not reopen InputStream for URI: $uri")
                    return null
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                return bitmap
            } finally {
                inputStream?.close()
            }
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

        @JvmStatic
        fun removeImageFromGallery(fileUri: Uri, context: Context) {
            try {
                val rowsDeleted = context.contentResolver.delete(fileUri, null, null)
                if (rowsDeleted == 0) {
                    Log.w(TAG, "Nothing deleted for: $fileUri")
                }
            } catch (e: SecurityException) {
                // we should have permissions
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun isPackageInstalled(context: Context, packagename: String): Boolean {
            return try {
                context.packageManager.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
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