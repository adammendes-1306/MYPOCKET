package com.example.mypocket.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mypocket.R
import com.example.mypocket.databinding.FragmentSettingsBinding

/**
 * SettingsFragment shows three main options:
 * - Income Category Settings
 * - Expense Category Settings
 * - Account Settings
 *
 * Clicking any row navigates to the corresponding settings fragment.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set tvTitle for each row
        binding.rowIncomeCategory.tvTitle.text = "Income Category Setting"
        binding.rowExpenseCategory.tvTitle.text = "Expense Category Setting"
        binding.rowAccount.tvTitle.text = "Account Setting"

        binding.rowAccountGroup.tvTitle.text = "Account Groups"
        binding.rowAccount.tvTitle.text = "Account Settings"

        binding.rowBackupRestore.tvTitle.text = "Backup and Restore"
        binding.rowUserManual.tvTitle.text = "User Manual"
        binding.rowAbout.tvTitle.text = "About"

        // Navigate to Income Category Settings
        binding.rowIncomeCategory.root.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_categorySettingsFragment,
                Bundle().apply { putString("CATEGORY_TYPE", "INCOME") }
            )
        }

        // Navigate to Expense Category Settings
        binding.rowExpenseCategory.root.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_categorySettingsFragment,
                Bundle().apply { putString("CATEGORY_TYPE", "EXPENSE") }
            )
        }

        // Navigate to Account Settings
        binding.root.findViewById<LinearLayout>(R.id.rowAccount).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_accountSettingsFragment)
        }

        binding.root.findViewById<LinearLayout>(R.id.rowAccountGroup).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_accountGroupSettingsFragment)
        }

        // Navigate to Backup and Restore Setting
        binding.rowBackupRestore.root.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_backupRestoreFragment,
                Bundle().apply { putString("CATEGORY_TYPE", "EXPENSE") }
            )
        }

        // Navigate to User Manual
        binding.rowUserManual.root.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1yJ8fK4NyjojSN_TDCzvjJXcBRGyygMco/view?usp=drive_link"))
            startActivity(intent)
        }

        // Navigate to AboutFragment
        binding.rowAbout.root.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}