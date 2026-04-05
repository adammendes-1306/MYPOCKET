package com.example.mypocket.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mypocket.data.AccountBalancesRepository

class AccountBalancesViewModelFactory(
    private val repository: AccountBalancesRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountBalancesViewModel::class.java)) {
            return AccountBalancesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}