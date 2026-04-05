package com.example.mypocket.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mypocket.data.AccountBalancesRepository
import com.example.mypocket.model.AccountBalanceModels
import com.example.mypocket.model.AccountBalancesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountBalancesViewModel(
    private val repository: AccountBalancesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountBalancesUiState())
    val uiState: StateFlow<AccountBalancesUiState> = _uiState.asStateFlow()

    init {
        loadBalances()
    }

    private fun loadBalances() {
        viewModelScope.launch {
            val summary = repository.getBalanceSummary()
            val groupBalances = repository.getGroupedAccountBalances()

            val listItems = mutableListOf<AccountBalanceModels>()

            groupBalances.forEach { group ->
                listItems.add(
                    AccountBalanceModels.GroupHeader(
                        groupId = group.groupId,
                        groupName = group.groupName,
                        groupTotal = group.groupTotal
                    )
                )

                group.accounts.forEach { account ->
                    listItems.add(
                        AccountBalanceModels.AccountChild(
                            accountId = account.accountId,
                            accountName = account.accountName,
                            accountBalance = account.accountBalance
                        )
                    )
                }
            }

            _uiState.value = AccountBalancesUiState(
                totalAssets = summary.totalAssets,
                totalLiabilities = summary.totalLiabilities,
                totalNetWorth = summary.totalNetWorth,
                items = listItems
            )
        }
    }
}