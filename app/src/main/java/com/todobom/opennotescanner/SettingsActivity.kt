package com.todobom.opennotescanner

import android.os.Bundle
import android.preference.Preference
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceCategory
import androidx.appcompat.app.AppCompatActivity
import com.todobom.opennotescanner.helpers.AboutFragment
import com.todobom.opennotescanner.helpers.Utils.Companion.isPackageInstalled

class SettingsActivity : AppCompatActivity() {
    private lateinit var sf: SettingsFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val fm = fragmentManager
        val ft = fm.beginTransaction()
        sf = SettingsFragment()
        ft.replace(android.R.id.content, sf)
        ft.commit()
    }

    override fun onResume() {
        super.onResume()
        val aboutPreference = sf.findPreference("about_preference")
        aboutPreference.onPreferenceClickListener = OnPreferenceClickListener { preference: Preference? ->
            val fm = supportFragmentManager
            val aboutDialog = AboutFragment()
            aboutDialog.show(fm, "about_view")
            true
        }
        val donateCategory = sf.findPreference("donate_pref_category") as PreferenceCategory
        val bitcoinPref = sf.findPreference("donate_bitcoin")
        if (bitcoinPref != null && !isPackageInstalled(this, "de.schildbach.wallet")) {
            donateCategory.removePreference(bitcoinPref)
        }
    }
}