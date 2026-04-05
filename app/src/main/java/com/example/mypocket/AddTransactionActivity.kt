package com.example.mypocket.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mypocket.R
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.data.DefaultData
import com.example.mypocket.databinding.ActivityAddTransactionBinding
import com.example.mypocket.entity.TransactionEntity
import com.example.mypocket.model.TransactionType
import com.example.mypocket.utils.DialogPickers.showAccountDialogPicker
import com.example.mypocket.utils.DialogPickers.showCategoryDialogPicker
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "TYPE"

        fun newIntent(context: Context, type: TransactionType): Intent {
            return Intent(context, AddTransactionActivity::class.java).apply {
                putExtra(EXTRA_TYPE, type.name)
            }
        }
    }

    private lateinit var binding: ActivityAddTransactionBinding

    private val cal: Calendar = Calendar.getInstance()
    private var currentType: TransactionType = TransactionType.EXPENSE

    private var selectedCategory: String = ""
    private var selectedAccount: String = ""
    private var selectedFromAccount: String = ""
    private var selectedToAccount: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if the Android version is 11 (API level 30) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            insetsController?.show(WindowInsets.Type.statusBars())
        }

        currentType = intent.getStringExtra(EXTRA_TYPE)
            ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            ?: TransactionType.EXPENSE

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Insert default accounts if none exist
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@AddTransactionActivity)
            if (db.accountDao().getAllAccountsList().isEmpty()) {
                DefaultData.accounts.forEach { db.accountDao().insertAccount(it) }
            }
        }

        setupUI()
        applyTypeUI(currentType)
        setupTransferPickers()
    }

    private fun setupUI() {
        binding.toggleType.check(
            when (currentType) {
                TransactionType.INCOME -> binding.btnIncome.id
                TransactionType.EXPENSE -> binding.btnExpense.id
                TransactionType.TRANSFER -> binding.btnTransfer.id
            }
        )

        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentType = when (checkedId) {
                binding.btnIncome.id -> TransactionType.INCOME
                binding.btnExpense.id -> TransactionType.EXPENSE
                else -> TransactionType.TRANSFER
            }
            applyTypeUI(currentType)
        }

        binding.btnSelectCategory.setOnClickListener {
            showCategoryDialogPicker(this, currentType) { selected ->
                selectedCategory = selected
                binding.btnSelectCategory.text = selected
                updateButtonTextColor(binding.btnSelectCategory)
            }
        }

        binding.btnSelectAccount.setOnClickListener {
            showAccountDialogPicker(this) { selected ->
                selectedAccount = selected
                binding.btnSelectAccount.text = selected
                updateButtonTextColor(binding.btnSelectAccount)
            }
        }

        binding.switchFee.setOnCheckedChangeListener { _, isChecked ->
            binding.tilFee.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) binding.etFee.setText("")
        }

        updateDateTimeText()
        binding.btnDateTime.setOnClickListener {
            showDateThenTimePicker { updateDateTimeText() }
        }

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { onSaveClicked() }
    }

    private fun applyTypeUI(type: TransactionType) {
        binding.toolbar.title = when (type) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Expense"
            TransactionType.TRANSFER -> "Transfer"
        }

        binding.btnSave.backgroundTintList = ContextCompat.getColorStateList(
            this,
            when (type) {
                TransactionType.INCOME -> R.color.income
                TransactionType.EXPENSE -> R.color.expense
                TransactionType.TRANSFER -> R.color.transfer
            }
        )

        // Reset selections
        selectedCategory = ""
        binding.btnSelectCategory.text = "Category"
        updateButtonTextColor(binding.btnSelectCategory)

        selectedAccount = ""
        binding.btnSelectAccount.text = "Account"
        updateButtonTextColor(binding.btnSelectAccount)

        binding.actvFrom.setText("")
        binding.actvTo.setText("")
        selectedFromAccount = ""
        selectedToAccount = ""

        when (type) {
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                binding.blockIncomeExpense.visibility = View.VISIBLE
                binding.blockTransfer.visibility = View.GONE
                binding.feeRow.visibility = View.GONE
            }
            TransactionType.TRANSFER -> {
                binding.blockIncomeExpense.visibility = View.GONE
                binding.blockTransfer.visibility = View.VISIBLE
                binding.feeRow.visibility = View.VISIBLE
            }
        }
    }

    private fun setupTransferPickers() {
        binding.actvFrom.apply {
            keyListener = null
            isLongClickable = false
            setTextIsSelectable(false)
            isFocusable = false
            isFocusableInTouchMode = false
        }

        binding.actvTo.apply {
            keyListener = null
            isLongClickable = false
            setTextIsSelectable(false)
            isFocusable = false
            isFocusableInTouchMode = false
        }

        binding.actvFrom.setOnClickListener {
            showAccountDialogPicker(this@AddTransactionActivity) { selected ->
                selectedFromAccount = selected
                binding.actvFrom.setText(selected)
            }
        }

        binding.actvTo.setOnClickListener {
            showAccountDialogPicker(this@AddTransactionActivity) { selected ->
                selectedToAccount = selected
                binding.actvTo.setText(selected)
            }
        }
    }

    private fun onSaveClicked() {
        val amount = parseMoney(binding.etAmount.text?.toString())
        if (amount == null || amount <= 0.0) {
            binding.tilAmount.error = "Enter valid amount"
            return
        }

        binding.tilAmount.error = null
        binding.btnSave.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@AddTransactionActivity)
            var categoryId: Long? = null
            var accountId: Long? = null
            var fromAccountId: Long? = null
            var toAccountId: Long? = null

            if (currentType == TransactionType.TRANSFER) {
                if (selectedFromAccount.isBlank() || selectedToAccount.isBlank()) {
                    withContext(Dispatchers.Main) {
                        toast("Select both accounts")
                        binding.btnSave.isEnabled = true
                    }
                    return@launch
                }

                fromAccountId = db.accountDao().getIdByName(selectedFromAccount)
                toAccountId = db.accountDao().getIdByName(selectedToAccount)

                if (fromAccountId == null || toAccountId == null) {
                    withContext(Dispatchers.Main) {
                        toast("Invalid account(s)")
                        binding.btnSave.isEnabled = true
                    }
                    return@launch
                }
            } else {
                if (selectedCategory.isBlank() || selectedAccount.isBlank()) {
                    withContext(Dispatchers.Main) {
                        toast("Select category and account")
                        binding.btnSave.isEnabled = true
                    }
                    return@launch
                }

                categoryId = db.categoryDao().getIdByName(selectedCategory)
                accountId = db.accountDao().getIdByName(selectedAccount)

                if (categoryId == null || accountId == null) {
                    withContext(Dispatchers.Main) {
                        toast("Invalid category or account")
                        binding.btnSave.isEnabled = true
                    }
                    return@launch
                }
            }

            val tx = TransactionEntity(
                type = currentType,
                timestamp = cal.timeInMillis,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                note = binding.etNote.text?.toString(),
                description = binding.etDescription.text?.toString(),
                imageUris = null // photos removed
            )

            db.transactionDao().insertWithBalanceUpdate(tx, db.accountDao())

            withContext(Dispatchers.Main) {
                toast("Saved successfully")
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun showDateThenTimePicker(onDone: () -> Unit) {
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d)
                TimePickerDialog(
                    this,
                    { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, min)
                        onDone()
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeText() {
        val sdf = SimpleDateFormat("dd/MM/yyyy • EEEE • HH:mm", Locale.getDefault())
        binding.btnDateTime.text = sdf.format(cal.time)
    }

    private fun updateButtonTextColor(button: MaterialButton) {
        button.setTextColor(
            if (button.text.isNullOrBlank() || button.text == "Category" || button.text == "Account")
                ContextCompat.getColor(this, R.color.gray)
            else
                ContextCompat.getColor(this, R.color.black)
        )
    }

    private fun parseMoney(raw: String?): Double? =
        raw?.replace(",", "")?.toDoubleOrNull()

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}