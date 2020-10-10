package com.todobom.opennotescanner

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.viewpager.widget.PagerAdapter
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.assist.ImageSize
import com.ortiz.touchview.TouchImageView
import java.util.*

/*
* based on code originally at http://www.androidhive.info/2013/09/android-fullscreen-image-slider-with-swipe-and-pinch-zoom-gestures/
*/
class FullScreenImageAdapter(
        private val _activity: Activity,
        private val _imagePaths: ArrayList<String>
) : PagerAdapter() {
    private var maxTexture = 0
    private var mImageLoader: ImageLoader? = null
    private var mTargetSize: ImageSize? = null
    override fun getCount(): Int {
        return _imagePaths.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imgDisplay: TouchImageView
        val inflater = _activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container,
                false)
        imgDisplay = viewLayout.findViewById<View>(R.id.imgDisplay) as TouchImageView
        val imagePath = _imagePaths[position]
        /*
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        int maxSize=Math.max(options.outHeight,options.outWidth);

        Log.d(TAG,"Texture: "+maxTexture + "Size: "+maxSize);

        if (maxTexture>0 && maxSize>maxTexture) {
            double scale=(double)maxSize/(double)maxTexture;

            options.inSampleSize = (int) Math.pow( 2, (Math.floor(Log(scale, 2))+1) );

            Log.d(TAG, "inSampleSize: " + options.inSampleSize);
        }

        options.inJustDecodeBounds = false;

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        */

        // imgDisplay.setImageBitmap(bitmap);
        mImageLoader!!.displayImage("file:///$imagePath", imgDisplay, mTargetSize)
        container.addView(viewLayout)
        return viewLayout
    }

    fun getPath(position: Int): String {
        return _imagePaths[position]
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as RelativeLayout)
    }

    fun setMaxTexture(maxTexture: Int, targetSize: ImageSize?) {
        this.maxTexture = maxTexture
        mTargetSize = targetSize
    }

    fun setImageLoader(imageLoader: ImageLoader?) {
        mImageLoader = imageLoader
    }

    companion object {
        private const val TAG = "FullScreenImageAdapter"
        private fun Log(n: Double, base: Double): Double {
            return Math.log(n) / Math.log(base)
        }
    }
}