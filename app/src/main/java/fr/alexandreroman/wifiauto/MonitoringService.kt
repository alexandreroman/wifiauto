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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat

/**
 * [Service] implementation notifying user when this application is monitoring Wi-Fi.
 * This notification will help the application to stay in the background execution limits whitelist,
 * preventing this process from being "killed" by the operating system.
 * This service literally does nothing other than displaying a visible notification.
 * @author Alexandre Roman
 */
class MonitoringService : Service() {
    companion object {
        private const val NOTIF_CHANNEL_ID = "notif.monitoring"
        private const val EXTRA_HIDE_NOTIF = "hide_notif"

        @JvmStatic
        fun start(context: Context) {
            ContextCompat.startForegroundService(context,
                    Intent(context, MonitoringService::class.java))
        }

        @JvmStatic
        fun stop(context: Context) {
            context.stopService(
                    Intent(context, MonitoringService::class.java)
                            .putExtra(EXTRA_HIDE_NOTIF, true))
        }
    }

    private object NoOpBinder : Binder()

    override fun onBind(intent: Intent?): IBinder {
        return NoOpBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_HIDE_NOTIF, false) == true) {
            stopForeground(true)
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifChan = NotificationChannel(NOTIF_CHANNEL_ID,
                    getString(R.string.notif_channel_monitoring_wifi),
                    NotificationManager.IMPORTANCE_LOW)
            val notifMan = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifMan.createNotificationChannel(notifChan)
        }
        val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setContentText(getString(R.string.service_monitoring_wifi))
                .setSmallIcon(R.drawable.baseline_network_check_24)
                .setOngoing(true)
                .setShowWhen(false)
                .setLocalOnly(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(
                        PendingIntent.getActivity(this, 0,
                                Intent(this, MainActivity::class.java),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        startForeground(R.string.service_monitoring_wifi, notif)

        return START_NOT_STICKY
    }
}
