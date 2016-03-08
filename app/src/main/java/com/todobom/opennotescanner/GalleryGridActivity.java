package com.todobom.opennotescanner;

// based on http://android-er.blogspot.com.br/2012/07/gridview-loading-photos-from-sd-card.html

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.todobom.opennotescanner.helpers.AboutFragment;
import com.todobom.opennotescanner.helpers.Utils;

import java.io.File;
import java.util.ArrayList;

import static com.todobom.opennotescanner.helpers.Utils.decodeSampledBitmapFromUri;

public class GalleryGridActivity extends AppCompatActivity {

    private ArrayList<String> selection;
    private MenuItem mShare;
    private MenuItem mDelete;
    private GridView gridview;
    private AlertDialog.Builder deleteConfirmBuilder;

    public class ImageAdapter extends BaseAdapter {

        private Context mContext;
        ArrayList<String> itemList = new ArrayList<String>();

        public ImageAdapter(Context c, ArrayList<String> files) {
            mContext = c;

            for (String file : files){
                add(file);
            }

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
                int squareSpec = Math.max(widthMeasureSpec, heightMeasureSpec);
                super.onMeasure(squareSpec, squareSpec);
            }
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

        selection = new ArrayList<String>();

        gridview = (GridView) findViewById(R.id.gridview);

        myImageAdapter = new ImageAdapter(this, new Utils(getApplicationContext()).getFilePaths());
        gridview.setAdapter(myImageAdapter);

        String ExternalStorageDirectoryPath = Environment
                .getExternalStorageDirectory()
                .getAbsolutePath();

        String targetPath = ExternalStorageDirectoryPath + "/OpenNoteScanner/";

        // Toast.makeText(getApplicationContext(), targetPath, Toast.LENGTH_LONG).show();
        File targetDirector = new File(targetPath);



        deleteConfirmBuilder = new AlertDialog.Builder(this);

        deleteConfirmBuilder.setTitle(getString(R.string.confirm_title));
        deleteConfirmBuilder.setMessage(getString(R.string.confirm_delete_multiple_text));

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

    private void deleteImage() {
        for ( String filePath: selection ) {
            final File photoFile = new File(filePath);
            photoFile.delete();
        }

        selection.clear();
        selection = new ArrayList<String>();

        gridview.setAdapter(null);
        myImageAdapter = new ImageAdapter(this,new Utils(getApplicationContext()).getFilePaths());

        gridview.setAdapter(myImageAdapter);
        gridview.invalidate();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery, menu);

        mShare = menu.findItem(R.id.action_share);
        mShare.setVisible(false);

        mDelete = menu.findItem(R.id.action_delete);
        mDelete.setVisible(false);

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
                break;
            case R.id.action_share:
                shareImages();
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

    public void shareImages() {

        final Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("image/jpg");

        ArrayList<Uri> filesUris = new ArrayList<Uri>();

        for ( String i: selection ) {
            filesUris.add(Uri.parse("file://" + i));
        }
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesUris);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_snackbar)));
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

        String filePath = (String) myImageAdapter.getItem(position);

        if (selection.contains(filePath)) {
            selection.remove(filePath);
            v.setColorFilter(Color.argb(0, 0, 0, 0));
        } else {
            selection.add(filePath);
            v.setColorFilter(Color.argb(140, 0, 0, 255));
        }

        boolean newState = selection.size()>0;

        Log.d("gallery", "oldstate " + oldState + " newstate " + newState);

        if (newState != oldState) {
            mShare.setVisible(newState);
            mDelete.setVisible(newState);
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
