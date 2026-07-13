package com.princevekariya.projectledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchants",
    indices = [
        Index(
            value = ["search_key"],
            unique = true,
        ),
    ],
)
data class MerchantEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "search_key")
    val searchKey: String,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
)
