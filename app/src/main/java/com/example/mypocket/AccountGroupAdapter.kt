package com.example.mypocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.entity.AccountGroupEntity
import com.example.mypocket.databinding.ItemSettingsRowBinding

/**
 * Adapter for displaying account groups in AccountGroupSettingsFragment
 * - Each row shows the group name
 * - Clicking a row can trigger edit/delete actions
 */
class AccountGroupAdapter(
    private val onEditClick: (AccountGroupEntity) -> Unit,
    private val onDeleteClick: ((AccountGroupEntity) -> Unit)? = null
) : ListAdapter<AccountGroupEntity, AccountGroupAdapter.GroupViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemSettingsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = getItem(position)
        holder.bind(group)
    }

    inner class GroupViewHolder(private val binding: ItemSettingsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: AccountGroupEntity) {
            binding.tvName.text = group.name

            // Edit button click
            binding.btnEdit.setOnClickListener { onEditClick(group) }

            // Delete button: hide if isDefault
            binding.btnDelete.visibility = if (group.isDefault) View.INVISIBLE else View.VISIBLE
            binding.btnDelete.setOnClickListener {
                if (!group.isDefault) {
                    onDeleteClick?.invoke(group)
                }
            }

            // Reorder button hidden for account groups
            binding.btnReorder.visibility = View.GONE
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AccountGroupEntity>() {
        override fun areItemsTheSame(oldItem: AccountGroupEntity, newItem: AccountGroupEntity): Boolean {
            return oldItem.accountGroupId == newItem.accountGroupId
        }

        override fun areContentsTheSame(oldItem: AccountGroupEntity, newItem: AccountGroupEntity): Boolean {
            return oldItem == newItem
        }
    }
}