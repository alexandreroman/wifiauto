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

import android.arch.lifecycle.*
import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Activity displaying the event log.
 * @author Alexandre Roman
 */
class EventLogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_log)

        val listAdapter = EventLogAdapter()
        val list = findViewById<RecyclerView>(R.id.list)
        list.adapter = listAdapter
        list.layoutManager = LinearLayoutManager(this)

        val viewModel = EventLogViewModel.from(this)
        viewModel.events.observe(this, Observer { listAdapter.update(it.orEmpty()) })
        viewModel.update(this)
    }

    private class EventLogAdapter(private var events: MutableList<String> = mutableListOf()) : RecyclerView.Adapter<EventLogAdapter.ViewHolder>() {
        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val event = view.findViewById<TextView>(R.id.event)
        }

        fun update(newEvents: List<String>) {
            events.clear()
            events.addAll(newEvents)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false))

        override fun getItemCount() = events.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val event = events[position]
            holder.event.text = event
        }
    }

    class EventLogViewModel(val events: LiveData<List<String>> = MutableLiveData()) : ViewModel() {
        companion object {
            @JvmStatic
            fun from(activity: FragmentActivity): EventLogViewModel {
                return ViewModelProviders.of(activity).get(EventLogViewModel::class.java)
            }
        }

        fun update(context: Context) {
            val eventsFromDisk = EventLog.from(context).readAll()
            (events as MutableLiveData).postValue(eventsFromDisk.asReversed())
        }
    }
}
