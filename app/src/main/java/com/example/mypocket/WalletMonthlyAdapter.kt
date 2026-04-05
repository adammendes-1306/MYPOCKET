package com.example.mypocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.model.WalletRow
import java.util.Locale
import kotlin.math.abs

class WalletMonthlyAdapter(
    private val onToggleMonth: (monthKey: String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<WalletRow>()

    fun submit(rows: List<WalletRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is WalletRow.MonthHeader -> TYPE_MONTH
        is WalletRow.WeekSummary -> TYPE_WEEK
        else -> error("WalletMonthlyAdapter only supports MonthHeader + WeekSummary. Found: ${items[position]}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MONTH -> MonthVH(inflater.inflate(R.layout.row_wallet_month_header, parent, false))
            TYPE_WEEK -> WeekVH(inflater.inflate(R.layout.row_wallet_week, parent, false))
            else -> error("Unknown viewType: $viewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is WalletRow.MonthHeader -> (holder as MonthVH).bind(row, onToggleMonth)
            is WalletRow.WeekSummary -> (holder as WeekVH).bind(row)
            else -> Unit
        }
    }

    class MonthVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        private val tvIncome: TextView = itemView.findViewById(R.id.tvIncome)
        private val tvExpense: TextView = itemView.findViewById(R.id.tvExpense)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val tvChevron: TextView? = itemView.findViewById(R.id.tvChevron) // optional in XML

        fun bind(row: WalletRow.MonthHeader, onToggle: (String) -> Unit) {
            tvLabel.text = row.label
            tvIncome.text = money(row.totalIncome)
            tvExpense.text = money(row.totalExpense)
            tvTotal.text = totalMoney(row.totalIncome - row.totalExpense)

            tvChevron?.text = if (row.expanded) "▾" else "▸"

            itemView.setOnClickListener { onToggle(row.key) }
        }
    }

    class WeekVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        private val tvIncome: TextView = itemView.findViewById(R.id.tvIncome)
        private val tvExpense: TextView = itemView.findViewById(R.id.tvExpense)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)

        fun bind(row: WalletRow.WeekSummary) {
            tvLabel.text = row.label
            tvIncome.text = money(row.totalIncome)
            tvExpense.text = money(row.totalExpense)
            tvTotal.text = totalMoney(row.totalIncome - row.totalExpense)
        }
    }

    companion object {
        private const val TYPE_MONTH = 0
        private const val TYPE_WEEK = 1

        private fun money(v: Double): String {
            val s = String.format(Locale.getDefault(), "%,.2f", abs(v))
            return "RM$s"
        }

        private fun totalMoney(total: Double): String {
            return if (total < 0) "-${money(total)}" else money(total)
        }
    }
}