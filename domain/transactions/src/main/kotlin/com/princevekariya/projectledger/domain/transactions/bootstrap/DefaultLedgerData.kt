package com.princevekariya.projectledger.domain.transactions.bootstrap

import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.TransactionCategory

object DefaultLedgerData {
    val accounts: List<FinancialAccount> = listOf(
        FinancialAccount(
            id = "account-cash",
            name = "Cash",
            type = AccountType.CASH,
        ),
    )

    val categories: List<TransactionCategory> = listOf(
        expenseCategory(
            id = "category-food",
            name = "Food and Dining",
            iconKey = "restaurant",
        ),
        expenseCategory(
            id = "category-transport",
            name = "Transport",
            iconKey = "directions_bus",
        ),
        expenseCategory(
            id = "category-shopping",
            name = "Shopping",
            iconKey = "shopping_bag",
        ),
        expenseCategory(
            id = "category-bills",
            name = "Bills and Recharge",
            iconKey = "receipt_long",
        ),
        expenseCategory(
            id = "category-health",
            name = "Health",
            iconKey = "medical_services",
        ),
        expenseCategory(
            id = "category-education",
            name = "Education",
            iconKey = "school",
        ),
        expenseCategory(
            id = "category-entertainment",
            name = "Entertainment",
            iconKey = "movie",
        ),
        expenseCategory(
            id = "category-other-expense",
            name = "Other Expense",
            iconKey = "more_horiz",
        ),
        incomeCategory(
            id = "category-pocket-money",
            name = "Pocket Money",
            iconKey = "account_balance_wallet",
        ),
        incomeCategory(
            id = "category-salary",
            name = "Salary",
            iconKey = "payments",
        ),
        incomeCategory(
            id = "category-refund",
            name = "Refund",
            iconKey = "currency_exchange",
        ),
        incomeCategory(
            id = "category-other-income",
            name = "Other Income",
            iconKey = "add_circle",
        ),
    )

    private fun expenseCategory(id: String, name: String, iconKey: String): TransactionCategory = TransactionCategory(
        id = id,
        name = name,
        type = CategoryType.EXPENSE,
        iconKey = iconKey,
        isDefault = true,
    )

    private fun incomeCategory(id: String, name: String, iconKey: String): TransactionCategory = TransactionCategory(
        id = id,
        name = name,
        type = CategoryType.INCOME,
        iconKey = iconKey,
        isDefault = true,
    )
}
