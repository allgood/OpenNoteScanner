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

import com.todobom.opennotescanner.helpers.AboutFragment;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.todobom.opennotescanner.helpers.Utils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    private SettingsFragment sf;

    private static final String TAG = "SettingsActivity";

    private RewardedAd rewardedAd;
    private Preference rewardPreference;
    private Preference googlePlayPreference;
    private BillingClient billingClient;
    private ArrayList<String> skuList;
    private SkuDetails skuDetails;

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

        Preference aboutPreference = sf.findPreference("about_preference");
        aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
                AboutFragment aboutDialog = new AboutFragment();
                aboutDialog.show(fm, "about_view");
                return true;
            }
        });

        PreferenceCategory donateCategory = (PreferenceCategory) sf.findPreference("donate_pref_category");
        Preference bitcoinPref = sf.findPreference("donate_bitcoin");

        if (donateCategory != null && bitcoinPref != null && !Utils.isPackageInstalled(this,"de.schildbach.wallet")) {
            donateCategory.removePreference(bitcoinPref);
        }

        monetize();
    }

    private void monetize() {
        rewardedAd = new RewardedAd(this,
                getString(R.string.admob_rewardedad_id));
        rewardPreference = sf.findPreference("reward_ad");
        googlePlayPreference = sf.findPreference("googleplay");

        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                Log.d(TAG, "Ad loaded" );
                rewardPreference.setEnabled(true);
            }

            @Override
            public void onRewardedAdFailedToLoad(int errorCode) {
                Log.d(TAG, "Ad load Failed" );
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);

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

        skuList = new ArrayList<String>();
        skuList.add("one_year_donation");

        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    int rc = billingResult.getResponseCode();
                                    if ( rc == BillingClient.BillingResponseCode.OK && skuDetailsList != null && skuDetailsList.size() > 0) {
                                        skuDetails = skuDetailsList.get(0);
                                        googlePlayPreference.setEnabled(true);
                                    } else {
                                        googlePlayPreference.setEnabled(false);
                                    }
                                }
                            });
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                googlePlayPreference.setEnabled(false);
                billingClient.startConnection(this);
            }
        });

        googlePlayPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (skuDetails != null) {
                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build();
                    BillingResult billingResult = billingClient.launchBillingFlow(SettingsActivity.this, billingFlowParams);
                }

                return true;
            }
        });

    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    private void handlePurchase(Purchase purchase) {
        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
                    mSharedPref.edit().putLong( "one_year_donation_time" , System.currentTimeMillis() / 1000L ).commit();
                }
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }


}
