package com.example.mypocket.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mypocket.R
import com.example.mypocket.entity.AccountEntity
import com.example.mypocket.model.AccountGroupWithAccounts
import com.example.mypocket.adapter.AccountWithGroupAdapter
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.databinding.FragmentAccountSettingsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for managing all accounts:
 * - Shows accounts grouped by AccountGroup
 * - Clicking an account opens ManageAccountFragment
 */
class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AccountWithGroupAdapter
    private val db by lazy { AppDatabase.getInstance(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.apply {
            // Navigation button works
            setNavigationOnClickListener {
                requireActivity().supportFragmentManager.popBackStack()
            }

            // Inflate menu after the toolbar is attached
            inflateMenu(R.menu.menu_add)

            // Handle clicks — must return true for handled items
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_add -> {
                        // Your logic for add button
                        // Example: open ManageAccountFragment without an accountId
                        val bundle = Bundle()
                        findNavController().navigate(R.id.manageAccountFragment, bundle)
                        true // <- must return true
                    }
                    else -> false
                }
            }
        }

        adapter = AccountWithGroupAdapter { account: AccountEntity ->
            val bundle = Bundle().apply {
                putLong("accountId", account.accountId) // pass the clicked account ID
            }
            findNavController().navigate(R.id.manageAccountFragment, bundle)
        }

        binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAccounts.adapter = adapter

        // Load accounts grouped by AccountGroup
        lifecycleScope.launch {
            db.accountGroupDao().getGroupsWithAccounts().collectLatest { list: List<AccountGroupWithAccounts> ->
                adapter.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}