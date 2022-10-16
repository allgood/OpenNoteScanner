package com.todobom.opennotescanner.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import com.todobom.opennotescanner.R
import java.io.IOException

/**
 * Created by allgood on 29/05/16.
 */
class TagEditorFragment : DialogFragment() {
    private var mRunOnDetach: Runnable? = null
    private var filePath: String? = null
    var stdTagsState = BooleanArray(7)
    var stdTags = arrayOf("rocket", "gift", "tv", "bell", "game", "star", "magnet")
    var stdTagsButtons = arrayOfNulls<ImageView>(7)
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
        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(filePath!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val userComment = exif!!.getAttribute("UserComment")
        for (i in 0..6) {
            stdTagsState[i] = userComment!!.contains("<" + stdTags[i] + ">")
        }
    }

    private fun saveTags() {
        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(filePath!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var userComment = exif!!.getAttribute("UserComment")
        for (i in 0..6) {
            if (stdTagsState[i] && !userComment!!.contains("<" + stdTags[i] + ">")) {
                userComment += "<" + stdTags[i] + ">"
            } else if (!stdTagsState[i] && userComment!!.contains("<" + stdTags[i] + ">")) {
                userComment!!.replace("<" + stdTags[i] + ">".toRegex(), "")
            }
        }
        exif.setAttribute("UserComment", userComment)
        try {
            exif.saveAttributes()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (mRunOnDetach != null) {
            mRunOnDetach!!.run()
        }
    }

    fun setRunOnDetach(runOnDetach: Runnable?) {
        mRunOnDetach = runOnDetach
    }

    fun setFilePath(filePath: String?) {
        this.filePath = filePath
        loadTags()
    }

    init {
        retainInstance = true
    }
}