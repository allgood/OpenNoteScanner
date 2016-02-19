package com.todobom.opennotescanner;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.todobom.opennotescanner.helpers.Utils;

import java.io.File;

public class FullScreenViewActivity extends AppCompatActivity {

    private Utils utils;
    private FullScreenImageAdapter adapter;
    private ViewPager viewPager;
    private AlertDialog.Builder deleteConfirmBuilder;
    // private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_view);

        viewPager = (ViewPager) findViewById(R.id.pager);

        // mToolbar = (Toolbar) findViewById(R.id.FullImageViewToolbar);

        // setDisplayHomeAsUpEnabled(true);

        // setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(null);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#66ffffff")));


        /*

        // close button click event
        Button btnClose = (Button) findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullScreenViewActivity.this.finish();
            }
        });

        // */

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
                Log.d("fullview", "scrolled position " + position + " offset " + positionOffset);
                Log.d("fullview", "pager " + FullScreenViewActivity.this.viewPager.getCurrentItem());
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

        deleteConfirmBuilder = new AlertDialog.Builder(this);

        deleteConfirmBuilder.setTitle(getString(R.string.confirm_title));
        deleteConfirmBuilder.setMessage(getString(R.string.confirm_delete_text));

        deleteConfirmBuilder.setPositiveButton(getString(R.string.answer_yes), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                deleteImage();
                dialog.dismiss();
            }

        });

        deleteConfirmBuilder.setNegativeButton(getString(R.string.answer_no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_imagepager, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_share:
                shareImage();
                return true;
            case R.id.action_delete:
                deleteConfirmBuilder.create().show();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteImage() {
        ViewPager pager = FullScreenViewActivity.this.viewPager;
        int item = pager.getCurrentItem();

        final File photoFile = new File(adapter.getPath(item));

        // pager.removeViewAt(item);

        photoFile.delete();

        pager.setAdapter(null);
        adapter = new FullScreenImageAdapter(FullScreenViewActivity.this,
                utils.getFilePaths());

        pager.setAdapter(adapter);

    }

    public void shareImage() {

        ViewPager pager = FullScreenViewActivity.this.viewPager;
        int item = pager.getCurrentItem();

        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpg");
        final File photoFile = new File(adapter.getPath(item));
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile));
        Log.d("Fullscreen","uri "+Uri.fromFile(photoFile));
        startActivity(Intent.createChooser(shareIntent, "Share image using"));
    }

}
