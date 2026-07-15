package com.princevekariya.projectledger.domain.transactions.account

import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import kotlinx.coroutines.flow.first

class CreateFinancialAccountUseCase(
    private val accountRepository: AccountRepository,
    private val idGenerator: AccountIdGenerator,
) {
    suspend operator fun invoke(draft: CreateFinancialAccountDraft): FinancialAccount {
        val normalizedName = draft.normalizedName
        require(normalizedName.isNotBlank()) {
            "Account name cannot be blank."
        }
        require(normalizedName.length <= MAX_ACCOUNT_NAME_LENGTH) {
            "Account name cannot exceed $MAX_ACCOUNT_NAME_LENGTH characters."
        }

        val existingAccounts = accountRepository.observeAll().first()
        require(
            existingAccounts.none { account ->
                account.name.equals(
                    other = normalizedName,
                    ignoreCase = true,
                )
            },
        ) {
            "An account with this name already exists."
        }

        val account = FinancialAccount(
            id = createUniqueAccountId(),
            name = normalizedName,
            type = draft.type,
            openingBalance = draft.openingBalance,
            currentBalance = draft.openingBalance,
        )
        accountRepository.save(account = account)
        return account
    }

    private suspend fun createUniqueAccountId(): String {
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val generatedId = idGenerator.generateId().trim()
            require(generatedId.isNotBlank()) {
                "Generated account identifier cannot be blank."
            }
            if (accountRepository.findById(id = generatedId) == null) {
                return generatedId
            }
        }
        error("Unable to generate a unique account identifier.")
    }

    private companion object {
        const val MAX_ACCOUNT_NAME_LENGTH: Int = 40
        const val MAX_ID_GENERATION_ATTEMPTS: Int = 3
    }
}
