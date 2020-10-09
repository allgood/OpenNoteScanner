package com.todobom.opennotescanner

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import org.matomo.sdk.TrackerBuilder
import org.matomo.sdk.extra.DownloadTracker.Extra.ApkChecksum
import org.matomo.sdk.extra.MatomoApplication
import org.matomo.sdk.extra.TrackHelper
import java.util.*

/**
 * Created by allgood on 23/04/16.
 */
class OpenNoteScannerApplication : MatomoApplication() {
    private var mOptOut = false
    private var mPreferenceChangeListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "usage_stats") {
            mOptOut = !sharedPreferences.getBoolean("usage_stats", false)
            tracker.isOptOut = mOptOut

            // when user opt-in, register the download
            if (!mOptOut) {
                trackDownload()
            }
        }
    }

    override fun onCreateTrackerConfig(): TrackerBuilder {
        return TrackerBuilder.createDefault("https://stats.todobom.com/matomo.php", 2)
    }

    override fun onCreate() {
        super.onCreate()
        initMatomo()
    }

    private fun initMatomo() {
        val mSharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // enable usage stats on google play
        if (BuildConfig.FLAVOR == "gplay" && mSharedPref.getBoolean("isFirstRun", true)) {
            mSharedPref.edit().putBoolean("usage_stats", true).apply()
            mSharedPref.edit().putBoolean("isFirstRun", false).apply()
        }

        // usage stats is optional and only when not debugging
        mOptOut = !mSharedPref.getBoolean("usage_stats", false)
        tracker.isOptOut = mOptOut
        mSharedPref.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener)

        // When working on an app we don't want to skew tracking results.
        tracker.dryRunTarget = if (BuildConfig.DEBUG) Collections.synchronizedList(ArrayList()) else null

        // If you want to set a specific userID other than the random UUID token, do it NOW to ensure all future actions use that token.
        // Changing it later will track new events as belonging to a different user.
        // String userEmail = ....preferences....getString
        // getTracker().setUserId(userEmail);
        if (!mOptOut) {
            trackDownload()
        }
    }

    private fun trackDownload() {
        // Track this app install, this will only trigger once per app version.
        TrackHelper.track().download().identifier(ApkChecksum(this)).with(tracker)
    }
}