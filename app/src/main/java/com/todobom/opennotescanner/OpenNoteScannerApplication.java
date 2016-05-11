package com.todobom.opennotescanner;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.Tracker;

/**
 * Created by allgood on 23/04/16.
 */
public class OpenNoteScannerApplication extends PiwikApplication {
    private SharedPreferences mSharedPref;
    private boolean mOptOut;

    SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("usage_stats")) {
                mOptOut = !sharedPreferences.getBoolean("usage_stats", false);
                getPiwik().setOptOut(mOptOut);

                // when user opt-in, register the download
                if (!mOptOut) {
                    getTracker().trackAppDownload(OpenNoteScannerApplication.this, Tracker.ExtraIdentifier.APK_CHECKSUM);
                }
            }
        }
    };

    @Override
    public String getTrackerUrl() {
        return "https://stats.todobom.com/";
    }

    @Override
    public Integer getSiteId() {
        return 2;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initPiwik();
    }


    private void initPiwik() {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // enable usage stats on google play
        if (BuildConfig.FLAVOR.equals("gplay") && mSharedPref.getBoolean("isFirstRun",true)) {
            mSharedPref.edit().putBoolean("usage_stats", true).commit();
            mSharedPref.edit().putBoolean("isFirstRun", false).commit();
        }

        // usage stats is optional and only when not debugging
        mOptOut = !mSharedPref.getBoolean("usage_stats", false);
        getPiwik().setOptOut(mOptOut);

        mSharedPref.registerOnSharedPreferenceChangeListener( mPreferenceChangeListener );

        // Print debug output when working on an app.
        getPiwik().setDebug(BuildConfig.DEBUG);

        // When working on an app we don't want to skew tracking results.
        getPiwik().setDryRun(BuildConfig.DEBUG);

        // If you want to set a specific userID other than the random UUID token, do it NOW to ensure all future actions use that token.
        // Changing it later will track new events as belonging to a different user.
        // String userEmail = ....preferences....getString
        // getTracker().setUserId(userEmail);

        // Track this app install, this will only trigger once per app version.
        // i.e. "http://com.piwik.demo:1/185DECB5CFE28FDB2F45887022D668B4"
        getTracker().trackAppDownload(this, Tracker.ExtraIdentifier.APK_CHECKSUM);
        // Alternative:
        // i.e. "http://com.piwik.demo:1/com.android.vending"
        // getTracker().trackAppDownload();
    }
}
