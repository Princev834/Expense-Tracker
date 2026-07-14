package com.princevekariya.projectledger.domain.transactions.command

import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.TransactionSource
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.balance.AccountBalanceProjector
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository

class SaveManualTransactionUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantRepository: MerchantRepository,
    private val transactionRepository: TransactionRepository,
    private val idGenerator: TransactionIdGenerator,
    private val timeProvider: EpochTimeProvider,
    private val balanceProjector: AccountBalanceProjector =
        AccountBalanceProjector(),
) {
    suspend operator fun invoke(draft: ManualTransactionDraft): LedgerTransaction {
        val account = requireAccount(draft = draft)
        val category = requireCategory(draft = draft)
        val merchantId = requireMerchant(draft = draft)

        require(account.currentBalance.currency == draft.amount.currency) {
            "Transaction currency must match the selected account."
        }
        require(category.type == draft.type.requiredCategoryType()) {
            "The selected category does not match the transaction type."
        }

        val now = timeProvider.currentTimeMillis()
        require(now >= 0L) {
            "Current time cannot be negative."
        }

        val transaction = LedgerTransaction(
            id = createUniqueTransactionId(),
            type = draft.type,
            amount = draft.amount,
            accountId = draft.normalizedAccountId,
            categoryId = draft.normalizedCategoryId,
            merchantId = merchantId,
            occurredAtEpochMillis = draft.occurredAtEpochMillis ?: now,
            paymentMethod = draft.paymentMethod,
            source = TransactionSource.MANUAL,
            note = draft.normalizedNote,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        val updatedAccount = balanceProjector.project(
            account = account,
            transactionType = transaction.type,
            amount = transaction.amount,
        )

        transactionRepository.saveWithUpdatedAccount(
            transaction = transaction,
            updatedAccount = updatedAccount,
        )
        return transaction
    }

    private suspend fun requireAccount(draft: ManualTransactionDraft) = requireNotNull(
        accountRepository.findById(id = draft.normalizedAccountId),
    ) {
        "The selected account does not exist."
    }.also { account ->
        require(!account.isArchived) {
            "The selected account is archived."
        }
    }

    private suspend fun requireCategory(draft: ManualTransactionDraft) = requireNotNull(
        categoryRepository.findById(id = draft.normalizedCategoryId),
    ) {
        "The selected category does not exist."
    }.also { category ->
        require(!category.isArchived) {
            "The selected category is archived."
        }
    }

    private suspend fun requireMerchant(draft: ManualTransactionDraft): String? {
        val merchantSearchKey =
            draft.normalizedMerchantSearchKey ?: return null
        val merchant = requireNotNull(
            merchantRepository.findBySearchKey(
                searchKey = merchantSearchKey,
            ),
        ) {
            "The selected merchant does not exist."
        }
        require(!merchant.isArchived) {
            "The selected merchant is archived."
        }
        return merchant.id
    }

    private suspend fun createUniqueTransactionId(): String {
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val generatedId = idGenerator.generateId().trim()
            require(generatedId.isNotBlank()) {
                "Generated transaction identifier cannot be blank."
            }
            if (transactionRepository.findById(id = generatedId) == null) {
                return generatedId
            }
        }
        error("Unable to generate a unique transaction identifier.")
    }

    private fun TransactionType.requiredCategoryType(): CategoryType = when (this) {
        TransactionType.EXPENSE -> CategoryType.EXPENSE
        TransactionType.INCOME -> CategoryType.INCOME
        TransactionType.TRANSFER -> error(
            "A transfer does not use an expense or income category.",
        )
    }

    private companion object {
        const val MAX_ID_GENERATION_ATTEMPTS: Int = 3
    }
}
