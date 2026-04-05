package com.example.mypocket.utils

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mypocket.R
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.model.TransactionType
import com.example.mypocket.HomeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DialogPickers {

    // Fragment version
    fun showAccountDialogPicker(
        context: Fragment,
        onSelected: (String) -> Unit
    ) {
        val view = context.layoutInflater.inflate(R.layout.dialog_account_picker, null)
        val dialog = AlertDialog.Builder(context.requireContext())
            .setView(view)
            .create()

        val grid = view.findViewById<GridLayout>(R.id.gridAccounts)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEdit)
        grid.columnCount = 3
        grid.removeAllViews()

        CoroutineScope(Dispatchers.IO).launch {
            val accounts = AppDatabase.getInstance(context.requireContext())
                .accountDao().getAllAccountsList()

            withContext(Dispatchers.Main) {
                accounts.forEach { account ->
                    val tv = TextView(context.requireContext()).apply {
                        text = account.name
                        gravity = Gravity.CENTER
                        includeFontPadding = false
                        setTextColor(ContextCompat.getColor(context.requireContext(), R.color.black))
                        setBackgroundResource(R.drawable.category_cell_border)
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = dpToPx(context, 56)
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                            setGravity(Gravity.FILL)
                            setMargins(
                                dpToPx(context, 4),
                                dpToPx(context, 4),
                                dpToPx(context, 4),
                                dpToPx(context, 4)
                            )
                        }
                        setOnClickListener {
                            onSelected(account.name)
                            dialog.dismiss()
                        }
                    }
                    grid.addView(tv)
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnEdit.setOnClickListener {
            dialog.dismiss()
            val navController = (context.requireActivity()
                .supportFragmentManager
                .findFragmentById(R.id.fragmentContainer) as androidx.navigation.fragment.NavHostFragment)
                .navController

            navController.navigate(R.id.action_global_accountSettingsFragment)
        }

        dialog.show()
    }

    fun showCategoryDialogPicker(
        context: Fragment,
        type: TransactionType,
        onSelected: (String) -> Unit
    ) {
        val view = context.layoutInflater.inflate(R.layout.dialog_category_picker, null)
        val dialog = AlertDialog.Builder(context.requireContext())
            .setView(view)
            .create()

        val grid = view.findViewById<GridLayout>(R.id.gridCategories)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEdit)
        grid.columnCount = 3
        grid.removeAllViews()

        CoroutineScope(Dispatchers.IO).launch {
            val categories = AppDatabase.getInstance(context.requireContext())
                .categoryDao()
                .getCategoriesByTypeOrdered(
                    if (type == TransactionType.INCOME) "INCOME" else "EXPENSE"
                )

            withContext(Dispatchers.Main) {
                categories.forEach { category ->
                    val tv = TextView(context.requireContext()).apply {
                        text = category.name
                        gravity = Gravity.CENTER
                        includeFontPadding = false
                        setTextColor(ContextCompat.getColor(context.requireContext(), R.color.black))
                        setBackgroundResource(R.drawable.category_cell_border)
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = dpToPx(context, 56)
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                            setGravity(Gravity.FILL)
                            setMargins(
                                dpToPx(context, 4),
                                dpToPx(context, 4),
                                dpToPx(context, 4),
                                dpToPx(context, 4)
                            )
                        }
                        setOnClickListener {
                            onSelected(category.name)
                            dialog.dismiss()
                        }
                    }
                    grid.addView(tv)
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnEdit.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply {
                putString(
                    "CATEGORY_TYPE",
                    if (type == TransactionType.INCOME) "INCOME" else "EXPENSE"
                )
            }
            val navController = (context.requireActivity()
                .supportFragmentManager
                .findFragmentById(R.id.fragmentContainer) as androidx.navigation.fragment.NavHostFragment)
                .navController

            navController.navigate(
                R.id.action_global_categorySettingsFragment,
                bundle
            )
        }

        dialog.show()
    }

    // Activity version
    fun showAccountDialogPicker(
        activity: AppCompatActivity,
        onSelected: (String) -> Unit
    ) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_account_picker, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .create()

        val grid = view.findViewById<GridLayout>(R.id.gridAccounts)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEdit)
        grid.columnCount = 3
        grid.removeAllViews()

        CoroutineScope(Dispatchers.IO).launch {
            val accounts = AppDatabase.getInstance(activity)
                .accountDao().getAllAccountsList()

            withContext(Dispatchers.Main) {
                accounts.forEach { account ->
                    val tv = TextView(activity).apply {
                        text = account.name
                        gravity = Gravity.CENTER
                        includeFontPadding = false
                        setTextColor(ContextCompat.getColor(activity, R.color.black))
                        setBackgroundResource(R.drawable.category_cell_border)
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = dpToPx(activity, 56)
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                            setGravity(Gravity.FILL)
                            setMargins(
                                dpToPx(activity, 4),
                                dpToPx(activity, 4),
                                dpToPx(activity, 4),
                                dpToPx(activity, 4)
                            )
                        }
                        setOnClickListener {
                            onSelected(account.name)
                            dialog.dismiss()
                        }
                    }
                    grid.addView(tv)
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnEdit.setOnClickListener {
            dialog.dismiss()
            activity.startActivity(
                Intent(activity, HomeActivity::class.java).apply {
                    putExtra("openDestination", "accountSettingsFragment")
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }

        dialog.show()
    }

    // Activity version
    fun showCategoryDialogPicker(
        activity: AppCompatActivity,
        type: TransactionType,
        onSelected: (String) -> Unit
    ) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_category_picker, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .create()

        val grid = view.findViewById<GridLayout>(R.id.gridCategories)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEdit)
        grid.columnCount = 3
        grid.removeAllViews()

        CoroutineScope(Dispatchers.IO).launch {
            val categories = AppDatabase.getInstance(activity)
                .categoryDao()
                .getCategoriesByTypeOrdered(
                    if (type == TransactionType.INCOME) "INCOME" else "EXPENSE"
                )

            withContext(Dispatchers.Main) {
                categories.forEach { category ->
                    val tv = TextView(activity).apply {
                        text = category.name
                        gravity = Gravity.CENTER
                        includeFontPadding = false
                        setTextColor(ContextCompat.getColor(activity, R.color.black))
                        setBackgroundResource(R.drawable.category_cell_border)
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = dpToPx(activity, 56)
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                            setGravity(Gravity.FILL)
                            setMargins(
                                dpToPx(activity, 4),
                                dpToPx(activity, 4),
                                dpToPx(activity, 4),
                                dpToPx(activity, 4)
                            )
                        }
                        setOnClickListener {
                            onSelected(category.name)
                            dialog.dismiss()
                        }
                    }
                    grid.addView(tv)
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnEdit.setOnClickListener {
            dialog.dismiss()
            activity.startActivity(
                Intent(activity, HomeActivity::class.java).apply {
                    putExtra("openDestination", "categorySettingsFragment")
                    putExtra(
                        "CATEGORY_TYPE",
                        if (type == TransactionType.INCOME) "INCOME" else "EXPENSE"
                    )
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }

        dialog.show()
    }

    private fun dpToPx(fragment: Fragment, dp: Int): Int {
        return (dp * fragment.resources.displayMetrics.density).toInt()
    }

    private fun dpToPx(activity: AppCompatActivity, dp: Int): Int {
        return (dp * activity.resources.displayMetrics.density).toInt()
    }
}