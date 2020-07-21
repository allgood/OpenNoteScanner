package com.todobom.opennotescanner;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import com.todobom.opennotescanner.helpers.AboutFragment;
import com.todobom.opennotescanner.helpers.Utils;

import androidx.appcompat.app.AppCompatActivity;

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

    }
}
