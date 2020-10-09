package com.todobom.opennotescanner.helpers

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.todobom.opennotescanner.R

class ScanTopicDialogFragment : DialogFragment() {
    interface SetTopicDialogListener {
        fun onFinishTopicDialog(inputText: String?)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = AlertDialog.Builder(activity!!)
        alertDialogBuilder.setTitle(R.string.set_scan_topic)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.dialog_scan_topic, null)
        alertDialogBuilder.setView(view)
        val scanTopic = view.findViewById<EditText>(R.id.editTextScanTopic)
        alertDialogBuilder.setPositiveButton(R.string.set_scan_topic) { dialog: DialogInterface?, which: Int ->
            val listener = activity as SetTopicDialogListener?
            listener!!.onFinishTopicDialog(scanTopic.text.toString())
            dismiss()
            (context as Activity?)!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        alertDialogBuilder.setNegativeButton(R.string.skip_scan_topic) { dialog: DialogInterface?, which: Int ->
            if (dialog != null) {
                val listener = activity as SetTopicDialogListener?
                listener!!.onFinishTopicDialog(null)
                dialog.dismiss()
                (context as Activity?)!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
        return alertDialogBuilder.create()
    }

    init {
        isCancelable = false
    }
}