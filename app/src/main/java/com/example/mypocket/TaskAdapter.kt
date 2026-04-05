package com.example.mypocket.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.entity.TaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TaskAdapter(
    private val onToggleComplete: (Long, Boolean) -> Unit,
    private val onClick: ((Long) -> Unit)? = null
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    private val items = mutableListOf<TaskEntity>()
    fun submitList(list: List<TaskEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: (Int)) {
        val item = items[position]

        holder.tvTitle.text = item.title

        // Note visibility
        if (item.note.isNullOrBlank()) {
            holder.tvNote.visibility = View.GONE
        } else {
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = item.note
        }

        // Prevent listener firing during binding
        holder.cbDone.setOnCheckedChangeListener(null)

        holder.cbDone.isChecked = item.isCompleted
        applyCompletedStyle(holder.tvTitle, item.isCompleted)

        // Lighter title and note
        val alpha = if (item.isCompleted) 0.5f else 1f
        holder.tvTitle.alpha = alpha
        holder.tvNote.alpha = alpha

        // Checkbox click
        holder.cbDone.setOnCheckedChangeListener { _, isChecked ->
            onToggleComplete(item.taskId, isChecked)
        }

        // Tap whole row
        holder.itemView.setOnClickListener {
            onToggleComplete(item.taskId, !item.isCompleted)
        }

        val taskDate = LocalDate.ofEpochDay(item.dateEpochDay)
        holder.tvDate.text = taskDate.format(
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        )

        val overdue = item.dateEpochDay < LocalDate.now().toEpochDay()
        holder.tvDate.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (overdue) android.R.color.holo_red_dark else android.R.color.darker_gray
            )
        )

        holder.itemView.setOnClickListener {
            onClick?.invoke(item.taskId)
        }

    }

    private fun applyCompletedStyle(tv: TextView, completed: Boolean) {
        tv.paintFlags = if (completed) {
            tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount(): Int = items.size
}
