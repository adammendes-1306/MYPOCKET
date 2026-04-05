package com.example.mypocket.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mypocket.R
import com.example.mypocket.adapter.AccountGroupAdapter
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.databinding.FragmentAccountGroupSettingsBinding
import com.example.mypocket.entity.AccountGroupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AccountGroupSettingsFragment : Fragment() {

    private var _binding: FragmentAccountGroupSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AccountGroupAdapter
    private val db by lazy { AppDatabase.getInstance(requireContext()) }

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        _binding = FragmentAccountGroupSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Inflate menu only once from XML
                menuInflater.inflate(R.menu.menu_add, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        // Navigate to add new account group
                        findNavController().navigate(
                            R.id.action_accountGroupSettingsFragment_to_manageAccountGroupFragment
                        )
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        // RecyclerView & adapter
        adapter = AccountGroupAdapter(
            onEditClick = { group ->
                // Edit: pass groupId to ManageAccountGroupFragment
                val bundle = Bundle().apply { putLong("groupId", group.accountGroupId) }
                findNavController().navigate(
                    R.id.action_accountGroupSettingsFragment_to_manageAccountGroupFragment,
                    bundle
                )
            },
            onDeleteClick = { group ->
                if (!group.isDefault) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Account Group")
                        .setMessage("Are you sure you want to delete '${group.name}'?")
                        .setPositiveButton("Delete") { dialog, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                db.accountGroupDao().deleteGroup(group)
                                val updatedList = db.accountGroupDao().getAllGroups().first()
                                lifecycleScope.launch(Dispatchers.Main) {
                                    adapter.submitList(updatedList)
                                }
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        )

        binding.rvAccountGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAccountGroups.adapter = adapter

        // Load account groups
        lifecycleScope.launch(Dispatchers.IO) {
            val list = db.accountGroupDao().getAllGroups().first()
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}