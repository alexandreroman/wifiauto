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
import android.content.Intent
import android.support.v4.app.JobIntentService
import timber.log.Timber

/**
 * Service used to setup Wi-Fi monitoring.
 * @author Alexandre Roman
 */
class SetupService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        when (intent?.action) {
            ACTION_SETUP -> {
                handleActionSetup()
            }
        }
    }

    /**
     * Handle setup for Wi-Fi monitoring.
     */
    private fun handleActionSetup() {
        Timber.i("Setup Wi-Fi monitoring")
        val enabled = sharedPreferences.wifiMonitoringEnabled
        Timber.d("Wi-Fi monitoring is ${if (enabled) "enabled" else "disabled"}")

        if (enabled) {
            MonitoringService.schedule(this)
        } else {
            MonitoringService.cancelScheduling(this)
        }
    }

    companion object {
        private const val ACTION_SETUP = "fr.alexandreroman.wifiauto.action.SETUP"
        private const val JOB_SETUP = 12

        /**
         * Start this service to setup Wi-Fi monitoring.
         */
        @JvmStatic
        fun setup(context: Context) {
            val intent = Intent(context, SetupService::class.java).apply {
                action = ACTION_SETUP
            }
            Timber.i("Starting Wi-Fi monitoring setup")
            JobIntentService.enqueueWork(context, SetupService::class.java, JOB_SETUP, intent)
        }
    }
}
