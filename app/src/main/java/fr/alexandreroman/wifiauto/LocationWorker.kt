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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import timber.log.Timber

/**
 * [Worker] implementation used to get current device location.
 * Once current location is known, a new worker is started to setup a geofence around this point.
 * @author Alexandre Roman
 */
class LocationWorker : Worker() {
    override fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("User has not granted access to device location")
            return Result.FAILURE
        }

        Timber.i("Acquiring device location")
        if (BuildConfig.DEBUG) {
            EventLog.from(applicationContext).append("Acquiring device location")
        }

        val locProvider = LocationServices.getFusedLocationProviderClient(applicationContext)
        // Our BroadcastReceiver will be called when a location is acquired.
        // Multiple calls are expected.
        locProvider.requestLocationUpdates(createLocationRequest(), createListenerPendingIntent())

        return Result.SUCCESS
    }

    private fun createListenerPendingIntent() =
            PendingIntent.getBroadcast(applicationContext, 0,
                    Intent(applicationContext, LocationListener::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

    override fun onStopped(cancelled: Boolean) {
        super.onStopped(cancelled)

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        Timber.i("Location acquisition cancelled")
        val locProvider = LocationServices.getFusedLocationProviderClient(applicationContext)
        locProvider.removeLocationUpdates(createListenerPendingIntent())
    }

    companion object {
        private const val LOCATION_TAG = "wifiauto.location"

        /**
         * Start acquiring device location, in order to setup a geofence.
         */
        @JvmStatic
        fun start() {
            WorkManager.getInstance()?.enqueue(
                    OneTimeWorkRequest.Builder(LocationWorker::class.java)
                            .addTag(LOCATION_TAG).build())
        }

        /**
         * Cancel location acquisition, if it's running.
         */
        @JvmStatic
        fun cancel(context: Context) {
            WorkManager.getInstance()?.cancelAllWorkByTag(LOCATION_TAG)
            GeofenceWorker.cancel(context)
        }

        /**
         * Create a [LocationRequest] instance used to get device location
         * using Google Play Services.
         */
        @JvmStatic
        fun createLocationRequest() =
                LocationRequest()
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setInterval(4000)
                        .setFastestInterval(1000)
                        .setExpirationDuration(1000 * 30)!!
    }
}
