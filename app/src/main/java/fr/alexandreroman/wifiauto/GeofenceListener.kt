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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

/**
 * [BroadcastReceiver] implementation listening to geofence notifications.
 * When the device is known to be within a geofence, Wi-Fi signal is automatically enabled.
 * @author Alexandre Roman
 */
class GeofenceListener : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (!context!!.sharedPreferences.geofenceEnabled) {
            Timber.w("Geofence triggered while user disabled this feature")
            return
        }

        val geofenceEvent = GeofencingEvent.fromIntent(intent)
        if (geofenceEvent.hasError()) {
            // Most probable error: location services disabled by user.
            Timber.w("Geofence not available: code=%d reason=%s",
                    geofenceEvent.errorCode, GeofenceStatusCodes.getStatusCodeString(geofenceEvent.errorCode))
            return
        }

        val wifiMan = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiMan.isWifiEnabled) {
            Timber.i("Enabling Wi-Fi inside geofence")
            if (BuildConfig.DEBUG) {
                EventLog.from(context).append("Wi-Fi enabled within geofence")
            }
            wifiMan.isWifiEnabled = true
        } else {
            Timber.i("Wi-Fi already enabled within geofence")
        }
    }
}
