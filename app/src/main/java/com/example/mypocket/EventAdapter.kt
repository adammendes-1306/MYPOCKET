package com.example.mypocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.entity.EventEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// An EventAdapter is the component that connects your list of event data
// to the RecyclerView so each event is displayed properly on screen
class EventAdapter(
    private val onClick: (EventEntity) -> Unit
) : RecyclerView.Adapter<EventAdapter.VH>() {

    private val items = mutableListOf<EventEntity>()
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun submit(list: List<EventEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvEventDesc)
        val tvTime: TextView = itemView.findViewById(R.id.tvEventTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // Set title text
        holder.tvTitle.text = item.title

        // Set description text
        if (item.description.isBlank()) {
            holder.tvDesc.visibility = View.GONE
        } else {
            holder.tvDesc.visibility = View.VISIBLE
            holder.tvDesc.text = item.description
            holder.tvDesc.alpha = 1f
        }

        // Set time
        holder.tvTime.text = if (item.allDay) {
            "All day"
        } else {
            val start = Instant.ofEpochMilli(item.startAt).atZone(ZoneId.systemDefault()).toLocalTime()
            val end = Instant.ofEpochMilli(item.endAt).atZone(ZoneId.systemDefault()).toLocalTime()
            "${start.format(timeFmt)} - ${end.format(timeFmt)}"
        }

        // Make each item clickable
        holder.itemView.setOnClickListener { onClick(item) }
    }
    override fun getItemCount(): Int = items.size
}
