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
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat

/**
 * Event log.
 * @author Alexandre Roman
 */
class EventLog private constructor(
        private val context: Context,
        private val outputFile: File = File(context.filesDir, "events.log")) {

    companion object {
        @JvmStatic
        fun from(context: Context): EventLog {
            return EventLog(context)
        }
    }

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    /**
     * Append an entry to the event log.
     */
    fun append(message: String) {
        val now = System.currentTimeMillis()
        val nowStr = timeFormatter.format(now)
        val eventStr = "$nowStr $message\n"

        val writer = FileOutputStream(outputFile, true).bufferedWriter()
        writer.use { it.write(eventStr) }

        if (outputFile.length() > 1024 * 128) {
            reset()
        }
    }

    /**
     * Read all entries.
     */
    fun readAll(): List<String> {
        val events = mutableListOf<String>()
        outputFile.forEachLine { events.add(it) }
        return events
    }

    /**
     * Reset event log: all entries are cleared.
     */
    fun reset() {
        Timber.i("Resetting event log")
        FileOutputStream(outputFile).use { it.channel.truncate(0) }
    }
}
