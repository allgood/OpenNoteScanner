package com.todobom.opennotescanner;

/*
 * based on code originaly at http://www.androidhive.info/2013/09/android-fullscreen-image-slider-with-swipe-and-pinch-zoom-gestures/
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.todobom.opennotescanner.views.TouchImageView;

import java.util.ArrayList;

public class FullScreenImageAdapter extends PagerAdapter {

    private Activity _activity;
    private ArrayList<String> _imagePaths;
    private LayoutInflater inflater;
    private int maxTexture;

    // constructor
    public FullScreenImageAdapter(Activity activity,
                                  ArrayList<String> imagePaths) {
        this._activity = activity;
        this._imagePaths = imagePaths;
    }

    @Override
    public int getCount() {
        return this._imagePaths.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        TouchImageView imgDisplay;

        inflater = (LayoutInflater) _activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container,
                false);

        imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.imgDisplay);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(_imagePaths.get(position), options);

        int origWidth=bitmap.getWidth();
        int origHeight=bitmap.getHeight();
        int maxSize=Math.max(origHeight,origWidth);
        if (maxTexture>0 && maxSize>maxTexture) {
            float scale=(float)maxSize/(float)maxTexture;
            Bitmap origBitmap = bitmap;
            bitmap = Bitmap.createScaledBitmap(origBitmap, (int)(origWidth/scale), (int)(origHeight/scale), true);
        }

        imgDisplay.setImageBitmap(bitmap);

        ((ViewPager) container).addView(viewLayout);

        return viewLayout;
    }

    public String getPath(int position) {
        return _imagePaths.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((RelativeLayout) object);
    }


    public void setMaxTexture(int maxTexture) {
        this.maxTexture = maxTexture;
    }
}
