package com.example.mypocket.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.mypocket.entity.AccountEntity
import com.example.mypocket.entity.AccountGroupEntity

data class AccountGroupWithAccounts(

    @Embedded
    val group: AccountGroupEntity,

    @Relation(
        parentColumn = "accountGroupId",
        entityColumn = "accountGroupId"
    )
    val accounts: List<AccountEntity>
)