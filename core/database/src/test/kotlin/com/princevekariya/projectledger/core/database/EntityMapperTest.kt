package com.princevekariya.projectledger.core.database

import com.princevekariya.projectledger.core.database.mapper.toEntity
import com.princevekariya.projectledger.core.database.mapper.toModel
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.Budget
import com.princevekariya.projectledger.core.model.BudgetPeriod
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Merchant
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.core.model.TransactionSource
import com.princevekariya.projectledger.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMapperTest {
    @Test
    fun `account survives entity round trip`() {
        val model = FinancialAccount(
            id = "account-cash",
            name = "Cash",
            type = AccountType.CASH,
            openingBalance = Money(minorUnits = 2_500L),
            currentBalance = Money(minorUnits = 1_900L),
        )

        assertEquals(model, model.toEntity().toModel())
    }

    @Test
    fun `category survives entity round trip`() {
        val model = TransactionCategory(
            id = "category-food",
            name = "Food and Dining",
            type = CategoryType.EXPENSE,
            iconKey = "restaurant",
            isDefault = true,
        )

        assertEquals(model, model.toEntity().toModel())
    }

    @Test
    fun `merchant survives entity round trip`() {
        val model = Merchant(
            id = "merchant-canteen",
            name = "College Canteen",
        )

        assertEquals(model, model.toEntity().toModel())
    }

    @Test
    fun `expense survives entity round trip`() {
        val model = LedgerTransaction(
            id = "transaction-lunch",
            type = TransactionType.EXPENSE,
            amount = Money(minorUnits = 12_050L),
            accountId = "account-bank",
            occurredAtEpochMillis = 1_725_000_000_000L,
            paymentMethod = PaymentMethod.UPI,
            categoryId = "category-food",
            merchantId = "merchant-canteen",
            source = TransactionSource.MANUAL,
            note = "Lunch",
        )

        assertEquals(model, model.toEntity().toModel())
    }

    @Test
    fun `budget survives entity round trip`() {
        val model = Budget(
            id = "budget-food",
            name = "Monthly food",
            limit = Money(minorUnits = 500_000L),
            period = BudgetPeriod.MONTHLY,
            startEpochMillis = 1_725_000_000_000L,
            endEpochMillis = 1_727_678_400_000L,
            categoryId = "category-food",
        )

        assertEquals(model, model.toEntity().toModel())
    }
}
