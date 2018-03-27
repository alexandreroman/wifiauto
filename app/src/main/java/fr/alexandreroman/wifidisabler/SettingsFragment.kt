/*
 * (C) Copyright 2018 Alexandre Roman (http://alexandreroman.fr).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.wifidisabler

import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.Preference
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.net.toUri
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers
import timber.log.Timber

/**
 * Fragment displaying application preferences.
 * @author Alexandre Roman
 */
class SettingsFragment : PreferenceFragmentCompatDividers() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = context!!.sharedPreferencesName

        setPreferencesFromResource(R.xml.prefs, rootKey)

        preferenceScreen.findPreference("pref_key_service_enabled")
                .onPreferenceClickListener = Preference.OnPreferenceClickListener { onToggleMonitoring(); true }
        preferenceScreen.findPreference("pref_key_about_source_code")
                .onPreferenceClickListener = Preference.OnPreferenceClickListener { onShowSourceCode(); true }
        preferenceScreen.findPreference("pref_key_about_licenses")
                .onPreferenceClickListener = Preference.OnPreferenceClickListener { onShowLicenses(); true }
        preferenceScreen.findPreference("pref_key_about_version").summary = getApplicationVersion()
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
        val licenseIntent = Intent(activity, OssLicensesMenuActivity::class.java)
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
        return activity?.packageManager?.getPackageInfo(activity?.packageName, 0)?.versionName
                ?: "<dev>"
    }

    private fun onToggleMonitoring() {
        val ctx = context
        if (ctx != null) {
            SetupService.setup(ctx)
        }
    }
}
