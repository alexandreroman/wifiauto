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
import com.google.android.gms.location.LocationResult
import timber.log.Timber

/**
 * [BroadcastReceiver] implementation listening to location updates.
 * @author Alexandre Roman
 */
class LocationListener : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Check if a location is available in the intent.
        if (LocationResult.hasResult(intent)) {
            val locResult = LocationResult.extractResult(intent)
            val loc = locResult.lastLocation
            if (loc != null) {
                Timber.i("Received location update: %s", loc)
                if (BuildConfig.DEBUG) {
                    EventLog.from(context!!).append("Received location update")
                }
                // Create a geofence using this location.
                GeofenceWorker.start(loc)
            }
        }
    }
}