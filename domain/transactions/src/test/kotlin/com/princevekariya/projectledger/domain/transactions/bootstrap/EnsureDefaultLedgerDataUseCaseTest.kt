package com.princevekariya.projectledger.domain.transactions.bootstrap

import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EnsureDefaultLedgerDataUseCaseTest {
    @Test
    fun createsEveryMissingDefaultRecord() = runBlocking {
        val accounts = FakeAccountRepository()
        val categories = FakeCategoryRepository()
        val useCase = EnsureDefaultLedgerDataUseCase(
            accountRepository = accounts,
            categoryRepository = categories,
        )

        val result = useCase()

        assertEquals(DefaultLedgerData.accounts.size, result.createdAccounts)
        assertEquals(DefaultLedgerData.categories.size, result.createdCategories)
        assertEquals(
            DefaultLedgerData.accounts,
            accounts.currentAccounts(),
        )
        assertEquals(
            DefaultLedgerData.categories,
            categories.currentCategories(),
        )
    }

    @Test
    fun runningBootstrapTwiceDoesNotCreateDuplicates() = runBlocking {
        val accounts = FakeAccountRepository()
        val categories = FakeCategoryRepository()
        val useCase = EnsureDefaultLedgerDataUseCase(
            accountRepository = accounts,
            categoryRepository = categories,
        )

        val firstResult = useCase()
        val secondResult = useCase()

        assertEquals(
            DefaultLedgerData.accounts.size + DefaultLedgerData.categories.size,
            firstResult.createdItems,
        )
        assertEquals(0, secondResult.createdItems)
        assertEquals(
            DefaultLedgerData.accounts.size,
            accounts.currentAccounts().size,
        )
        assertEquals(
            DefaultLedgerData.categories.size,
            categories.currentCategories().size,
        )
    }

    @Test
    fun existingRecordsWithDefaultIdentifiersArePreserved() = runBlocking {
        val customCash = FinancialAccount(
            id = DefaultLedgerData.accounts.single().id,
            name = "My Cash",
            type = AccountType.CASH,
        )
        val customFood = TransactionCategory(
            id = "category-food",
            name = "College Food",
            type = CategoryType.EXPENSE,
            iconKey = "lunch_dining",
        )
        val accounts = FakeAccountRepository(initialAccounts = listOf(customCash))
        val categories = FakeCategoryRepository(
            initialCategories = listOf(customFood),
        )
        val useCase = EnsureDefaultLedgerDataUseCase(
            accountRepository = accounts,
            categoryRepository = categories,
        )

        val result = useCase()

        assertEquals(0, result.createdAccounts)
        assertEquals(DefaultLedgerData.categories.size - 1, result.createdCategories)
        assertSame(customCash, accounts.findById(id = customCash.id))
        assertSame(customFood, categories.findById(id = customFood.id))
    }

    private class FakeAccountRepository(
        initialAccounts: List<FinancialAccount> = emptyList(),
    ) : AccountRepository {
        private val accounts = linkedMapOf<String, FinancialAccount>().apply {
            initialAccounts.forEach { account ->
                put(account.id, account)
            }
        }
        private val state = MutableStateFlow(accounts.values.toList())

        override fun observeAll(): Flow<List<FinancialAccount>> = state

        override suspend fun findById(id: String): FinancialAccount? = accounts[id]

        override suspend fun save(account: FinancialAccount) {
            accounts[account.id] = account
            state.value = accounts.values.toList()
        }

        fun currentAccounts(): List<FinancialAccount> = accounts.values.toList()
    }

    private class FakeCategoryRepository(
        initialCategories: List<TransactionCategory> = emptyList(),
    ) : CategoryRepository {
        private val categories = linkedMapOf<String, TransactionCategory>().apply {
            initialCategories.forEach { category ->
                put(category.id, category)
            }
        }
        private val expenseState = MutableStateFlow(
            categories.values.filter { category ->
                category.type == CategoryType.EXPENSE
            },
        )
        private val incomeState = MutableStateFlow(
            categories.values.filter { category ->
                category.type == CategoryType.INCOME
            },
        )

        override fun observeActive(type: CategoryType): Flow<List<TransactionCategory>> = when (type) {
            CategoryType.EXPENSE -> expenseState
            CategoryType.INCOME -> incomeState
        }

        override suspend fun findById(id: String): TransactionCategory? = categories[id]

        override suspend fun save(category: TransactionCategory) {
            categories[category.id] = category
            expenseState.value = categories.values.filter { item ->
                item.type == CategoryType.EXPENSE
            }
            incomeState.value = categories.values.filter { item ->
                item.type == CategoryType.INCOME
            }
        }

        fun currentCategories(): List<TransactionCategory> = categories.values.toList()
    }
}
