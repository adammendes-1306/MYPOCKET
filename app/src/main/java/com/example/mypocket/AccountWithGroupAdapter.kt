package com.example.mypocket.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.entity.AccountEntity
import com.example.mypocket.entity.AccountGroupEntity
import com.example.mypocket.model.AccountGroupWithAccounts
import com.example.mypocket.databinding.ItemAccountChildBinding
import com.example.mypocket.databinding.ItemAccountGroupHeaderBinding

/**
 * Adapter to display accounts grouped by AccountGroup
 * - Header: Account Group name
 * - Child: Account row
 * - Clicking an account triggers ManageAccountFragment
 */
class AccountWithGroupAdapter(
    private val onAccountClick: (AccountEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data: List<AccountGroupWithAccounts> = emptyList()

    // View types
    private val TYPE_HEADER = 0
    private val TYPE_CHILD = 1

    // Flattened list to include headers and children
    private val flatList = mutableListOf<Any>()

    fun submitList(list: List<AccountGroupWithAccounts>) {
        data = list
        flatList.clear()
        list.forEach { group ->
            flatList.add(group.group)          // header
            flatList.addAll(group.accounts)    // children
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (flatList[position] is AccountGroupEntity) TYPE_HEADER else TYPE_CHILD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemAccountGroupHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            GroupViewHolder(binding)
        } else {
            val binding = ItemAccountChildBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AccountViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GroupViewHolder) {
            holder.bind(flatList[position] as AccountGroupEntity)
        } else if (holder is AccountViewHolder) {
            holder.bind(flatList[position] as AccountEntity)
        }
    }

    override fun getItemCount(): Int = flatList.size

    inner class GroupViewHolder(private val binding: ItemAccountGroupHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: AccountGroupEntity) {
            binding.tvGroupName.text = group.name
        }
    }

    inner class AccountViewHolder(private val binding: ItemAccountChildBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(account: AccountEntity) {
            binding.tvAccountName.text = account.name
            binding.root.setOnClickListener { onAccountClick(account) }
        }
    }
}