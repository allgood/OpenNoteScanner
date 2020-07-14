package com.todobom.opennotescanner;

// based on http://android-er.blogspot.com.br/2012/07/gridview-loading-photos-from-sd-card.html

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView;
import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.todobom.opennotescanner.helpers.AboutFragment;
import com.todobom.opennotescanner.helpers.PdfHelper;
import com.todobom.opennotescanner.helpers.Utils;

import java.io.File;
import java.util.ArrayList;

public class GalleryGridActivity extends AppCompatActivity
        implements ClickListener, DragSelectRecyclerViewAdapter.SelectionListener {

    private static final String TAG = "GalleryGridActivity";
    private MenuItem mShare;
    private MenuItem mTag;
    private MenuItem mDelete;
    private MenuItem mPdfExport;
    private DragSelectRecyclerView recyclerView;
    private AlertDialog.Builder deleteConfirmBuilder;
    private boolean selectionMode = false;
    private ImageLoader mImageLoader;
    private ImageSize mTargetSize;
    private SharedPreferences mSharedPref;

    @Override
    public void onClick(int index) {
        if (selectionMode) {
            myThumbAdapter.toggleSelected(index);
        } else {
            Intent i = new Intent(this, FullScreenViewActivity.class);
            i.putExtra("position", index);
            this.startActivity(i);
        }
    }

    @Override
    public void onLongClick(int index) {
        if (!selectionMode) {
            setSelectionMode(true);
        }
        recyclerView.setDragSelectActive(true, index);
    }

    private void setSelectionMode(boolean selectionMode) {
        if (mShare != null && mDelete != null ) {
            mShare.setVisible(selectionMode);
            //mTag.setVisible(selectionMode);
            mDelete.setVisible(selectionMode);
        }

        if (mPdfExport != null) {
            mPdfExport.setVisible(selectionMode);
        }

        this.selectionMode = selectionMode;
    }

    @Override
    public void onDragSelectionChanged(int i) {
        Log.d(TAG, "DragSelectionChanged: " + i);

        setSelectionMode(i > 0);
    }


    public class ThumbAdapter extends DragSelectRecyclerViewAdapter<ThumbAdapter.ThumbViewHolder> {

        private final ClickListener mCallback;

        ArrayList<String> itemList = new ArrayList<>();

        // Constructor takes click listener callback
        protected ThumbAdapter(GalleryGridActivity activity, ArrayList<String> files) {
            super();
            mCallback = activity;

            for (String file : files) {
                add(file);
            }

            setSelectionListener(activity);

        }

        void add(String path) {
            itemList.add(path);
        }

        @Override
        public ThumbViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
            return new ThumbViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ThumbViewHolder holder, int position) {
            super.onBindViewHolder(holder, position); // this line is important!

            String filename = itemList.get(position);

            if (!filename.equals(holder.filename)) {

                // remove previous image
                holder.image.setImageBitmap(null);

                // Load image, decode it to Bitmap and return Bitmap to callback
                mImageLoader.displayImage("file:///" + filename, holder.image, mTargetSize);

                // holder.image.setImageBitmap(decodeSampledBitmapFromUri(filename, 220, 220));

                holder.filename = filename;
            }

            if (isIndexSelected(position)) {
                holder.image.setColorFilter(Color.argb(140, 0, 255, 0));
            } else {
                holder.image.setColorFilter(Color.argb(0, 0, 0, 0));
            }
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        public ArrayList<String> getSelectedFiles() {

            ArrayList<String> selection = new ArrayList<>();

            for (Integer i : getSelectedIndices()) {
                selection.add(itemList.get(i));
            }

            return selection;
        }


        public class ThumbViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener, View.OnLongClickListener {

            public final ImageView image;
            public String filename;

            public ThumbViewHolder(View itemView) {
                super(itemView);
                this.image = (ImageView) itemView.findViewById(R.id.gallery_image);
                this.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                // this.image.setPadding(8, 8, 8, 8);
                this.itemView.setOnClickListener(this);
                this.itemView.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View v) {
                // Forwards to the adapter's constructor callback
                if (mCallback != null) mCallback.onClick(getAdapterPosition());
            }

            @Override
            public boolean onLongClick(View v) {
                // Forwards to the adapter's constructor callback
                if (mCallback != null) mCallback.onLongClick(getAdapterPosition());
                return true;
            }
        }

    }


    ThumbAdapter myThumbAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        ((OpenNoteScannerApplication) getApplication()).getTracker()
                .trackScreenView("/GalleryGridActivity", "Gallery");

        setContentView(R.layout.activity_gallery);

        ActionBar actionBar = getSupportActionBar();

        assert actionBar != null;
        actionBar.setDisplayShowHomeEnabled(true);

        actionBar.setTitle(null);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        mImageLoader = ImageLoader.getInstance();
        mImageLoader.init(config);

        mTargetSize = new ImageSize(220, 220); // result Bitmap will be fit to this size

        ArrayList<String> ab = new ArrayList<>();
        myThumbAdapter = new ThumbAdapter(this, ab);
        // new Utils(getApplicationContext()).getFilePaths(););

        recyclerView = (DragSelectRecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(myThumbAdapter);

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

    private void reloadAdapter() {
        recyclerView.setAdapter(null);

        // ArrayList<String> ab = new ArrayList<>();
        myThumbAdapter = new ThumbAdapter(this, new Utils(getApplicationContext()).getFilePaths());

        recyclerView.setAdapter(myThumbAdapter);
        recyclerView.invalidate();

        setSelectionMode(false);

    }

    @Override
    public void onResume() {
        super.onResume();
        reloadAdapter();
    }

    private void deleteImage() {
        for (String filePath : myThumbAdapter.getSelectedFiles()) {
            final File photoFile = new File(filePath);
            if (photoFile.delete()) {
                Utils.removeImageFromGallery(filePath, this);
                Log.d(TAG, "Removed file: " + filePath);
            }
        }

        reloadAdapter();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery, menu);

        mShare = menu.findItem(R.id.action_share);
        mShare.setVisible(false);

        mTag = menu.findItem(R.id.action_tag);
        // mTag.setVisible(false);

        mDelete = menu.findItem(R.id.action_delete);
        mDelete.setVisible(false);

        mPdfExport = menu.findItem(R.id.action_pdfexport);
        mPdfExport.setVisible(false);

        invalidateOptionsMenu();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_share:
                shareImages();
                return true;
            case R.id.action_pdfexport:
                pdfExport();
                return true;
            case R.id.action_tag:
                break;
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

    public void pdfExport() {
        String pdfFilePath = PdfHelper.mergeImagesToPdf(getApplicationContext(), myThumbAdapter.getSelectedFiles());

        if (pdfFilePath != null) {
            try {
                File file = new File(pdfFilePath);
                Intent i=new Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(getApplicationContext(),
                        getPackageName() + ".fileprovider", file));
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(i);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getApplicationContext(), "Cant Find Your File", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void shareImages() {
        ArrayList<String> selectedFiles = myThumbAdapter.getSelectedFiles();

        if (selectedFiles.size() == 1) {
            /* Only one scanned document selected: ACTION_SEND intent */
            final Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpg");

            Uri uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", new File(selectedFiles.get(0)));
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            Log.d("GalleryGridActivity", "uri " + uri);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_snackbar)));
        } else {
            ArrayList<Uri> filesUris = new ArrayList<>();
            for (String i : myThumbAdapter.getSelectedFiles()) {
                Uri uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", new File(i));
                filesUris.add(uri);
                Log.d("GalleryGridActivity", "uri " + uri);
            }

            final Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("image/jpg");

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesUris);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_snackbar)));
        }
    }


}
