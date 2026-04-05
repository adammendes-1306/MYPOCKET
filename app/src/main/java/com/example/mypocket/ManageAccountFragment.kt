package com.example.mypocket.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mypocket.R
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.databinding.FragmentManageAccountBinding
import com.example.mypocket.entity.AccountEntity
import com.example.mypocket.entity.AccountGroupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.mypocket.utils.toCurrency

class ManageAccountFragment : Fragment() {

    private var _binding: FragmentManageAccountBinding? = null
    private val binding get() = _binding!!
    private val db by lazy { AppDatabase.getInstance(requireContext()) }

    private var accountToEdit: AccountEntity? = null
    private var groupList: List<AccountGroupEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val accountId = arguments?.getLong("accountId") ?: 0L
        val deleteItem = binding.toolbar.menu.findItem(R.id.action_delete)
        deleteItem?.isVisible = accountId != 0L

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    accountToEdit?.let { account ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Account")
                            .setMessage("Are you sure you want to delete \"${account.name}\"?")
                            .setPositiveButton("Delete") { _, _ ->
                                lifecycleScope.launch {
                                    db.accountDao().deleteAccount(account)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                                        requireActivity().supportFragmentManager.popBackStack()
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    true
                }
                else -> false
            }
        }

        // Load account if editing
        if (accountId != 0L) {
            lifecycleScope.launch {
                accountToEdit = db.accountDao().getAccountById(accountId)
                accountToEdit?.let { prefillAccountFields(it) }
            }
        }

        // Load groups for dropdown
        lifecycleScope.launch {
            db.accountGroupDao().getAllGroups().collect { groups ->
                groupList = groups
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    groups.map { it.name }
                )
                binding.etGroup.setAdapter(adapter)

                // Prefill group if editing
                accountToEdit?.let { account ->
                    val groupName = groupList.find { it.accountGroupId == account.accountGroupId }?.name
                    binding.etGroup.setText(groupName, false)
                }
            }
        }

        // Show dropdown when clicking the field
        binding.etGroup.setOnClickListener {
            binding.etGroup.showDropDown()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val groupName = binding.etGroup.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val amountText = binding.etAmount.text.toString().trim()
            val description = binding.etDescription.text.toString()

            // Validation: Group, Name cannot be empty/null
            if (groupName.isEmpty()) {
                binding.etGroup.error = "Group cannot be empty"
                return@setOnClickListener
            }

            val selectedGroup = groupList.find { it.name == groupName }
            if (selectedGroup == null) {
                binding.etGroup.error = "Please select a valid group"
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                binding.etName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            binding.etGroup.error = null
            binding.etName.error = null

            // --- Adjust amount parsing ---
            val amount = amountText
                .replace(",", "")
                .trim()
                .toDoubleOrNull() ?: 0.0  // <-- Default to 0.0 if empty or invalid

            // Optional: format the amount field to show 0.00 if user left it empty
            binding.etAmount.setText("%.2f".format(amount))

            lifecycleScope.launch {
                var message = ""
                if (accountToEdit != null) {
                    // Update existing account
                    val updated = accountToEdit!!.copy(
                        accountGroupId = selectedGroup.accountGroupId,
                        name = name,
                        amount = amount,
                        description = description
                    )
                    db.accountDao().updateAccount(updated)
                    message = "Account updated successfully"
                } else {
                    // Insert new account
                    val newAccount = AccountEntity(
                        accountGroupId = selectedGroup.accountGroupId,
                        name = name,
                        amount = amount,
                        description = description
                    )
                    db.accountDao().insertAccount(newAccount)
                    message = "Account added successfully"
                }

                // Show toast and pop back
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun prefillAccountFields(account: AccountEntity) {
        binding.etName.setText(account.name)

        // EditText for amount entry should hold raw numeric text, not display currency formatting
        binding.etAmount.setText("%.2f".format(account.amount ?: 0.0))
        binding.etDescription.setText(account.description)
        val groupName = groupList.find { it.accountGroupId == account.accountGroupId }?.name
        binding.etGroup.setText(groupName, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}