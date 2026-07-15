package com.princevekariya.projectledger.feature.dashboard

import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Merchant
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class DashboardDataMapper(
    private val timeProvider: EpochTimeProvider,
    private val zoneId: ZoneId,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern(
        "dd MMM",
        Locale.ENGLISH,
    )

    fun map(baseState: DashboardUiState, sourceData: DashboardSourceData): DashboardUiState {
        val now = timeProvider.currentTimeMillis()
        val activeAccounts = sourceData.accounts.filterNot { account ->
            account.isArchived
        }
        val monthWindow = currentMonthWindow(now = now)
        val monthlyTransactions = sourceData.transactions.filter { transaction ->
            transaction.occurredAtEpochMillis >= monthWindow.startEpochMillis &&
                transaction.occurredAtEpochMillis < monthWindow.endEpochMillis
        }

        return baseState.copy(
            totalBalance = activeAccounts.sumBalances(),
            incomeThisMonth = monthlyTransactions.sumByType(
                type = TransactionType.INCOME,
            ),
            expensesThisMonth = monthlyTransactions.sumByType(
                type = TransactionType.EXPENSE,
            ),
            activeAccountCount = activeAccounts.size,
            recentTransactions = sourceData.toRecentItems(now = now),
            loadState = UiLoadState.Content,
        )
    }

    private fun DashboardSourceData.toRecentItems(now: Long): List<DashboardTransactionItem> {
        val categories = (
            expenseCategories + incomeCategories
            ).associateBy { category ->
            category.id
        }
        val merchantsById = merchants.associateBy { merchant ->
            merchant.id
        }

        return transactions
            .sortedWith(
                compareByDescending<LedgerTransaction> { transaction ->
                    transaction.occurredAtEpochMillis
                }.thenByDescending { transaction ->
                    transaction.createdAtEpochMillis
                },
            )
            .take(RECENT_TRANSACTION_LIMIT)
            .map { transaction ->
                transaction.toDashboardItem(
                    categories = categories,
                    merchants = merchantsById,
                    now = now,
                )
            }
    }

    private fun LedgerTransaction.toDashboardItem(
        categories: Map<String, TransactionCategory>,
        merchants: Map<String, Merchant>,
        now: Long,
    ): DashboardTransactionItem {
        val categoryName = categoryId
            ?.let(categories::get)
            ?.name
        val merchantName = merchantId
            ?.let(merchants::get)
            ?.name
        val title = merchantName
            ?: note
            ?: categoryName
            ?: type.defaultTitle()
        val subtitlePrefix = categoryName
            ?: type.defaultSubtitle()
        val dateLabel = formatDate(
            epochMillis = occurredAtEpochMillis,
            now = now,
        )

        return DashboardTransactionItem(
            id = id,
            title = title,
            subtitle = "$subtitlePrefix - $dateLabel",
            amount = amount,
            type = type,
        )
    }

    private fun List<FinancialAccount>.sumBalances(): Money = fold(
        initial = Money.zero(),
    ) { total, account ->
        total + account.currentBalance
    }

    private fun List<LedgerTransaction>.sumByType(type: TransactionType): Money = asSequence()
        .filter { transaction ->
            transaction.type == type
        }
        .map { transaction ->
            transaction.amount
        }
        .fold(Money.zero()) { total, amount ->
            total + amount
        }

    private fun currentMonthWindow(now: Long): MonthWindow {
        val currentDateTime = Instant
            .ofEpochMilli(now)
            .atZone(zoneId)
        val start = currentDateTime
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(zoneId)
        val end = start.plusMonths(1)

        return MonthWindow(
            startEpochMillis = start.toInstant().toEpochMilli(),
            endEpochMillis = end.toInstant().toEpochMilli(),
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

    private data class MonthWindow(
        val startEpochMillis: Long,
        val endEpochMillis: Long,
    )

    private companion object {
        const val RECENT_TRANSACTION_LIMIT: Int = 5
    }
}
