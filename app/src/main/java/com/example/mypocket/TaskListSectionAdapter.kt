package com.example.mypocket.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.entity.TaskEntity
import com.example.mypocket.data.TaskListRow

class TaskListSectionAdapter(
    private val onToggleComplete: (Long, Boolean) -> Unit,
    private val onTaskClick: (Long) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TaskListRow>()

    fun submit(list: List<TaskListRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is TaskListRow.Header -> 0
            is TaskListRow.TaskItem -> 1
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val v = inflater.inflate(R.layout.item_task_header, parent, false)
            HeaderVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_task, parent, false) // your task row layout
            TaskVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is TaskListRow.Header -> {
                (holder as HeaderVH).bind(row)
                holder.itemView.setOnClickListener(null) // Header not clickable
            }

            is TaskListRow.TaskItem -> {
                (holder as TaskVH).bind(row.task, onToggleComplete)

                holder.itemView.setOnClickListener {
                    onTaskClick(row.task.taskId)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHeader: TextView = itemView.findViewById(R.id.tvHeader)
        fun bind(row: TaskListRow.Header) {
            tvHeader.text = row.text
        }
    }

    class TaskVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)

        fun bind(task: TaskEntity, onToggleComplete: (Long, Boolean) -> Unit) {
            tvTitle.text = task.title
            tvNote.text = task.note

            // Note visibility (same behavior as TaskAdapter)
            if (task.note.isNullOrBlank()) {
                tvNote.visibility = View.GONE
            } else {
                tvNote.visibility = View.VISIBLE
                tvNote.text = task.note
            }

            // Title strike-through only
            tvTitle.paintFlags = if (task.isCompleted) {
                tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Make BOTH title + note lighter when completed
            val alpha = if (task.isCompleted) 0.5f else 1f
            tvTitle.alpha = alpha
            tvNote.alpha = alpha

            // Prevent listener firing during binding
            // To avoid CHAOS
            cbDone.setOnCheckedChangeListener(null)
            cbDone.isChecked = task.isCompleted

            cbDone.setOnCheckedChangeListener { _, checked ->
                onToggleComplete(task.taskId, checked)
            }

            itemView.setOnClickListener {
                onToggleComplete(task.taskId, !task.isCompleted)
            }
        }
    }
}
