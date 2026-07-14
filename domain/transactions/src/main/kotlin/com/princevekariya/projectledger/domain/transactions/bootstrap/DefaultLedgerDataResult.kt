package com.princevekariya.projectledger.domain.transactions.bootstrap

data class DefaultLedgerDataResult(
    val createdAccounts: Int,
    val createdCategories: Int,
) {
    val createdItems: Int
        get() = createdAccounts + createdCategories
}
