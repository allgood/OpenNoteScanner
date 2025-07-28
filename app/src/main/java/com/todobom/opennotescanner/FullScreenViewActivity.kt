package com.todobom.opennotescanner

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.ImageSize
import com.todobom.opennotescanner.helpers.AboutFragment
import com.todobom.opennotescanner.helpers.Utils
import com.todobom.opennotescanner.helpers.Utils.Companion.maxTextureSize
import com.todobom.opennotescanner.helpers.Utils.Companion.removeImageFromGallery
import com.todobom.opennotescanner.views.TagEditorFragment

class FullScreenViewActivity : AppCompatActivity() {
    private lateinit var utils: Utils
    private lateinit var mAdapter: FullScreenImageAdapter
    private lateinit var mViewPager: ViewPager
    private lateinit var deleteConfirmBuilder: AlertDialog.Builder
    private lateinit var mImageLoader: ImageLoader
    private lateinit var mTargetSize: ImageSize

    private var mMaxTexture = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_view)
        mViewPager = findViewById<View>(R.id.pager) as ViewPager
        val actionBar = supportActionBar!!
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setTitle(null)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp)
        utils = Utils(applicationContext)
        val i = intent
        val position = i.getIntExtra("position", 0)

        // initialize Universal Image Loader
        val config = ImageLoaderConfiguration.Builder(this).build()
        mImageLoader = ImageLoader.getInstance()
        mImageLoader.init(config)
        mMaxTexture = maxTextureSize
        Log.d("FullScreenViewActivity", "gl resolution: $mMaxTexture")
        mTargetSize = ImageSize(mMaxTexture, mMaxTexture)
        mAdapter = loadAdapter()

        // displaying selected image first
        mViewPager.currentItem = position
        mViewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                Log.d("fullview", "scrolled position $position offset $positionOffset")
                Log.d("fullview", "pager " + mViewPager.currentItem)
            }

            override fun onPageSelected(position: Int) {
                Log.d("fullview", "selected")
                Log.d("fullview", "item" + mViewPager.currentItem)
            }

            override fun onPageScrollStateChanged(state: Int) {
                Log.d("fullview", "state changed")
            }
        })
        deleteConfirmBuilder = AlertDialog.Builder(this)
        deleteConfirmBuilder.setTitle(getString(R.string.confirm_title))
        deleteConfirmBuilder.setMessage(getString(R.string.confirm_delete_text))
        deleteConfirmBuilder.setPositiveButton(getString(R.string.answer_yes)) { dialog: DialogInterface, which: Int ->
            deleteImage()
            dialog.dismiss()
        }
        deleteConfirmBuilder.setNegativeButton(getString(R.string.answer_no)) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
    }

    private fun loadAdapter(): FullScreenImageAdapter {
        mViewPager.adapter = null
        val adapter = FullScreenImageAdapter(this@FullScreenViewActivity, utils.fileUris)
        adapter.setImageLoader(mImageLoader)
        adapter.setMaxTexture(mMaxTexture, mTargetSize)
        mViewPager.adapter = adapter

        return adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_imagepager, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        when (id) {
            android.R.id.home -> finish()
            R.id.action_tag -> {
                tagImage()
                return true
            }
            R.id.action_share -> {
                shareImage()
                return true
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

    private fun isPng(context: Context, uri: Uri): Boolean {
        // First try MIME type
        val type = context.contentResolver.getType(uri)
        if (type == "image/png") return true

        // Fallback: try extension
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return extension.equals("png", ignoreCase = true)
    }

    private fun tagImage() {
        val item = mViewPager.currentItem
        val fileUri = mAdapter.getUri(item)
        if (isPng(this, fileUri)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.format_not_supported)
            builder.setMessage(R.string.format_not_supported_message)
            builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            val alerta = builder.create()
            alerta.show()
            return
        }
        val tagEditorDialog = TagEditorFragment(fileUri)
        tagEditorDialog.show(supportFragmentManager, "tageditor_view")
    }

    private fun deleteImage() {
        val item = mViewPager.currentItem
        val fileUri = mAdapter.getUri(item)
        removeImageFromGallery(fileUri, this)
        loadAdapter()
        if (0 == mAdapter.count) finish()
        mViewPager.currentItem = item
    }

    fun shareImage() {
        val uri = mAdapter.getUri(mViewPager.currentItem)
        val shareIntent = Intent(Intent.ACTION_SEND)
        val shareUri = if (uri.scheme == "file") {
            FileProvider.getUriForFile(applicationContext, "$packageName.fileprovider", uri.toFile())
        } else {
            uri
        }
        shareIntent.type = this.contentResolver.getType(shareUri)
        shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri)
        Log.d("Fullscreen", "uri $shareUri")
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_snackbar)))
    }
}