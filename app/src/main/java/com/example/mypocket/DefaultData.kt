package com.example.mypocket.data

import com.example.mypocket.entity.AccountEntity
import com.example.mypocket.entity.AccountGroupEntity
import com.example.mypocket.entity.CategoryEntity

object DefaultData {

    val incomeCategories = listOf(
        CategoryEntity(name = "💰 Salary", type = "INCOME", sortOrder = 0),
        CategoryEntity(name = "🎁 Gift", type = "INCOME", sortOrder = 1),
        CategoryEntity(name = "🪙 Allowance", type = "INCOME", sortOrder = 2),
        CategoryEntity(name = "Others", type = "INCOME", sortOrder = 3)
    )

    val expenseCategories = listOf(
        CategoryEntity(name = "🍔 Food", type = "EXPENSE", sortOrder = 0),
        CategoryEntity(name = "🚌 Transport", type = "EXPENSE", sortOrder = 1),
        CategoryEntity(name = "🛍️ Shopping", type = "EXPENSE", sortOrder = 2),
        CategoryEntity(name = "💡 Bills", type = "EXPENSE", sortOrder = 3),
        CategoryEntity(name = "Others", type = "EXPENSE", sortOrder = 4)
    )

    val accountGroups = listOf(
        AccountGroupEntity(name = "Cash", isDefault = true),
        AccountGroupEntity(name = "Bank Accounts", isDefault = true),
        AccountGroupEntity(name = "Card", isDefault = true)
    )

    // Default Accounts (one per group; groupId will be set dynamically)
    val accounts = listOf(
        AccountEntity(accountGroupId = 0L, name = "Cash", amount = 0.0),        // Cash group
        AccountEntity(accountGroupId = 0L, name = "Bank Account", amount = 0.0), // Bank Accounts group
        AccountEntity(accountGroupId = 0L, name = "Card", amount = 0.0)          // Card group
    )
}