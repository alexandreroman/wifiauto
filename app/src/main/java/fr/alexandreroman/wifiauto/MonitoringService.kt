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
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val JOB_MONITORING = 42

/**
 * Monitoring service started by JobScheduler.
 * @author Alexandre Roman
 */
class MonitoringService : JobService() {
    override fun onStopJob(params: JobParameters?): Boolean {
        Timber.i("Wi-Fi monitoring has stopped")
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Timber.i("Wi-Fi monitoring has started")

        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetInfo = connManager.activeNetworkInfo
        var connectedToWifi = activeNetInfo?.type == ConnectivityManager.TYPE_WIFI

        if (connectedToWifi) {
            connectedToWifi = activeNetInfo.isConnectedOrConnecting
        }

        if (!connectedToWifi) {
            disableWifi()
        } else {
            Timber.i("Keep current settings: Wi-Fi is still enabled")
        }

        return false
    }

    fun disableWifi() {
        val wifiMan = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiMan.isWifiEnabled) {
            Timber.i("Disabling Wi-Fi")
            wifiMan.isWifiEnabled = false
        } else {
            Timber.i("Wi-Fi is already disabled")
        }
    }

    companion object {
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
