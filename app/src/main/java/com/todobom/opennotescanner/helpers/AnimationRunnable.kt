package com.todobom.opennotescanner.helpers

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.animation.*
import android.widget.ImageView
import android.widget.RelativeLayout
import com.todobom.opennotescanner.OpenNoteScannerActivity
import com.todobom.opennotescanner.R
import org.opencv.core.Point
import org.opencv.core.Size

class AnimationRunnable(private val activity: OpenNoteScannerActivity, filename: String, document: ScannedDocument) : Runnable {
    private val imageSize: Size = document.processed!!.size()
    private val previewPoints: Array<Point>? = if(document.quadrilateral != null) document.previewPoints else null
    private val previewSize: Size? = if(document.quadrilateral != null) document.previewSize else null
    private val fileName: String = filename
    private var bitmap: Bitmap? = null

    override fun run() {
        val imageView = activity.findViewById<View>(R.id.scannedAnimation) as ImageView
        val display = activity.windowManager.defaultDisplay
        val size = android.graphics.Point()
        display.getRealSize(size)
        val width = Math.min(size.x, size.y)
        val height = Math.max(size.x, size.y)

        // ATENTION: captured images are always in landscape, values should be swapped
        val imageWidth = imageSize.height
        val imageHeight = imageSize.width
        val params = imageView.layoutParams as RelativeLayout.LayoutParams
        val prevPoints = previewPoints
        val prevSize = previewSize

        if (prevPoints != null) {
            prevSize!!

            val documentLeftHeight = hipotenuse(prevPoints[0], prevPoints[1])
            val documentBottomWidth = hipotenuse(prevPoints[1], prevPoints[2])
            val documentRightHeight = hipotenuse(prevPoints[2], prevPoints[3])
            val documentTopWidth = hipotenuse(prevPoints[3], prevPoints[0])
            val documentWidth = Math.max(documentTopWidth, documentBottomWidth)
            val documentHeight = Math.max(documentLeftHeight, documentRightHeight)
            Log.d(TAG, "device: " + width + "x" + height + " image: " + imageWidth + "x" + imageHeight + " document: " + documentWidth + "x" + documentHeight)
            Log.d(TAG, "prevPoints[0] x=" + prevPoints[0].x + " y=" + prevPoints[0].y)
            Log.d(TAG, "prevPoints[1] x=" + prevPoints[1].x + " y=" + prevPoints[1].y)
            Log.d(TAG, "prevPoints[2] x=" + prevPoints[2].x + " y=" + prevPoints[2].y)
            Log.d(TAG, "prevPoints[3] x=" + prevPoints[3].x + " y=" + prevPoints[3].y)

            // ATENTION: again, swap width and height
            val xRatio = width / prevSize.height
            val yRatio = height / prevSize.width
            params.topMargin = (prevPoints[3].x * yRatio).toInt()
            params.leftMargin = ((prevSize.height - prevPoints[3].y) * xRatio).toInt()
            params.width = (documentWidth * xRatio).toInt()
            params.height = (documentHeight * yRatio).toInt()
        } else {
            params.topMargin = height / 4
            params.leftMargin = width / 4
            params.width = width / 2
            params.height = height / 2
        }

        bitmap = Utils.decodeSampledBitmapFromUri(fileName, params.width, params.height)
        imageView.setImageBitmap(bitmap)
        imageView.visibility = View.VISIBLE
        val translateAnimation = TranslateAnimation(
                Animation.ABSOLUTE, 0f, Animation.ABSOLUTE, (-params.leftMargin).toFloat(),
                Animation.ABSOLUTE, 0f, Animation.ABSOLUTE, (height - params.topMargin).toFloat()
        )
        val scaleAnimation = ScaleAnimation(1f, 0f, 1f, 0f)
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(translateAnimation)
        animationSet.duration = 600
        animationSet.interpolator = AccelerateInterpolator()
        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                imageView.visibility = View.INVISIBLE
                imageView.setImageBitmap(null)
                bitmap?.recycle()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        imageView.startAnimation(animationSet)
    }

    companion object {
        private const val TAG = "AnimationRunnable"

        @JvmStatic
        fun hipotenuse(a: Point, b: Point): Double {
            return Math.sqrt(Math.pow(a.x - b.x, 2.0) + Math.pow(a.y - b.y, 2.0))
        }
    }
}