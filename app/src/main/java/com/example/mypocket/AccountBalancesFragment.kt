package com.example.mypocket.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mypocket.R
import com.example.mypocket.data.AccountBalancesRepository
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.databinding.FragmentAccountBalancesBinding
import com.example.mypocket.utils.toCurrency
import kotlinx.coroutines.launch

class AccountBalancesFragment : Fragment(R.layout.fragment_account_balances) {

    private var _binding: FragmentAccountBalancesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountBalancesViewModel by viewModels {
        AccountBalancesViewModelFactory(
            AccountBalancesRepository(
                AppDatabase.getInstance(requireContext()).accountBalancesDao()
            )
        )
    }

    private lateinit var balancesAdapter: AccountBalancesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAccountBalancesBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        balancesAdapter = AccountBalancesAdapter()

        binding.rvAccountBalances.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = balancesAdapter
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.tvAssetsAmount.text = state.totalAssets.toCurrency()
                binding.tvLiabilitiesAmount.text = state.totalLiabilities.toCurrency()
                binding.tvTotalAmount.text = state.totalNetWorth.toCurrency(withSign = true)

                balancesAdapter.submitList(state.items)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}