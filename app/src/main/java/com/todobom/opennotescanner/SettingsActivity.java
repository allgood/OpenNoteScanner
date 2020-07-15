package com.todobom.opennotescanner;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.todobom.opennotescanner.helpers.Utils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SettingsFragment sf;

    private static final String TAG = "SettingsActivity";
    private RewardedAd rewardedAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        FragmentManager fm=getFragmentManager();
        FragmentTransaction ft=fm.beginTransaction();

        sf=new SettingsFragment();
        ft.replace(android.R.id.content, sf);
        ft.commit();

    }

    @Override
    protected void onResume() {
        super.onResume();

        PreferenceCategory donateCategory = (PreferenceCategory) sf.findPreference("donate_pref_category");
        Preference bitcoinPref = sf.findPreference("donate_bitcoin");

        if (donateCategory != null && bitcoinPref != null && !Utils.isPackageInstalled(this,"de.schildbach.wallet")) {
            donateCategory.removePreference(bitcoinPref);
        }


        // code that follows handle admob and in-app purchases
        rewardedAd = new RewardedAd(this,
                getString(R.string.admob_rewardedad_id));

        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                Log.d(TAG, "Ad loaded" );
            }

            @Override
            public void onRewardedAdFailedToLoad(int errorCode) {
                Log.d(TAG, "Ad load Failed" );
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);

        Preference rewardPreference = sf.findPreference("reward_ad");
        rewardPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (rewardedAd.isLoaded()) {
                    Activity activityContext = SettingsActivity.this;
                    RewardedAdCallback adCallback = new RewardedAdCallback() {
                        @Override
                        public void onRewardedAdOpened() {
                            Log.d(TAG, "Ad opened" );
                        }

                        @Override
                        public void onRewardedAdClosed() {
                            Log.d(TAG, "Ad closed" );
                        }

                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                            Log.d(TAG, "Ad earned" );
                            SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);

                            mSharedPref.edit().putLong( "rewarded_time" , System.currentTimeMillis() / 1000L ).commit();
                        }

                        @Override
                        public void onRewardedAdFailedToShow(int errorCode) {
                            Log.d(TAG, "Ad failed to show" );
                        }
                    };
                    rewardedAd.show(activityContext, adCallback);
                } else {
                    Log.d("TAG", "The rewarded ad wasn't loaded yet.");
                }
                return true;
            }
        });

    }
}
