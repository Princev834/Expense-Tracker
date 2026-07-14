package com.princevekariya.projectledger.domain.transactions.bootstrap

import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository

class EnsureDefaultLedgerDataUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(): DefaultLedgerDataResult {
        var createdAccounts = 0
        DefaultLedgerData.accounts.forEach { account ->
            if (accountRepository.findById(id = account.id) == null) {
                accountRepository.save(account = account)
                createdAccounts += 1
            }
        }

        var createdCategories = 0
        DefaultLedgerData.categories.forEach { category ->
            if (categoryRepository.findById(id = category.id) == null) {
                categoryRepository.save(category = category)
                createdCategories += 1
            }
        }

        return DefaultLedgerDataResult(
            createdAccounts = createdAccounts,
            createdCategories = createdCategories,
        )
    }
}
