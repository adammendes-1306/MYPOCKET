package com.example.mypocket.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.entity.CategoryEntity

/**
 * Adapter for displaying categories in RecyclerView.
 * Supports delete, edit, and reorder actions via lambda callback.
 */
class CategoryAdapter(
    private val onAction: (action: String, category: CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, CategoryAdapter.CategoryViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CategoryEntity>() {
            override fun areItemsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity) =
                oldItem.categoryId == newItem.categoryId

            override fun areContentsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity) =
                oldItem == newItem
        }
    }

    // Listener to trigger drag from Fragment using ItemTouchHelper
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val btnReorder: ImageButton = itemView.findViewById(R.id.btnReorder)

        fun bind(category: CategoryEntity) {
            tvName.text = category.name

            // Edit action
            btnEdit.setOnClickListener { onAction("edit", category) }

            // Delete action
            btnDelete.setOnClickListener { onAction("delete", category) }

            // Reorder is now handled by drag instead of click
            // When user presses the reorder button, start drag
            btnReorder.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onStartDrag?.invoke(this)
                }
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings_row, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}