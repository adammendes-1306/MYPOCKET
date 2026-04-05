package com.example.mypocket.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.databinding.FragmentManageAccountGroupBinding
import com.example.mypocket.entity.AccountGroupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fragment to add or edit an Account Group.
 *
 * Usage:
 * - Add new group: no arguments
 * - Edit existing group: pass "groupId" as argument
 */
class ManageAccountGroupFragment : Fragment() {

    private var _binding: FragmentManageAccountGroupBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { AppDatabase.getInstance(requireContext()) }
    private var editingGroupId: Long? = null // null = add, not null = edit

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageAccountGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Check if editing
        editingGroupId = arguments?.getLong("groupId")?.takeIf { it != -1L }

        if (editingGroupId != null) {
            // Load existing group
            lifecycleScope.launch(Dispatchers.IO) {
                val groups = db.accountGroupDao().getAllGroups().first()
                val group = groups.find { it.accountGroupId == editingGroupId }
                group?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.etAccountGroupName.setText(it.name)
                    }
                }
            }
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val name = binding.etAccountGroupName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                if (editingGroupId != null) {
                    // Edit existing: preserve original isDefault
                    val existingGroup = db.accountGroupDao().getAllGroups().first()
                        .find { it.accountGroupId == editingGroupId }

                    existingGroup?.let { group ->
                        val updatedGroup = group.copy(name = name) // keeps isDefault and other fields
                        db.accountGroupDao().updateGroup(updatedGroup)
                    }
                } else {
                    // Add new
                    val newGroup = AccountGroupEntity(
                        name = name,
                        isDefault = false
                    )
                    db.accountGroupDao().insertGroup(newGroup)
                }

                // Go back to AccountGroupSettingsFragment
                lifecycleScope.launch(Dispatchers.Main) {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}