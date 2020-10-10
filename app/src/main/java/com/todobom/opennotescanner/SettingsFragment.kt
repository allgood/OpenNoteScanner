package com.todobom.opennotescanner

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.*

/**
 * Created by allgood on 21/04/16.
 */
class SettingsFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        updatePreferenceScreen(preferenceScreen)
    }

    private fun updatePreferenceScreen(screen: PreferenceScreen) {
        for (i in 0 until screen.preferenceCount) {
            val preference = screen.getPreference(i)
            if (preference is PreferenceGroup) {
                for (j in 0 until preference.preferenceCount) {
                    val singlePref = preference.getPreference(j)
                    updatePreference(singlePref, singlePref.key)
                }
            } else {
                updatePreference(preference, preference.key)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        updatePreference(findPreference(key), key)
    }

    private fun updatePreference(preference: Preference?, key: String?) {
        if (preference == null) return
        if (preference is ListPreference) {
            val listPreference = preference
            listPreference.summary = listPreference.entry
            return
        }
        if (preference is EditTextPreference) {
            val sharedPrefs = preferenceManager.sharedPreferences
            preference.setSummary(sharedPrefs.getString(key, "Default"))
        }
        if (preference is PreferenceScreen) {
            updatePreferenceScreen(preference)
        }
    }
}