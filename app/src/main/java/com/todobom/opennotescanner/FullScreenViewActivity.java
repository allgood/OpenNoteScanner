package com.todobom.opennotescanner;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.todobom.opennotescanner.helpers.AboutFragment;
import com.todobom.opennotescanner.helpers.Utils;
import com.todobom.opennotescanner.views.TagEditorFragment;

import java.io.File;

public class FullScreenViewActivity extends AppCompatActivity {

    private Utils utils;
    private FullScreenImageAdapter mAdapter;
    private ViewPager mViewPager;
    private AlertDialog.Builder deleteConfirmBuilder;
    private ImageLoader mImageLoader;
    private ImageSize mTargetSize;
    private int mMaxTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((OpenNoteScannerApplication) getApplication()).getTracker()
                .trackScreenView("/FullScreenViewActivity", "Full Screen Viewer");

        setContentView(R.layout.activity_fullscreen_view);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(null);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);

        utils = new Utils(getApplicationContext());

        Intent i = getIntent();
        int position = i.getIntExtra("position", 0);

        // initialize Universal Image Loader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        mImageLoader = ImageLoader.getInstance();
        mImageLoader.init(config);

        mMaxTexture = Utils.getMaxTextureSize();
        Log.d("FullScreenViewActivity", "gl resolution: " + mMaxTexture);
        mTargetSize = new ImageSize(mMaxTexture, mMaxTexture);

        loadAdapter();

        // displaying selected image first
        mViewPager.setCurrentItem(position);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d("fullview", "scrolled position " + position + " offset " + positionOffset);
                Log.d("fullview", "pager " + FullScreenViewActivity.this.mViewPager.getCurrentItem());
            }

            @Override
            public void onPageSelected(int position) {
                Log.d("fullview", "selected");
                Log.d("fullview", "item" + FullScreenViewActivity.this.mViewPager.getCurrentItem());
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

    private void loadAdapter() {
        mViewPager.setAdapter(null);
        mAdapter = new FullScreenImageAdapter(FullScreenViewActivity.this,
                utils.getFilePaths());
        mAdapter.setImageLoader(mImageLoader);
        mAdapter.setMaxTexture(mMaxTexture, mTargetSize);
        mViewPager.setAdapter(mAdapter);
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
            case R.id.action_tag:
                tagImage();
                return true;
            case R.id.action_share:
                shareImage();
                return true;
            case R.id.action_delete:
                deleteConfirmBuilder.create().show();
                return true;
            case R.id.action_about:
                FragmentManager fm = getSupportFragmentManager();
                AboutFragment aboutDialog = new AboutFragment();
                aboutDialog.show(fm, "about_view");
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void tagImage() {
        int item = mViewPager.getCurrentItem();
        String filePath = mAdapter.getPath(item);

        FragmentManager fm = getSupportFragmentManager();
        TagEditorFragment tagEditorDialog = new TagEditorFragment();

        tagEditorDialog.setFilePath(filePath);

        tagEditorDialog.setRunOnDetach(new Runnable() {
            @Override
            public void run() {
            }
        });
        tagEditorDialog.show(fm, "tageditor_view");
    }

    private void deleteImage() {
        int item = mViewPager.getCurrentItem();

        String filePath = mAdapter.getPath(item);
        final File photoFile = new File(filePath);

        photoFile.delete();
        Utils.removeImageFromGallery(filePath,this);

        loadAdapter();
        mViewPager.setCurrentItem(item);
    }

    public void shareImage() {

        ViewPager pager = FullScreenViewActivity.this.mViewPager;
        int item = pager.getCurrentItem();

        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpg");
        final File photoFile = new File(mAdapter.getPath(item));
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile));
        Log.d("Fullscreen","uri "+Uri.fromFile(photoFile));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_snackbar)));
    }

}
