package com.todobom.opennotescanner.helpers

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.todobom.opennotescanner.OpenNoteScannerApplication
import com.todobom.opennotescanner.R
import org.matomo.sdk.extra.TrackHelper
import us.feras.mdv.MarkdownView

class AboutFragment : DialogFragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val aboutView = inflater.inflate(R.layout.about_view, container)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return aboutView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //These two in case the fragment detached
        val activity = activity ?: return
        val window = dialog?.window ?: return

        val markdownView = view.findViewById<MarkdownView>(R.id.about_markdown)
        markdownView.loadMarkdownFile("file:///android_asset/" + getString(R.string.about_filename))
        val size = Point()
        activity.windowManager.defaultDisplay.getRealSize(size)
        window.setLayout((size.x * 0.9).toInt(), (size.y * 0.9).toInt())
        window.setGravity(Gravity.CENTER)
        val about_shareapp = view.findViewById<View>(R.id.about_shareapp)
        about_shareapp.setOnClickListener {
            val shareBody = getString(R.string.share_app_body) + APP_LINK
            val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject))
                putExtra(Intent.EXTRA_TEXT, shareBody)
            }
            val tracker = (activity.application as OpenNoteScannerApplication).tracker
            TrackHelper.track().screen("/shareapp").title("Share Application").with(tracker)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_app_using)))
        }
    }

    companion object {
        private const val APP_LINK = "https://goo.gl/2JwEPq"
    }
}