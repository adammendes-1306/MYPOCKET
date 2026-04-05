package com.example.mypocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.model.TransactionType
import com.example.mypocket.model.WalletRow
import kotlin.math.abs

class WalletAdapter(
    private val onDetailClick: (WalletRow.Detail) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<WalletRow>()

    fun submit(list: List<WalletRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is WalletRow.SectionHeader -> TYPE_HEADER
            is WalletRow.Detail -> TYPE_DETAIL
            else -> error("WalletAdapter only supports SectionHeader + Detail (daily mode). Found: ${items[position]}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val v = inflater.inflate(R.layout.item_wallet_section_header, parent, false)
                HeaderVH(v)
            }
            TYPE_DETAIL -> {
                val v = inflater.inflate(R.layout.item_wallet_detail, parent, false)
                DetailVH(v)
            }
            else -> error("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is WalletRow.SectionHeader -> (holder as HeaderVH).bind(row)
            is WalletRow.Detail -> (holder as DetailVH).bind(row, onDetailClick)
            else -> Unit // won't happen because getItemViewType() blocks it
        }
    }

    override fun getItemCount(): Int = items.size

    // Display wallet header
    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvIncome: TextView = itemView.findViewById(R.id.tvHeaderIncome)
        private val tvExpense: TextView = itemView.findViewById(R.id.tvHeaderExpense)

        fun bind(row: WalletRow.SectionHeader) {
            tvDate.text = row.label
            tvIncome.text = "RM %.2f".format(row.totalIncome)
            tvExpense.text = "RM %.2f".format(row.totalExpense)
        }
    }

    // Display wallet information
    class DetailVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvAccount: TextView = itemView.findViewById(R.id.tvAccount)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(row: WalletRow.Detail, onClick: (WalletRow.Detail) -> Unit) {
            tvCategory.text = row.category
            tvTitle.text = row.title
            tvTime.text = row.time
            tvAccount.text = row.account

            tvAmount.text = "RM %.2f".format(abs(row.amount))

            val colorRes = when (row.type) {
                TransactionType.INCOME -> R.color.income
                TransactionType.EXPENSE -> R.color.expense
                TransactionType.TRANSFER -> R.color.transfer
            }
            tvAmount.setTextColor(ContextCompat.getColor(itemView.context, colorRes))

            itemView.setOnClickListener { onClick(row) }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_DETAIL = 1
    }
}