package com.princevekariya.projectledger.feature.reports

import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal class MonthlyReportDataMapper(
    private val timeProvider: EpochTimeProvider,
    private val zoneId: ZoneId,
) {
    private val monthFormatter = DateTimeFormatter.ofPattern(
        "MMMM yyyy",
        Locale.ENGLISH,
    )

    fun initialState(): MonthlyReportUiState = loadingStateFor(
        selectedMonth = currentMonth(),
    )

    fun loadingStateFor(selectedMonth: YearMonth): MonthlyReportUiState {
        val currentMonth = currentMonth()
        return MonthlyReportUiState(
            selectedMonth = selectedMonth,
            currentMonth = currentMonth,
            selectedMonthLabel = selectedMonth.format(monthFormatter),
            loadState = UiLoadState.Loading,
        )
    }

    fun map(sourceData: MonthlyReportSourceData, selectedMonth: YearMonth): MonthlyReportUiState {
        val currentMonth = currentMonth()
        val monthTransactions = sourceData.transactions.filter { transaction ->
            transaction.occurredIn(month = selectedMonth)
        }
        val incomeTransactions = monthTransactions.filter { transaction ->
            transaction.type == TransactionType.INCOME
        }
        val expenseTransactions = monthTransactions.filter { transaction ->
            transaction.type == TransactionType.EXPENSE
        }
        val income = incomeTransactions.sumAmounts()
        val expenses = expenseTransactions.sumAmounts()
        val netCashFlow = income - expenses

        return MonthlyReportUiState(
            selectedMonth = selectedMonth,
            currentMonth = currentMonth,
            selectedMonthLabel = selectedMonth.format(monthFormatter),
            income = income,
            expenses = expenses,
            netCashFlow = netCashFlow,
            transactionCount = incomeTransactions.size +
                expenseTransactions.size,
            categoryExpenses = mapCategoryExpenses(
                expenseTransactions = expenseTransactions,
                sourceData = sourceData,
                totalExpenses = expenses,
            ),
            loadState = UiLoadState.Content,
        )
    }

    private fun mapCategoryExpenses(
        expenseTransactions: List<LedgerTransaction>,
        sourceData: MonthlyReportSourceData,
        totalExpenses: Money,
    ): List<MonthlyCategoryExpenseItem> {
        if (expenseTransactions.isEmpty()) {
            return emptyList()
        }

        val categoriesById = sourceData.expenseCategories.associateBy {
                category ->
            category.id
        }

        return expenseTransactions
            .groupBy { transaction ->
                requireNotNull(transaction.categoryId)
            }
            .map { (categoryId, transactions) ->
                val amount = transactions.sumAmounts()
                val share = amount.minorUnits.toDouble() /
                    totalExpenses.minorUnits.toDouble()

                MonthlyCategoryExpenseItem(
                    id = categoryId,
                    name = categoriesById[categoryId]?.name
                        ?: "Archived category",
                    amount = amount,
                    shareFraction = share.toFloat().coerceIn(0f, 1f),
                    sharePercent = (share * 100.0)
                        .roundToInt()
                        .coerceIn(0, 100),
                    transactionCount = transactions.size,
                )
            }
            .sortedByDescending { category ->
                category.amount.minorUnits
            }
    }

    private fun LedgerTransaction.occurredIn(month: YearMonth): Boolean {
        val start = month
            .atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val end = month
            .plusMonths(1)
            .atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        return occurredAtEpochMillis in start until end
    }

    private fun List<LedgerTransaction>.sumAmounts(): Money = fold(
        initial = Money.zero(),
    ) { total, transaction ->
        total + transaction.amount
    }

    private fun currentMonth(): YearMonth = YearMonth.from(
        Instant
            .ofEpochMilli(timeProvider.currentTimeMillis())
            .atZone(zoneId),
    )
}
