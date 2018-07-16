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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers
import timber.log.Timber

/**
 * Fragment displaying application preferences.
 * @author Alexandre Roman
 */
class SettingsFragment : PreferenceFragmentCompatDividers(), SharedPreferences.OnSharedPreferenceChangeListener {
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

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
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

    private fun onToggleMonitoring(enabled: Boolean) {
        if (BuildConfig.DEBUG) {
            EventLog.from(requireContext()).append(
                    "Setup Wi-Fi monitoring: ${if (enabled) "enabled" else "disabled"}")
        }
        if (enabled) {
            MonitoringWorker.schedule()
        } else {
            MonitoringWorker.cancelScheduling(requireContext())
        }
    }

    private fun onToggleGeofence(enabled: Boolean) {
        if (BuildConfig.DEBUG) {
            EventLog.from(requireContext()).append(
                    "Geofence: ${if (enabled) "enabled" else "disabled"}")
        }
        if (enabled) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERM_REQ)
            } else {
                doStartGeofence()
            }
        } else {
            LocationWorker.cancel(requireContext())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERM_REQ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doStartGeofence()
            } else {
                disableGeofencePref()
            }
        }
    }

    private fun disableGeofencePref() {
        (preferenceManager.findPreference("pref_key_service_geofence") as CheckBoxPreference).isChecked = false
    }

    private fun doStartGeofence() {
        Timber.i("Checking device configuration before acquiring location")

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val settingsReq = LocationSettingsRequest.Builder()
                .addLocationRequest(LocationWorker.createLocationRequest())
                .setAlwaysShow(true).build()
        settingsClient.checkLocationSettings(settingsReq).addOnCompleteListener {
            try {
                it.getResult(ApiException::class.java)
                // This device has a valid configuration: go on!
                Timber.i("Device configuration is OK: starting location process")
                LocationWorker.start()
            } catch (e: ApiException) {
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        // Location settings are not satisfied, but we can do something about it.
                        try {
                            Timber.i("Cannot start location process: asking user to enable location services")
                            val intentSender = (e as ResolvableApiException).resolution.intentSender
                            startIntentSenderForResult(intentSender, LOCATION_SETTINGS_REQ,
                                    null, 0, 0, 0, null)
                        } catch (ignore: Exception) {
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // Oops: there's nothing we can do to satisfy our location request.
                        Timber.w("Cannot start location process: this device cannot meet our location needs")
                        disableGeofencePref()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOCATION_SETTINGS_REQ -> {
                if (resultCode == Activity.RESULT_OK) {
                    doStartGeofence()
                } else {
                    Timber.w("User didn't accept to change location settings")
                    disableGeofencePref()
                }
            }
        }
    }

    private fun onEventLog() = startActivity(Intent(requireContext(), EventLogActivity::class.java))

    override fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {
        when (key) {
            "pref_key_service_enabled" -> onToggleMonitoring(pref!!.wifiMonitoringEnabled)
            "pref_key_service_geofence" -> onToggleGeofence(pref!!.geofenceEnabled)
        }
    }

    companion object {
        private const val LOCATION_PERM_REQ = 42
        private const val LOCATION_SETTINGS_REQ = 27
    }
}
