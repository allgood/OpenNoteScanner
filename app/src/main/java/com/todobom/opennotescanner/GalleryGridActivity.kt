package com.todobom.opennotescanner

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.view.View.OnLongClickListener
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView
import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.ImageSize
import com.todobom.opennotescanner.GalleryGridActivity.ThumbAdapter.ThumbViewHolder
import com.todobom.opennotescanner.helpers.AboutFragment
import com.todobom.opennotescanner.helpers.PdfHelper.mergeImagesToPdf
import com.todobom.opennotescanner.helpers.Utils
import com.todobom.opennotescanner.helpers.Utils.Companion.removeImageFromGallery
import java.io.File
import java.util.*

// based on http://android-er.blogspot.com.br/2012/07/gridview-loading-photos-from-sd-card.html
class GalleryGridActivity : AppCompatActivity(), ClickListener, DragSelectRecyclerViewAdapter.SelectionListener {
    private var mShare: MenuItem? = null
    private var mTag: MenuItem? = null
    private var mDelete: MenuItem? = null
    private var mPdfExport: MenuItem? = null
    private lateinit var recyclerView: DragSelectRecyclerView
    private lateinit var deleteConfirmBuilder: AlertDialog.Builder
    private var selectionMode = false
    private lateinit var mImageLoader: ImageLoader
    private lateinit var mTargetSize: ImageSize
    private lateinit var mSharedPref: SharedPreferences
    override fun onClick(index: Int) {
        if (selectionMode) {
            myThumbAdapter!!.toggleSelected(index)
        } else {
            val i = Intent(this, FullScreenViewActivity::class.java)
            i.putExtra("position", index)
            this.startActivity(i)
        }
    }

    override fun onLongClick(index: Int) {
        if (!selectionMode) {
            setSelectionMode(true)
        }
        recyclerView.setDragSelectActive(true, index)
    }

    private fun setSelectionMode(selectionMode: Boolean) {
        if (mShare != null && mDelete != null) {
            mShare!!.isVisible = selectionMode
            //mTag.setVisible(selectionMode);
            mDelete!!.isVisible = selectionMode
        }
        if (mPdfExport != null) {
            mPdfExport!!.isVisible = selectionMode
        }
        this.selectionMode = selectionMode
    }

    override fun onDragSelectionChanged(i: Int) {
        Log.d(TAG, "DragSelectionChanged: $i")
        setSelectionMode(i > 0)
    }

    inner class ThumbAdapter(activity: GalleryGridActivity?, files: ArrayList<String>) : DragSelectRecyclerViewAdapter<ThumbViewHolder>() {
        private val mCallback: ClickListener?
        var itemList = ArrayList<String>()
        fun add(path: String) {
            itemList.add(path)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.gallery_item, parent, false)
            return ThumbViewHolder(v)
        }

        override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
            super.onBindViewHolder(holder, position) // this line is important!
            val filename = itemList[position]
            if (filename != holder.filename) {

                // remove previous image
                holder.image.setImageBitmap(null)

                // Load image, decode it to Bitmap and return Bitmap to callback
                mImageLoader.displayImage("file:///$filename", holder.image, mTargetSize)

                // holder.image.setImageBitmap(decodeSampledBitmapFromUri(filename, 220, 220));
                holder.filename = filename
            }
            if (isIndexSelected(position)) {
                holder.image.setColorFilter(Color.argb(140, 0, 255, 0))
            } else {
                holder.image.setColorFilter(Color.argb(0, 0, 0, 0))
            }
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        val selectedFiles: ArrayList<String>
            get() {
                val selection = ArrayList<String>()
                for (i in selectedIndices) {
                    selection.add(itemList[i!!])
                }
                return selection
            }

        inner class ThumbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, OnLongClickListener {
            val image: ImageView
            var filename: String? = null
            override fun onClick(v: View) {
                // Forwards to the adapter's constructor callback
                mCallback?.onClick(adapterPosition)
            }

            override fun onLongClick(v: View): Boolean {
                // Forwards to the adapter's constructor callback
                mCallback?.onLongClick(adapterPosition)
                return true
            }

            init {
                image = itemView.findViewById<View>(R.id.gallery_image) as ImageView
                image.scaleType = ImageView.ScaleType.CENTER_CROP
                // this.image.setPadding(8, 8, 8, 8);
                this.itemView.setOnClickListener(this)
                this.itemView.setOnLongClickListener(this)
            }
        }

        // Constructor takes click listener callback
        init {
            mCallback = activity
            for (file in files) {
                add(file)
            }
            setSelectionListener(activity)
        }
    }

    var myThumbAdapter: ThumbAdapter? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        setContentView(R.layout.activity_gallery)
        val actionBar = supportActionBar!!
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setTitle(null)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp)
        val config = ImageLoaderConfiguration.Builder(this).build()
        mImageLoader = ImageLoader.getInstance()
        mImageLoader.init(config)
        mTargetSize = ImageSize(220, 220) // result Bitmap will be fit to this size
        val ab = ArrayList<String>()
        myThumbAdapter = ThumbAdapter(this, ab)
        // new Utils(getApplicationContext()).getFilePaths(););
        recyclerView = findViewById<View>(R.id.recyclerview) as DragSelectRecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.setAdapter(myThumbAdapter)
        deleteConfirmBuilder = AlertDialog.Builder(this)
        deleteConfirmBuilder.setTitle(getString(R.string.confirm_title))
        deleteConfirmBuilder.setMessage(getString(R.string.confirm_delete_multiple_text))
        deleteConfirmBuilder.setPositiveButton(getString(R.string.answer_yes)) { dialog: DialogInterface, which: Int ->
            deleteImage()
            dialog.dismiss()
        }
        deleteConfirmBuilder.setNegativeButton(getString(R.string.answer_no)) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
    }

    private fun reloadAdapter() {
        recyclerView.setAdapter(null)

        // ArrayList<String> ab = new ArrayList<>();
        myThumbAdapter = ThumbAdapter(this, Utils(applicationContext).filePaths)
        recyclerView.setAdapter(myThumbAdapter)
        recyclerView.invalidate()
        setSelectionMode(false)
    }

    public override fun onResume() {
        super.onResume()
        reloadAdapter()
    }

    private fun deleteImage() {
        for (filePath in myThumbAdapter!!.selectedFiles) {
            val photoFile = File(filePath)
            if (photoFile.delete()) {
                removeImageFromGallery(filePath, this)
                Log.d(TAG, "Removed file: $filePath")
            }
        }
        reloadAdapter()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_gallery, menu)
        mShare = menu.findItem(R.id.action_share).also {
            it.isVisible = false
        }
        mTag = menu.findItem(R.id.action_tag)
        // mTag.setVisible(false);
        mDelete = menu.findItem(R.id.action_delete).also {
            it.isVisible = false
        }
        mPdfExport = menu.findItem(R.id.action_pdfexport).also {
            it.isVisible = false
        }
        invalidateOptionsMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        when (id) {
            android.R.id.home -> finish()
            R.id.action_share -> {
                shareImages()
                return true
            }
            R.id.action_pdfexport -> {
                pdfExport()
                return true
            }
            R.id.action_tag -> {
            }
            R.id.action_delete -> {
                deleteConfirmBuilder.create().show()
                return true
            }
            R.id.action_about -> {
                val fm = supportFragmentManager
                val aboutDialog = AboutFragment()
                aboutDialog.show(fm, "about_view")
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun pdfExport() {
        val pdfFilePath = mergeImagesToPdf(applicationContext, myThumbAdapter!!.selectedFiles)
        if (pdfFilePath != null) {
            try {
                val file = File(pdfFilePath)
                val i = Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(applicationContext,
                        "$packageName.fileprovider", file))
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(i)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(applicationContext, "Cant Find Your File", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareImages() {
        val selectedFiles = myThumbAdapter!!.selectedFiles
        if (selectedFiles.size == 1) {
            /* Only one scanned document selected: ACTION_SEND intent */
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/jpg"
            val uri = FileProvider.getUriForFile(applicationContext, "$packageName.fileprovider", File(selectedFiles[0]))
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            Log.d("GalleryGridActivity", "uri $uri")
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_snackbar)))
        } else {
            val filesUris = ArrayList<Uri>()
            for (i in myThumbAdapter!!.selectedFiles) {
                val uri = FileProvider.getUriForFile(applicationContext, "$packageName.fileprovider", File(i))
                filesUris.add(uri)
                Log.d("GalleryGridActivity", "uri $uri")
            }
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            shareIntent.type = "image/jpg"
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesUris)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_snackbar)))
        }
    }

    companion object {
        private const val TAG = "GalleryGridActivity"
    }
}