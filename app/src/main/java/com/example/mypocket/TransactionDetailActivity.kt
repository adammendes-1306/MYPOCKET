package com.example.mypocket.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mypocket.R
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.databinding.ActivityTransactionDetailBinding
import com.example.mypocket.entity.TransactionEntity
import com.example.mypocket.model.TransactionType
import com.example.mypocket.utils.DialogPickers.showAccountDialogPicker
import com.example.mypocket.utils.DialogPickers.showCategoryDialogPicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TX_ID = "TX_ID"

        fun newIntent(context: Context, txId: Long): Intent {
            return Intent(context, TransactionDetailActivity::class.java).apply {
                putExtra(EXTRA_TX_ID, txId)
            }
        }
    }

    private lateinit var binding: ActivityTransactionDetailBinding

    private var txId: Long = -1L
    private var currentTx: TransactionEntity? = null
    private var isEditMode = false
    private var suppress = false
    private val cal: Calendar = Calendar.getInstance()

    private val transactionDao by lazy {
        AppDatabase.getInstance(this).transactionDao()
    }

    private val accountDao by lazy {
        AppDatabase.getInstance(this).accountDao()
    }

    private val textKeyListeners = mutableMapOf<TextInputEditText, android.text.method.KeyListener?>()
    private val autoKeyListeners = mutableMapOf<AutoCompleteTextView, android.text.method.KeyListener?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure status bar visible for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            insetsController?.show(WindowInsets.Type.statusBars())
        }

        txId = intent.getLongExtra(EXTRA_TX_ID, -1L)
        if (txId == -1L) {
            toast("Missing transaction id")
            finish()
            return
        }

        setupToolbar()
        cacheKeyListeners()
        setupDialogPickers()
        setEditMode(false)
        enableAutoEdit(
            binding.etAmount,
            binding.etNote,
            binding.etDescription,
            binding.etFee,
            binding.actvCategory,
            binding.actvAccount,
            binding.actvFrom,
            binding.actvTo,
            binding.btnDateTime
        )

        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppress) return@addOnButtonCheckedListener

            enterEditMode()

            val newType = when (checkedId) {
                binding.btnIncome.id -> TransactionType.INCOME
                binding.btnExpense.id -> TransactionType.EXPENSE
                else -> TransactionType.TRANSFER
            }

            applyTypeUI(newType)
            updateSaveButtonColor(newType)
        }

        bindClicks()
        loadTransaction()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = "Transaction Detail"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun bindClicks() {
        binding.btnDelete.setOnClickListener { confirmDelete() }

        binding.btnSave.setOnClickListener { saveEdits() }

        binding.btnCancelEditMode.setOnClickListener {
            hideKeyboard()
            binding.tilAmount.error = null
            binding.tilFee.error = null
            setEditMode(false)
        }

        binding.btnDateTime.setOnClickListener {
            enterEditMode()
            showDateThenTimePicker { updateDateTimeText() }
        }

        binding.switchFee.setOnCheckedChangeListener { _, isChecked ->
            if (!isEditMode) return@setOnCheckedChangeListener
            binding.tilFee.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.etFee.setText("")
                binding.tilFee.error = null
            }
        }
    }

    private fun cacheKeyListeners() {
        listOf(
            binding.etAmount,
            binding.etFee,
            binding.etNote,
            binding.etDescription
        ).forEach { textKeyListeners[it] = it.keyListener }

        listOf(
            binding.actvCategory,
            binding.actvAccount,
            binding.actvFrom,
            binding.actvTo
        ).forEach { autoKeyListeners[it] = it.keyListener }
    }

    private fun setupDialogPickers() {
        listOf(
            binding.actvCategory,
            binding.actvAccount,
            binding.actvFrom,
            binding.actvTo
        ).forEach { actv ->
            actv.keyListener = null
            actv.isLongClickable = false
            actv.setTextIsSelectable(false)
            actv.isFocusable = false
            actv.isFocusableInTouchMode = false
        }

        binding.actvCategory.setOnClickListener {
            if (!isEditMode) return@setOnClickListener
            showCategoryDialogPicker(this, toggleTypeToTransactionType()) { selected ->
                binding.actvCategory.setText(selected, false)
            }
        }

        binding.actvAccount.setOnClickListener {
            if (!isEditMode) return@setOnClickListener
            showAccountDialogPicker(this) { selected ->
                binding.actvAccount.setText(selected, false)
            }
        }

        binding.actvFrom.setOnClickListener {
            if (!isEditMode) return@setOnClickListener
            showAccountDialogPicker(this) { selected ->
                binding.actvFrom.setText(selected, false)
            }
        }

        binding.actvTo.setOnClickListener {
            if (!isEditMode) return@setOnClickListener
            showAccountDialogPicker(this) { selected ->
                binding.actvTo.setText(selected, false)
            }
        }
    }

    private fun toggleTypeToTransactionType() = when (binding.toggleType.checkedButtonId) {
        binding.btnIncome.id -> TransactionType.INCOME
        binding.btnExpense.id -> TransactionType.EXPENSE
        else -> TransactionType.TRANSFER
    }

    private fun enterEditMode() {
        if (!isEditMode) {
            setEditMode(true)
        } else {
            binding.rowViewButtons.visibility = View.GONE
            binding.rowEditButtons.visibility = View.VISIBLE
        }
    }

    private fun setEditMode(edit: Boolean) {
        isEditMode = edit

        binding.rowViewButtons.visibility = if (edit) View.GONE else View.VISIBLE
        binding.rowEditButtons.visibility = if (edit) View.VISIBLE else View.GONE

        binding.toggleType.isEnabled = edit
        binding.btnIncome.isEnabled = edit
        binding.btnExpense.isEnabled = edit
        binding.btnTransfer.isEnabled = edit

        listOf(
            binding.etAmount,
            binding.etFee,
            binding.etNote,
            binding.etDescription
        ).forEach { setEditable(it, edit) }

        listOf(
            binding.actvCategory,
            binding.actvAccount,
            binding.actvFrom,
            binding.actvTo
        ).forEach { setEditable(it, edit) }

        binding.btnDateTime.isEnabled = edit
        binding.switchFee.isEnabled = edit
    }

    private fun setEditable(et: TextInputEditText, editable: Boolean) {
        et.isEnabled = true
        et.isFocusable = editable
        et.isFocusableInTouchMode = editable
        et.isCursorVisible = editable
        et.keyListener = if (editable) textKeyListeners[et] else null
    }

    private fun setEditable(actv: AutoCompleteTextView, editable: Boolean) {
        actv.isEnabled = true
        actv.isFocusable = editable
        actv.isFocusableInTouchMode = editable
        actv.isCursorVisible = editable
        actv.keyListener = if (editable) autoKeyListeners[actv] else null
    }

    private fun loadTransaction() {
        lifecycleScope.launch(Dispatchers.IO) {
            val tx = transactionDao.getById(txId)

            withContext(Dispatchers.Main) {
                if (tx == null) {
                    toast("Transaction not found")
                    finish()
                } else {
                    currentTx = tx
                    bindToUi(tx)
                    setEditMode(false)
                }
            }
        }
    }

    private fun bindToUi(tx: TransactionEntity) {
        suppress = true

        cal.timeInMillis = tx.timestamp
        updateDateTimeText()

        binding.toggleType.check(
            when (tx.type) {
                TransactionType.INCOME -> binding.btnIncome.id
                TransactionType.EXPENSE -> binding.btnExpense.id
                TransactionType.TRANSFER -> binding.btnTransfer.id
            }
        )

        applyTypeUI(tx.type)
        updateSaveButtonColor(tx.type)

        binding.etAmount.setText("%.2f".format(tx.amount))
        binding.etNote.setText(tx.note ?: "")
        binding.etDescription.setText(tx.description ?: "")

        val hasFee = (tx.feeAmount ?: 0.0) > 0
        binding.switchFee.isChecked = hasFee
        binding.tilFee.visibility =
            if (tx.type == TransactionType.TRANSFER && hasFee) View.VISIBLE else View.GONE
        binding.etFee.setText(if (hasFee) "%.2f".format(tx.feeAmount) else "")

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@TransactionDetailActivity)

            val categoryName = tx.categoryId?.let { db.categoryDao().getCategoryByIdNullable(it)?.name } ?: ""
            val accountName = tx.accountId?.let { db.accountDao().getAccountById(it)?.name } ?: ""
            val fromName = tx.fromAccountId?.let { db.accountDao().getAccountById(it)?.name } ?: ""
            val toName = tx.toAccountId?.let { db.accountDao().getAccountById(it)?.name } ?: ""

            withContext(Dispatchers.Main) {
                binding.actvCategory.setText(categoryName, false)
                binding.actvAccount.setText(accountName, false)
                binding.actvFrom.setText(fromName, false)
                binding.actvTo.setText(toName, false)
                suppress = false
                setEditMode(isEditMode)
            }
        }
    }

    private fun applyTypeUI(type: TransactionType) {
        binding.toolbar.title = when (type) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Expense"
            TransactionType.TRANSFER -> "Transfer"
        }

        binding.blockIncomeExpense.visibility =
            if (type == TransactionType.INCOME || type == TransactionType.EXPENSE) View.VISIBLE else View.GONE

        binding.blockTransfer.visibility =
            if (type == TransactionType.TRANSFER) View.VISIBLE else View.GONE

        binding.feeRow.visibility =
            if (type == TransactionType.TRANSFER) View.VISIBLE else View.GONE

        if (type != TransactionType.TRANSFER) {
            binding.switchFee.isChecked = false
            binding.tilFee.visibility = View.GONE
            binding.etFee.setText("")
            binding.tilFee.error = null
        }
    }

    private fun saveEdits() {
        val old = currentTx ?: return
        val type = toggleTypeToTransactionType()
        val amount = parseMoney(binding.etAmount.text?.toString())

        if (amount == null || amount <= 0.0) {
            binding.tilAmount.error = "Enter a valid amount"
            return
        } else {
            binding.tilAmount.error = null
        }

        val note = binding.etNote.text?.toString()?.trim()
        val description = binding.etDescription.text?.toString()?.trim()
        val timestamp = cal.timeInMillis

        binding.btnSave.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@TransactionDetailActivity)

            val updated: TransactionEntity = when (type) {
                TransactionType.INCOME, TransactionType.EXPENSE -> {
                    val categoryText = binding.actvCategory.text?.toString()?.trim().orEmpty()
                    val accountText = binding.actvAccount.text?.toString()?.trim().orEmpty()

                    if (categoryText.isBlank() || accountText.isBlank()) {
                        withContext(Dispatchers.Main) {
                            toast("Choose category and account")
                            binding.btnSave.isEnabled = true
                        }
                        return@launch
                    }

                    val categoryId = db.categoryDao().getIdByName(categoryText)
                    val accountId = db.accountDao().getIdByName(accountText)

                    if (categoryId == null || accountId == null) {
                        withContext(Dispatchers.Main) {
                            toast("Invalid category or account")
                            binding.btnSave.isEnabled = true
                        }
                        return@launch
                    }

                    old.copy(
                        type = type,
                        timestamp = timestamp,
                        amount = amount,
                        categoryId = categoryId,
                        accountId = accountId,
                        fromAccountId = null,
                        toAccountId = null,
                        feeAmount = null,
                        note = note,
                        description = description
                    )
                }

                TransactionType.TRANSFER -> {
                    val fromText = binding.actvFrom.text?.toString()?.trim().orEmpty()
                    val toText = binding.actvTo.text?.toString()?.trim().orEmpty()

                    if (fromText.isBlank() || toText.isBlank()) {
                        withContext(Dispatchers.Main) {
                            toast("Choose From and To")
                            binding.btnSave.isEnabled = true
                        }
                        return@launch
                    }

                    val fromId = db.accountDao().getIdByName(fromText)
                    val toId = db.accountDao().getIdByName(toText)

                    if (fromId == null || toId == null) {
                        withContext(Dispatchers.Main) {
                            toast("Invalid account(s)")
                            binding.btnSave.isEnabled = true
                        }
                        return@launch
                    }

                    val fee = if (binding.switchFee.isChecked) {
                        parseMoney(binding.etFee.text?.toString()) ?: 0.0
                    } else {
                        null
                    }

                    old.copy(
                        type = type,
                        timestamp = timestamp,
                        amount = amount,
                        categoryId = null,
                        accountId = null,
                        fromAccountId = fromId,
                        toAccountId = toId,
                        feeAmount = fee,
                        note = note,
                        description = description
                    )
                }
            }

            updateAccountBalances(oldTx = old, newTx = updated)
            transactionDao.update(updated)

            withContext(Dispatchers.Main) {
                binding.btnSave.isEnabled = true
                toast("Transaction updated")
                hideKeyboard()
                currentTx = updated
                bindToUi(updated)
                setEditMode(false)
                setResult(RESULT_OK)
            }
        }
    }

    private suspend fun updateAccountBalances(
        oldTx: TransactionEntity? = null,
        newTx: TransactionEntity? = null
    ) {
        val accountDao = AppDatabase.getInstance(this).accountDao()

        oldTx?.let { tx ->
            when (tx.type) {
                TransactionType.INCOME -> tx.accountId?.let { accountDao.updateBalance(it, -tx.amount) }
                TransactionType.EXPENSE -> tx.accountId?.let { accountDao.updateBalance(it, tx.amount) }
                TransactionType.TRANSFER -> {
                    if (tx.fromAccountId == tx.toAccountId) {
                        tx.fromAccountId?.let { accountDao.updateBalance(it, tx.feeAmount ?: 0.0) }
                    } else {
                        tx.fromAccountId?.let { accountDao.updateBalance(it, tx.amount + (tx.feeAmount ?: 0.0)) }
                        tx.toAccountId?.let { accountDao.updateBalance(it, -tx.amount) }
                    }
                }
            }
        }

        newTx?.let { tx ->
            when (tx.type) {
                TransactionType.INCOME -> tx.accountId?.let { accountDao.updateBalance(it, tx.amount) }
                TransactionType.EXPENSE -> tx.accountId?.let { accountDao.updateBalance(it, -tx.amount) }
                TransactionType.TRANSFER -> {
                    if (tx.fromAccountId == tx.toAccountId) {
                        tx.fromAccountId?.let { accountDao.updateBalance(it, -(tx.feeAmount ?: 0.0)) }
                    } else {
                        tx.fromAccountId?.let { accountDao.updateBalance(it, -tx.amount - (tx.feeAmount ?: 0.0)) }
                        tx.toAccountId?.let { accountDao.updateBalance(it, tx.amount) }
                    }
                }
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete transaction?")
            .setMessage("This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> deleteTx() }
            .show()
    }

    private fun deleteTx() {
        lifecycleScope.launch(Dispatchers.IO) {
            currentTx?.let { updateAccountBalances(oldTx = it) }
            transactionDao.deleteById(txId)

            withContext(Dispatchers.Main) {
                toast("Deleted")
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun showDateThenTimePicker(onDone: () -> Unit) {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)

            TimePickerDialog(this, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, min)
                onDone()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, y, m, d).show()
    }

    private fun updateDateTimeText() {
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfDay = SimpleDateFormat("EEEE", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.btnDateTime.text =
            "${sdfDate.format(cal.time)} • ${sdfDay.format(cal.time)} • ${sdfTime.format(cal.time)}"
    }

    private fun updateSaveButtonColor(type: TransactionType) {
        val color = when (type) {
            TransactionType.INCOME -> getColor(R.color.income)
            TransactionType.EXPENSE -> getColor(R.color.expense)
            TransactionType.TRANSFER -> getColor(R.color.transfer)
        }
        binding.btnSave.setBackgroundColor(color)
    }

    private fun enableAutoEdit(vararg views: View) {
        views.forEach { v ->
            v.setOnTouchListener { _, _ ->
                if (!isEditMode) enterEditMode()
                false
            }
        }
    }

    private fun parseMoney(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return raw.replace(",", "").trim().toDoubleOrNull()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val v = currentFocus ?: binding.root
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
        v.clearFocus()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}