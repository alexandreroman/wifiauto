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
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import timber.log.Timber

/**
 * [Worker] implementation used to setup a geofence around a point:
 * if the device is inside the geofence, Wi-Fi signal will be enabled.
 * @author Alexandre Roman
 */
class GeofenceWorker : Worker() {
    override fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("User has not granted access to device location")
            return Result.FAILURE
        }

        val latitude = inputData.getDouble(ARG_LATITUDE, 0.0)
        val longitude = inputData.getDouble(ARG_LONGITUDE, 0.0)
        setupGeofence(applicationContext, latitude, longitude)

        return Result.SUCCESS
    }

    companion object {
        private const val ARG_LATITUDE = "geofence_latitude"
        private const val ARG_LONGITUDE = "geofence_longitude"
        private const val GEOFENCE_REQ_ID = "wifiauto"
        private const val GEOFENCE_TAG = "wifiauto.geofence"

        @JvmStatic
        fun start(loc: Location) {
            WorkManager.getInstance()?.enqueue(
                    OneTimeWorkRequest.Builder(GeofenceWorker::class.java)
                            .addTag(GEOFENCE_TAG)
                            .setInputData(
                                    Data.Builder()
                                            .putDouble(ARG_LATITUDE, loc.latitude)
                                            .putDouble(ARG_LONGITUDE, loc.longitude).build()).build())
        }

        @JvmStatic
        fun cancel(context: Context) {
            WorkManager.getInstance()?.cancelAllWorkByTag(GEOFENCE_TAG)

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Timber.i("Geofence cleared")
                val geofenceClient = LocationServices.getGeofencingClient(context)
                geofenceClient.removeGeofences(listOf(GEOFENCE_REQ_ID))
            }

            val prefs = context.sharedPreferences
            prefs.edit().remove(ARG_LATITUDE).remove(ARG_LONGITUDE).apply()
        }

        @JvmStatic
        fun reset(context: Context) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            val prefs = context.sharedPreferences
            val latitude = prefs.getFloat(ARG_LATITUDE, 0F)
            val longitude = prefs.getFloat(ARG_LONGITUDE, 0F)

            if (latitude == 0F && longitude == 0F) {
                Timber.d("Geofence not set")
                prefs.edit()
                        .remove(ARG_LATITUDE)
                        .remove(ARG_LONGITUDE)
                        .remove("pref_key_service_geofence")
                        .apply()
                return
            }
            setupGeofence(context, latitude.toDouble(), longitude.toDouble())
        }

        @SuppressLint("MissingPermission")
        private fun setupGeofence(context: Context, latitude: Double, longitude: Double) {
            val radius = 100F

            val geofenceReq = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
                    .addGeofence(
                            Geofence.Builder()
                                    .setCircularRegion(latitude, longitude, radius)
                                    .setLoiteringDelay(1000 * 60 * 2)
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_ENTER)
                                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                    .setRequestId(GEOFENCE_REQ_ID).build()).build()
            val geofenceClient = LocationServices.getGeofencingClient(context)
            geofenceClient.addGeofences(geofenceReq, PendingIntent.getBroadcast(context,
                    0, Intent(context, GeofenceListener::class.java), PendingIntent.FLAG_UPDATE_CURRENT))

            Timber.i("Geofence set: latitude=%f longitude=%f", latitude, longitude)
            if (BuildConfig.DEBUG) {
                EventLog.from(context).append("Geofence set")
            }

            val prefs = context.sharedPreferences
            prefs.edit()
                    .putFloat(ARG_LATITUDE, latitude.toFloat())
                    .putFloat(ARG_LONGITUDE, longitude.toFloat()).apply()
        }
    }
}
