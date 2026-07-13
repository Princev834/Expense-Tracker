package com.princevekariya.projectledger.core.database.mapper

import com.princevekariya.projectledger.core.database.entity.CategoryEntity
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.TransactionCategory

fun TransactionCategory.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type.name,
    iconKey = iconKey,
    isDefault = isDefault,
    isArchived = isArchived,
)

fun CategoryEntity.toModel(): TransactionCategory = TransactionCategory(
    id = id,
    name = name,
    type = CategoryType.valueOf(type),
    iconKey = iconKey,
    isDefault = isDefault,
    isArchived = isArchived,
)
