package com.example.mypocket.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.databinding.ItemAccountChildBinding
import com.example.mypocket.databinding.ItemAccountGroupHeaderBinding
import com.example.mypocket.model.AccountBalanceModels
import com.example.mypocket.utils.toCurrency

class AccountBalancesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<AccountBalanceModels>()

    fun submitList(newItems: List<AccountBalanceModels>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AccountBalanceModels.GroupHeader -> VIEW_TYPE_HEADER
            is AccountBalanceModels.AccountChild -> VIEW_TYPE_CHILD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemAccountGroupHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                GroupHeaderViewHolder(binding)
            }

            else -> {
                val binding = ItemAccountChildBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AccountChildViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AccountBalanceModels.GroupHeader -> {
                (holder as GroupHeaderViewHolder).bind(item)
            }

            is AccountBalanceModels.AccountChild -> {
                (holder as AccountChildViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class GroupHeaderViewHolder(
        private val binding: ItemAccountGroupHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AccountBalanceModels.GroupHeader) {
            binding.tvGroupName.text = item.groupName
            binding.tvGroupTotal.text = kotlin.math.abs(item.groupTotal).toCurrency()

            val color = if (item.groupTotal >= 0) {
                itemView.context.getColor(R.color.income)
            } else {
                itemView.context.getColor(R.color.expense)
            }

            binding.tvGroupTotal.setTextColor(color)
        }
    }

    class AccountChildViewHolder(
        private val binding: ItemAccountChildBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AccountBalanceModels.AccountChild) {
            binding.tvAccountName.text = item.accountName
            binding.tvAccountBalance.text = kotlin.math.abs(item.accountBalance).toCurrency()

            val color = if (item.accountBalance >= 0) {
                itemView.context.getColor(R.color.income)
            } else {
                itemView.context.getColor(R.color.expense)
            }

            binding.tvAccountBalance.setTextColor(color)
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CHILD = 1
    }
}