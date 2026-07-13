package com.princevekariya.projectledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["account_id"]),
        Index(value = ["start_epoch_millis", "end_epoch_millis"]),
    ],
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "limit_minor_units")
    val limitMinorUnits: Long,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    val period: String,
    @ColumnInfo(name = "start_epoch_millis")
    val startEpochMillis: Long,
    @ColumnInfo(name = "end_epoch_millis")
    val endEpochMillis: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    @ColumnInfo(name = "account_id")
    val accountId: String?,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,
)
