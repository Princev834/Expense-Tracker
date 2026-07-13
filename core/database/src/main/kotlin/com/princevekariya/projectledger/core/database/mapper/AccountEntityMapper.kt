package com.princevekariya.projectledger.core.database.mapper

import com.princevekariya.projectledger.core.database.entity.AccountEntity
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.CurrencyCode
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.Money

fun FinancialAccount.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    openingBalanceMinorUnits = openingBalance.minorUnits,
    currentBalanceMinorUnits = currentBalance.minorUnits,
    currencyCode = openingBalance.currency.name,
    isArchived = isArchived,
)

fun AccountEntity.toModel(): FinancialAccount {
    val currency = CurrencyCode.valueOf(currencyCode)
    return FinancialAccount(
        id = id,
        name = name,
        type = AccountType.valueOf(type),
        openingBalance = Money(
            minorUnits = openingBalanceMinorUnits,
            currency = currency,
        ),
        currentBalance = Money(
            minorUnits = currentBalanceMinorUnits,
            currency = currency,
        ),
        isArchived = isArchived,
    )
}
