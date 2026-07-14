package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.ProjectLedgerDatabase
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.BudgetRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository

data class LedgerRepositories(
    val accounts: AccountRepository,
    val budgets: BudgetRepository,
    val categories: CategoryRepository,
    val merchants: MerchantRepository,
    val transactions: TransactionRepository,
)

fun ProjectLedgerDatabase.createRepositories(): LedgerRepositories = LedgerRepositories(
    accounts = RoomAccountRepository(accountDao = accountDao()),
    budgets = RoomBudgetRepository(budgetDao = budgetDao()),
    categories = RoomCategoryRepository(categoryDao = categoryDao()),
    merchants = RoomMerchantRepository(merchantDao = merchantDao()),
    transactions = RoomTransactionRepository(transactionDao = transactionDao()),
)
