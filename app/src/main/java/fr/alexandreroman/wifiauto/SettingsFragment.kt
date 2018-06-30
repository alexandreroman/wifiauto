/*
 * Copyright 2018 Alexandre Roman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.wifiauto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers
import timber.log.Timber

/**
 * Fragment displaying application preferences.
 * @author Alexandre Roman
 */
class SettingsFragment : PreferenceFragmentCompatDividers() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load default preference values.
        val ctx = requireContext()
        PreferenceManager.setDefaultValues(ctx, ctx.sharedPreferencesName,
                Context.MODE_PRIVATE, R.xml.prefs, false)
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = requireContext().sharedPreferencesName

        setPreferencesFromResource(R.xml.prefs, rootKey)

        preferenceScreen.findPreference("pref_key_service_enabled")
                .onPreferenceClickListener = Preference.OnPreferenceClickListener { onToggleMonitoring(); true }
        preferenceScreen.findPreference("pref_key_about_source_code")
                .onPreferenceClickListener = Preference.OnPreferenceClickListener { onShowSourceCode(); true }
        preferenceScreen.findPreference("pref_key_about_licenses")
                .onPreferenceClickListener = Preference.OnPreferenceClickListener { onShowLicenses(); true }
        preferenceScreen.findPreference("pref_key_about_version").summary = getApplicationVersion()

        val eventLogPref = preferenceScreen.findPreference("pref_key_event_log")
        if (!BuildConfig.DEBUG) {
            preferenceScreen.removePreference(eventLogPref)
        } else {
            eventLogPref.setOnPreferenceClickListener { onEventLog(); true }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        try {
            return super.onCreateView(inflater, container, savedInstanceState)
        } finally {
            setDividerPreferences(PreferenceFragmentCompatDividers.DIVIDER_PADDING_CHILD
                    or PreferenceFragmentCompatDividers.DIVIDER_CATEGORY_AFTER_LAST
                    or PreferenceFragmentCompatDividers.DIVIDER_CATEGORY_BETWEEN)
        }
    }

    private fun onShowLicenses() {
        Timber.i("Showing software licenses")
        val licenseIntent = Intent(context, OssLicensesMenuActivity::class.java)
        OssLicensesMenuActivity.setActivityTitle(getString(R.string.software_licenses))
        startActivity(licenseIntent)
    }

    private fun onShowSourceCode() {
        Timber.i("Showing application source code")
        val webPageUrl = getString(R.string.source_code_ref)
        val webIntent = Intent(Intent.ACTION_VIEW).setData(webPageUrl.toUri())
        startActivity(webIntent)
    }

    private fun getApplicationVersion(): String {
        return requireActivity().packageManager?.getPackageInfo(activity?.packageName, 0)?.versionName
                ?: "<dev>"
    }

    private fun onToggleMonitoring() {
        SetupService.setup(requireContext())
    }

    private fun onEventLog() = startActivity(Intent(requireContext(), EventLogActivity::class.java))
}
