package com.todobom.opennotescanner.views

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import com.todobom.opennotescanner.R
import java.io.File
import java.io.IOException

/**
 * Created by allgood on 29/05/16.
 */
class TagEditorFragment(val fileUri: Uri) : DialogFragment() {
    private var mRunOnDetach: Runnable? = null
    var stdTagsState = BooleanArray(7)
    var stdTags = arrayOf("rocket", "gift", "tv", "bell", "game", "star", "magnet")
    var stdTagsButtons = arrayOfNulls<ImageView>(7)

    init {
        retainInstance = true
        loadTags()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val tagEditorView = inflater.inflate(R.layout.tageditor_view, container)
        dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        stdTagsButtons[0] = tagEditorView.findViewById(R.id.buttonRocket)
        stdTagsButtons[1] = tagEditorView.findViewById(R.id.buttonGift)
        stdTagsButtons[2] = tagEditorView.findViewById(R.id.buttonTv)
        stdTagsButtons[3] = tagEditorView.findViewById(R.id.buttonBell)
        stdTagsButtons[4] = tagEditorView.findViewById(R.id.buttonGame)
        stdTagsButtons[5] = tagEditorView.findViewById(R.id.buttonStar)
        stdTagsButtons[6] = tagEditorView.findViewById(R.id.buttonMagnet)
        for (i in 0..6) {
            stdTagsButtons[i]!!.setBackgroundTintList(ColorStateList.valueOf(if (stdTagsState[i]) -0xff198a else -0x5f5f60))
            stdTagsButtons[i]!!.setOnClickListener(View.OnClickListener { v: View ->
                val index = getTagIndex(v)
                stdTagsState[index] = !stdTagsState[index]
                v.backgroundTintList = ColorStateList.valueOf(if (stdTagsState[index]) -0xff198a else -0x5f5f60)
            })
        }
        val tagDoneButton = tagEditorView.findViewById<Button>(R.id.tag_done)
        tagDoneButton.setOnClickListener { v: View? ->
            saveTags()
            dismiss()
        }
        return tagEditorView
    }

    private fun getTagIndex(v: View): Int {
        for (i in 0..6) {
            if (stdTagsButtons[i] === v) {
                return i
            }
        }
        return -1
    }

    private fun loadTags() {
        val exif = try {
            context?.contentResolver?.openInputStream(fileUri)?.use {
                ExifInterface(it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        if (exif == null) return

        val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
        for (i in 0..6) {
            stdTagsState[i] = userComment!!.contains("<" + stdTags[i] + ">")
        }
    }

    private fun saveTags() {
        context?.let { context ->
            // save as temp file
            val inputStream = context.contentResolver?.openInputStream(fileUri)
            if (inputStream == null) return
            val tempFile = File.createTempFile("exif-edit", null, context.cacheDir)
            try {
                inputStream.use { input -> tempFile.outputStream().use { input.copyTo(it) } }

                // Open temp file for exif edit
                val exif = ExifInterface(tempFile.absolutePath)
                var userComment = exif.getAttribute("UserComment")
                for (i in 0..6) {
                    if (stdTagsState[i] && !userComment!!.contains("<" + stdTags[i] + ">")) {
                        userComment += "<" + stdTags[i] + ">"
                    } else if (!stdTagsState[i] && userComment!!.contains("<" + stdTags[i] + ">")) {
                        userComment!!.replace("<" + stdTags[i] + ">".toRegex(), "")
                    }
                }
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, userComment)
                exif.saveAttributes()

                // replace original with exif edited file
                context.contentResolver.openOutputStream(fileUri, "w")?.use { out ->
                    tempFile.inputStream().use { input -> input.copyTo(out) }
                }
            } finally {
                tempFile.delete()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mRunOnDetach?.run()
    }

    fun setRunOnDetach(runOnDetach: Runnable?) {
        mRunOnDetach = runOnDetach
    }
}