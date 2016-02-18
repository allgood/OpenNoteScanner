package com.todobom.opennotescanner;

// based on http://android-er.blogspot.com.br/2012/07/gridview-loading-photos-from-sd-card.html

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.todobom.opennotescanner.helpers.Utils;

import java.io.File;
import java.util.ArrayList;

public class GalleryGridActivity extends AppCompatActivity {

    private boolean selectionStarted = false;
    private ArrayList<ImageView> selection;
    private Toolbar mToolbar;
    private MenuItem mShare;

    public class ImageAdapter extends BaseAdapter {

        private Context mContext;
        ArrayList<String> itemList = new ArrayList<String>();

        public ImageAdapter(Context c) {
            mContext = c;
        }

        void add(String path){
            itemList.add(path);
        }

        @Override
        public int getCount() {
            return itemList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return itemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SquareImageView imageView;
            if (convertView == null) {  // if it's not recycled, initialize some attributes
                imageView = new SquareImageView(mContext);
                // imageView.setLayoutParams(new GridView.LayoutParams(220, 220));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (SquareImageView) convertView;
            }

            Bitmap bm = decodeSampledBitmapFromUri(itemList.get(position), 220, 220);

            imageView.setImageBitmap(bm);

            // image view click listener
            imageView.setOnClickListener(new OnImageClickListener(position));

            imageView.setOnLongClickListener(new OnImageLongClickListener(position));

            return imageView;
        }

        private class SquareImageView extends ImageView {

            public SquareImageView(Context context) {
                super(context);
            }

            @Override
            public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, widthMeasureSpec);
            }
        }

        public Bitmap decodeSampledBitmapFromUri(String path, int reqWidth, int reqHeight) {

            Bitmap bm = null;
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            bm = BitmapFactory.decodeFile(path, options);

            return bm;
        }

        public int calculateInSampleSize(

                BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                if (width > height) {
                    inSampleSize = Math.round((float)height / (float)reqHeight);
                } else {
                    inSampleSize = Math.round((float)width / (float)reqWidth);
                }
            }

            return inSampleSize;
        }

    }

    ImageAdapter myImageAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        /*

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        this.setSupportActionBar(mToolbar);

        */

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(null);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#66ffffff")));

        // */

        selection = new ArrayList<ImageView>();

        GridView gridview = (GridView) findViewById(R.id.gridview);
        myImageAdapter = new ImageAdapter(this);
        gridview.setAdapter(myImageAdapter);

        String ExternalStorageDirectoryPath = Environment
                .getExternalStorageDirectory()
                .getAbsolutePath();

        String targetPath = ExternalStorageDirectoryPath + "/OpenNoteScanner/";

        // Toast.makeText(getApplicationContext(), targetPath, Toast.LENGTH_LONG).show();
        File targetDirector = new File(targetPath);

        ArrayList<String> files = new Utils(getApplicationContext()).getFilePaths();

        for (String file : files){
            myImageAdapter.add(file);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery, menu);

        mShare = menu.findItem(R.id.action_share);
        mShare.setVisible(false);
        invalidateOptionsMenu();

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
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    class OnImageLongClickListener implements View.OnLongClickListener {


        int _position;

        // constructor
        public OnImageLongClickListener(int position) {
            this._position = position;
        }

        @Override
        public boolean onLongClick(View v) {
            GalleryGridActivity.this.selectionToggle(_position, (ImageView) v);

            return true;
        }

    }

    public void selectionToggle( int position , ImageView v ) {

        boolean oldState = selection.size()>0;

        if (selection.contains(v)) {
            selection.remove(v);
            v.setColorFilter(Color.argb(0, 0, 0, 0));
        } else {
            selection.add(v);
            v.setColorFilter(Color.argb(140, 0, 0, 255));
        }

        boolean newState = selection.size()>0;

        Log.d("gallery", "oldstate " + oldState + " newstate " + newState);

        if (newState != oldState) {
            mShare.setVisible(newState);
        }

    }

    class OnImageClickListener implements View.OnClickListener {

        int _position;

        // constructor
        public OnImageClickListener(int position) {
            this._position = position;
        }

        @Override
        public void onClick(View v) {


            if (GalleryGridActivity.this.selection.size()>0) {
                GalleryGridActivity.this.selectionToggle(_position, (ImageView) v);
            } else {
                GalleryGridActivity activity = GalleryGridActivity.this;
                Intent i = new Intent(GalleryGridActivity.this, FullScreenViewActivity.class);
                i.putExtra("position", _position);
                activity.startActivity(i);
            }
        }



    }

}
