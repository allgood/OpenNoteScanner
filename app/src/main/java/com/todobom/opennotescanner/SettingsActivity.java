package com.todobom.opennotescanner;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.v7.app.AppCompatActivity;

import com.todobom.opennotescanner.helpers.Utils;

public class SettingsActivity extends AppCompatActivity {

    private SettingsFragment sf;

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
        Preference dogecoinPref = sf.findPreference("donate_dogecoin");

        if (donateCategory != null && bitcoinPref != null && !Utils.isPackageInstalled(this,"de.schildbach.wallet")) {
            donateCategory.removePreference(bitcoinPref);
        }

        if (donateCategory != null && dogecoinPref != null && !Utils.isPackageInstalled(this,"de.langerhans.wallet")) {
            donateCategory.removePreference(dogecoinPref);
        }

    }
}
