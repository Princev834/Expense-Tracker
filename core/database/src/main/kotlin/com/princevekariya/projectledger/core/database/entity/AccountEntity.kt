package com.princevekariya.projectledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_accounts",
    indices = [
        Index(
            value = ["name"],
            unique = true,
        ),
    ],
)
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    @ColumnInfo(name = "opening_balance_minor_units")
    val openingBalanceMinorUnits: Long,
    @ColumnInfo(name = "current_balance_minor_units")
    val currentBalanceMinorUnits: Long,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
)
