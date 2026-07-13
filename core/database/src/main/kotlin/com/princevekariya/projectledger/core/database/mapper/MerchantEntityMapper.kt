package com.princevekariya.projectledger.core.database.mapper

import com.princevekariya.projectledger.core.database.entity.MerchantEntity
import com.princevekariya.projectledger.core.model.Merchant

fun Merchant.toEntity(): MerchantEntity = MerchantEntity(
    id = id,
    name = name,
    searchKey = searchKey,
    isArchived = isArchived,
)

fun MerchantEntity.toModel(): Merchant = Merchant(
    id = id,
    name = name,
    isArchived = isArchived,
)
