package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class TransactionHistoryDataMapper(
    private val timeProvider: EpochTimeProvider,
    private val zoneId: ZoneId,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern(
        "dd MMM yyyy",
        Locale.ENGLISH,
    )

    fun map(
        sourceData: TransactionHistorySourceData,
        selectedFilter: TransactionHistoryFilter,
    ): TransactionHistoryUiState {
        val now = timeProvider.currentTimeMillis()
        val accountsById = sourceData.accounts.associateBy { account ->
            account.id
        }
        val categoriesById = (
            sourceData.expenseCategories + sourceData.incomeCategories
            ).associateBy { category ->
            category.id
        }
        val merchantsById = sourceData.merchants.associateBy { merchant ->
            merchant.id
        }
        val sortedTransactions = sourceData.transactions.sortedWith(
            compareByDescending<LedgerTransaction> { transaction ->
                transaction.occurredAtEpochMillis
            }.thenByDescending { transaction ->
                transaction.createdAtEpochMillis
            },
        )
        val visibleTransactions = sortedTransactions.filter { transaction ->
            selectedFilter.accepts(type = transaction.type)
        }

        return TransactionHistoryUiState(
            selectedFilter = selectedFilter,
            transactions = visibleTransactions.map { transaction ->
                val category = transaction.categoryId?.let(categoriesById::get)
                val account = accountsById[transaction.accountId]
                val merchant = transaction.merchantId?.let(merchantsById::get)

                TransactionHistoryItem(
                    id = transaction.id,
                    title = merchant?.name
                        ?: transaction.note
                        ?: category?.name
                        ?: transaction.type.defaultTitle(),
                    subtitle = listOf(
                        category?.name ?: transaction.type.defaultSubtitle(),
                        account?.name ?: "Unknown account",
                        formatDate(
                            epochMillis = transaction.occurredAtEpochMillis,
                            now = now,
                        ),
                        transaction.paymentMethod.displayLabel(),
                    ).joinToString(separator = " - "),
                    amount = transaction.amount,
                    type = transaction.type,
                )
            },
            totalTransactionCount = sortedTransactions.size,
            loadState = UiLoadState.Content,
        )
    }

    private fun formatDate(epochMillis: Long, now: Long): String {
        val currentDate = Instant
            .ofEpochMilli(now)
            .atZone(zoneId)
            .toLocalDate()
        val transactionDate = Instant
            .ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()

        return when (transactionDate) {
            currentDate -> "Today"
            currentDate.minusDays(1) -> "Yesterday"
            else -> transactionDate.format(dateFormatter)
        }
    }

    private fun TransactionType.defaultTitle(): String = when (this) {
        TransactionType.EXPENSE -> "Expense"
        TransactionType.INCOME -> "Income"
        TransactionType.TRANSFER -> "Transfer"
    }

    private fun TransactionType.defaultSubtitle(): String = when (this) {
        TransactionType.EXPENSE -> "Expense"
        TransactionType.INCOME -> "Income"
        TransactionType.TRANSFER -> "Account transfer"
    }

    private fun PaymentMethod.displayLabel(): String = when (this) {
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.UPI -> "UPI"
        PaymentMethod.DEBIT_CARD -> "Debit card"
        PaymentMethod.CREDIT_CARD -> "Credit card"
        PaymentMethod.BANK_TRANSFER -> "Bank transfer"
        PaymentMethod.DIGITAL_WALLET -> "Digital wallet"
        PaymentMethod.OTHER -> "Other"
    }
}
