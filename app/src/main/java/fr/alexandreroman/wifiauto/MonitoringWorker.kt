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
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * [Worker] implementation monitoring Wi-Fi connectivity.
 * @author Alexandre Roman
 */
class MonitoringWorker : Worker() {
    override fun doWork(): Result {
        Timber.i("Starting Wi-Fi monitoring")

        // Notify user that Wi-Fi monitoring is enabled.
        MonitoringService.start(applicationContext)

        if (BuildConfig.DEBUG) {
            EventLog.from(applicationContext).append("Monitoring Wi-Fi")
        }

        val wifiMan = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiMan.isWifiEnabled) {
            Timber.i("Wi-Fi is already disabled")

            if (BuildConfig.DEBUG) {
                EventLog.from(applicationContext).append("Wi-Fi is already disabled")
            }
        } else if (applicationContext.isWifiGracePeriodEnabled()) {
            Timber.i("Wi-Fi grace period enabled")

            if (BuildConfig.DEBUG) {
                EventLog.from(applicationContext).append("Wi-Fi grace period enabled")
            }
        } else {
            // Check if there is a running network using Wi-Fi.
            // At this point, we don't need to check whether this network configuration
            // provides Internet connectivity or not.
            // We just want to know if Wi-Fi is enabled on this device for nothing
            // (ie no network connected).
            val wifiInfo = wifiMan.connectionInfo
            val connectedViaWifi = wifiInfo != null
                    && wifiInfo.networkId != -1
                    && wifiInfo.supplicantState == SupplicantState.COMPLETED

            if (!connectedViaWifi) {
                Timber.i("Wi-Fi is enabled but no active connection has been detected")
                wifiMan.isWifiEnabled = false
                Timber.i("Wi-Fi is disabled")

                if (BuildConfig.DEBUG) {
                    EventLog.from(applicationContext).append("Wi-Fi has been disabled")
                }
            } else {
                Timber.i("Device is connected via Wi-Fi: keep current settings")

                if (BuildConfig.DEBUG) {
                    EventLog.from(applicationContext).append("Keep Wi-Fi running")
                }
            }
        }

        Timber.i("Wi-Fi monitoring is done")

        return Result.SUCCESS
    }

    companion object {
        private const val MONITORING_TAG = "wifiauto.monitoring"

        @JvmStatic
        fun schedule() {
            Timber.i("Scheduling Wi-Fi monitoring")
            val req = PeriodicWorkRequest.Builder(MonitoringWorker::class.java,
                    15, TimeUnit.MINUTES)
                    .addTag(MONITORING_TAG)
                    .build()
            WorkManager.getInstance()?.enqueue(req)
        }

        @JvmStatic
        fun cancelScheduling(context: Context) {
            Timber.i("Canceling Wi-Fi monitoring enable")
            WorkManager.getInstance()?.cancelAllWorkByTag(MONITORING_TAG)
            MonitoringService.stop(context)
        }
    }
}
