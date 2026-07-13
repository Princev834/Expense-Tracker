package com.princevekariya.projectledger.core.database.mapper

import com.princevekariya.projectledger.core.database.entity.BudgetEntity
import com.princevekariya.projectledger.core.model.Budget
import com.princevekariya.projectledger.core.model.BudgetPeriod
import com.princevekariya.projectledger.core.model.CurrencyCode
import com.princevekariya.projectledger.core.model.Money

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    limitMinorUnits = limit.minorUnits,
    currencyCode = limit.currency.name,
    period = period.name,
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    categoryId = categoryId,
    accountId = accountId,
    isEnabled = isEnabled,
)

fun BudgetEntity.toModel(): Budget = Budget(
    id = id,
    name = name,
    limit = Money(
        minorUnits = limitMinorUnits,
        currency = CurrencyCode.valueOf(currencyCode),
    ),
    period = BudgetPeriod.valueOf(period),
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    categoryId = categoryId,
    accountId = accountId,
    isEnabled = isEnabled,
)
