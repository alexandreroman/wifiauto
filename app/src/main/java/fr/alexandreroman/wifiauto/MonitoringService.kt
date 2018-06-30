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

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Monitoring service started by [JobScheduler].
 * @author Alexandre Roman
 */
class MonitoringService : JobService() {
    override fun onStopJob(params: JobParameters?): Boolean {
        Timber.i("Wi-Fi monitoring has stopped")
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Timber.i("Starting Wi-Fi monitoring")

        if (BuildConfig.DEBUG) {
            EventLog.from(this).append("Monitoring Wi-Fi")
        }

        val wifiMan = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiMan.isWifiEnabled) {
            Timber.i("Wi-Fi is already disabled")

            if (BuildConfig.DEBUG) {
                EventLog.from(this).append("Wi-Fi is already disabled")
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
                    EventLog.from(this).append("Wi-Fi has been disabled")
                }
            } else {
                Timber.i("Device is connected via Wi-Fi: keep current settings")

                if (BuildConfig.DEBUG) {
                    EventLog.from(this).append("Keep Wi-Fi running")
                }
            }
        }

        // Next line is very important: we need to tell JobScheduler our job is done here,
        // so that it can safely reschedule this task.
        jobFinished(params, true)

        Timber.i("Wi-Fi monitoring is done")

        return false
    }

    companion object {
        private const val JOB_MONITORING = 42

        @JvmStatic
        fun schedule(context: Context) {
            Timber.i("Scheduling Wi-Fi monitoring")

            val jobService = ComponentName(context, MonitoringService::class.java)
            val jobBuilder = JobInfo.Builder(JOB_MONITORING, jobService).apply {
                setPersisted(true)
                setPeriodic(TimeUnit.MINUTES.toMillis(15))
            }

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(jobBuilder.build())
        }

        @JvmStatic
        fun cancelScheduling(context: Context) {
            Timber.i("Canceling Wi-Fi monitoring schedule")
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_MONITORING)
        }
    }
}
