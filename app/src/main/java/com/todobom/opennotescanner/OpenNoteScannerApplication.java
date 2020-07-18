package com.todobom.opennotescanner;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.matomo.sdk.TrackerBuilder;
import org.matomo.sdk.dispatcher.Packet;
import org.matomo.sdk.extra.DownloadTracker;
import org.matomo.sdk.extra.MatomoApplication;
import org.matomo.sdk.extra.TrackHelper;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by allgood on 23/04/16.
 */
public class OpenNoteScannerApplication extends MatomoApplication {
    private boolean mOptOut;

    SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("usage_stats")) {
                mOptOut = !sharedPreferences.getBoolean("usage_stats", false);
                getTracker().setOptOut(mOptOut);

                // when user opt-in, register the download
                if (!mOptOut) {
                    trackDownload();
                }
            }
        }
    };

    @Override
    public TrackerBuilder onCreateTrackerConfig() {
        return TrackerBuilder.createDefault("https://stats.todobom.com/matomo.php", 2);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initMatomo();
    }

    private void initMatomo() {
        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // enable usage stats on google play
        if (BuildConfig.FLAVOR.equals("gplay") && mSharedPref.getBoolean("isFirstRun",true)) {
            mSharedPref.edit().putBoolean("usage_stats", true).apply();
            mSharedPref.edit().putBoolean("isFirstRun", false).apply();
        }

        // usage stats is optional and only when not debugging
        mOptOut = !mSharedPref.getBoolean("usage_stats", false);
        getTracker().setOptOut(mOptOut);

        mSharedPref.registerOnSharedPreferenceChangeListener( mPreferenceChangeListener );

        // When working on an app we don't want to skew tracking results.
        getTracker().setDryRunTarget(BuildConfig.DEBUG ? Collections.synchronizedList(new ArrayList<Packet>()) : null);

        // If you want to set a specific userID other than the random UUID token, do it NOW to ensure all future actions use that token.
        // Changing it later will track new events as belonging to a different user.
        // String userEmail = ....preferences....getString
        // getTracker().setUserId(userEmail);

        if (!mOptOut) {
            trackDownload();
        }
    }

    private void trackDownload() {
        // Track this app install, this will only trigger once per app version.
        TrackHelper.track().download().identifier(new DownloadTracker.Extra.ApkChecksum(this)).with(getTracker());
    }
}
