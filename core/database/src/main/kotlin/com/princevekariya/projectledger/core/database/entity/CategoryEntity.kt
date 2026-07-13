package com.princevekariya.projectledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_categories",
    indices = [
        Index(
            value = ["name", "type"],
            unique = true,
        ),
    ],
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    @ColumnInfo(name = "icon_key")
    val iconKey: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
)
