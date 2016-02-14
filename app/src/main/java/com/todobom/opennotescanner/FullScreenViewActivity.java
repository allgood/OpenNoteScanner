package com.todobom.opennotescanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.todobom.opennotescanner.helpers.Utils;

public class FullScreenViewActivity extends Activity{

    private Utils utils;
    private FullScreenImageAdapter adapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_view);

        viewPager = (ViewPager) findViewById(R.id.pager);

        // close button click event
        Button btnClose = (Button) findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullScreenViewActivity.this.finish();
            }
        });



        utils = new Utils(getApplicationContext());

        Intent i = getIntent();
        int position = i.getIntExtra("position", 0);

        adapter = new FullScreenImageAdapter(FullScreenViewActivity.this,
                utils.getFilePaths());

        viewPager.setAdapter(adapter);

        // displaying selected image first
        viewPager.setCurrentItem(position);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d("fullview", "scrolled position "+position+" offset "+positionOffset);
                Log.d("fullview", "pager "+FullScreenViewActivity.this.viewPager.getCurrentItem());

            }

            @Override
            public void onPageSelected(int position) {
                Log.d("fullview", "selected");
                Log.d("fullview", "item" + FullScreenViewActivity.this.viewPager.getCurrentItem());

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d("fullview", "state changed");

            }


        });
    }
}
