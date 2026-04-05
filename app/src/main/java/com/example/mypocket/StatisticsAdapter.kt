package com.example.mypocket.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.databinding.ItemStatisticCategoryBinding

class StatisticsAdapter : RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder>() {

    private val items = mutableListOf<StatisticCategoryItem>()

    fun submitList(newItems: List<StatisticCategoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val binding = ItemStatisticCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatisticsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class StatisticsViewHolder(
        private val binding: ItemStatisticCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StatisticCategoryItem) {
            binding.tvPercentage.text = "${item.percentage}%"
            binding.tvCategoryName.text = item.categoryName
            binding.tvAmount.text = "RM %.2f".format(item.amount)
        }
    }
}