package com.princevekariya.projectledger.domain.transactions.command

import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Merchant
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.core.model.TransactionSource
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveManualTransactionUseCaseTest {
    @Test
    fun savesExpenseAndUpdatedAccountTogether() = runBlocking {
        val fixture = Fixture()
        val useCase = fixture.createUseCase(
            generatedIds = mutableListOf("transaction-one"),
        )

        val saved = useCase(
            draft = ManualTransactionDraft(
                type = TransactionType.EXPENSE,
                amount = Money(minorUnits = 12_050L),
                accountId = "  account-cash  ",
                categoryId = "  category-food  ",
                paymentMethod = PaymentMethod.UPI,
                merchantSearchKey = "  swiggy  ",
                note = "  Dinner  ",
            ),
        )

        val atomicWrite = fixture.transactions.atomicWrites.single()
        assertEquals("transaction-one", saved.id)
        assertEquals("account-cash", saved.accountId)
        assertEquals("category-food", saved.categoryId)
        assertEquals("merchant-swiggy", saved.merchantId)
        assertEquals("Dinner", saved.note)
        assertEquals(TransactionSource.MANUAL, saved.source)
        assertEquals(FIXED_TIME, saved.occurredAtEpochMillis)
        assertEquals(saved, atomicWrite.transaction)
        assertEquals(
            37_950L,
            atomicWrite.updatedAccount.currentBalance.minorUnits,
        )
        assertEquals(
            50_000L,
            atomicWrite.updatedAccount.openingBalance.minorUnits,
        )
    }

    @Test
    fun incomeIncreasesTheAccountBalance() = runBlocking {
        val fixture = Fixture()
        val useCase = fixture.createUseCase(
            generatedIds = mutableListOf("transaction-income"),
        )

        useCase(
            draft = ManualTransactionDraft(
                type = TransactionType.INCOME,
                amount = Money(minorUnits = 20_000L),
                accountId = "account-cash",
                categoryId = "category-salary",
                paymentMethod = PaymentMethod.BANK_TRANSFER,
            ),
        )

        assertEquals(
            70_000L,
            fixture.transactions
                .atomicWrites
                .single()
                .updatedAccount
                .currentBalance
                .minorUnits,
        )
    }

    @Test
    fun blankOptionalNoteIsStoredAsNull() = runBlocking {
        val fixture = Fixture()
        val saved = fixture.createUseCase(
            generatedIds = mutableListOf("transaction-two"),
        )(
            draft = fixture.expenseDraft(note = "   "),
        )

        assertNull(saved.note)
    }

    @Test
    fun categoryMustMatchTheTransactionType() = runBlocking {
        val fixture = Fixture()
        val useCase = fixture.createUseCase(
            generatedIds = mutableListOf("transaction-three"),
        )

        val failure = runCatching {
            useCase(
                draft = fixture.expenseDraft(
                    categoryId = "category-salary",
                ),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(
            failure?.message.orEmpty().contains("does not match"),
        )
        assertTrue(fixture.transactions.atomicWrites.isEmpty())
    }

    @Test
    fun missingOrArchivedReferencesAreRejected() = runBlocking {
        val missingFixture = Fixture()
        val missingUseCase = missingFixture.createUseCase(
            generatedIds = mutableListOf("transaction-four"),
        )
        val missingFailure = runCatching {
            missingUseCase(
                draft = missingFixture.expenseDraft(
                    accountId = "missing-account",
                ),
            )
        }.exceptionOrNull()

        val archivedFixture = Fixture(
            accounts = listOf(
                FinancialAccount(
                    id = "account-cash",
                    name = "Cash",
                    type = AccountType.CASH,
                    isArchived = true,
                ),
            ),
        )
        val archivedUseCase = archivedFixture.createUseCase(
            generatedIds = mutableListOf("transaction-five"),
        )
        val archivedFailure = runCatching {
            archivedUseCase(
                draft = archivedFixture.expenseDraft(),
            )
        }.exceptionOrNull()

        assertTrue(missingFailure is IllegalArgumentException)
        assertTrue(archivedFailure is IllegalArgumentException)
        assertTrue(missingFixture.transactions.atomicWrites.isEmpty())
        assertTrue(archivedFixture.transactions.atomicWrites.isEmpty())
    }

    @Test
    fun generatedIdentifierCollisionIsRetried() = runBlocking {
        val fixture = Fixture()
        fixture.transactions.seed(
            transaction = fixture.existingTransaction(
                id = "duplicate-id",
            ),
        )
        val useCase = fixture.createUseCase(
            generatedIds = mutableListOf(
                "duplicate-id",
                "unique-id",
            ),
        )

        val saved = useCase(draft = fixture.expenseDraft())

        assertEquals("unique-id", saved.id)
        assertEquals(2, fixture.transactions.saved.size)
    }

    @Test
    fun explicitOccurrenceTimeIsPreserved() = runBlocking {
        val fixture = Fixture()
        val saved = fixture.createUseCase(
            generatedIds = mutableListOf("transaction-six"),
        )(
            draft = fixture.expenseDraft(
                occurredAtEpochMillis = 5_000L,
            ),
        )

        assertEquals(5_000L, saved.occurredAtEpochMillis)
        assertEquals(FIXED_TIME, saved.createdAtEpochMillis)
        assertEquals(FIXED_TIME, saved.updatedAtEpochMillis)
    }

    private class Fixture(
        accounts: List<FinancialAccount> = defaultAccounts(),
    ) {
        private val accountRepository =
            FakeAccountRepository(accounts = accounts)
        private val categoryRepository = FakeCategoryRepository(
            categories = defaultCategories(),
        )
        private val merchantRepository = FakeMerchantRepository(
            merchants = defaultMerchants(),
        )
        val transactions = RecordingTransactionRepository()

        fun createUseCase(generatedIds: MutableList<String>): SaveManualTransactionUseCase =
            SaveManualTransactionUseCase(
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                merchantRepository = merchantRepository,
                transactionRepository = transactions,
                idGenerator = TransactionIdGenerator {
                    generatedIds.removeFirst()
                },
                timeProvider = EpochTimeProvider {
                    FIXED_TIME
                },
            )

        fun expenseDraft(
            accountId: String = "account-cash",
            categoryId: String = "category-food",
            occurredAtEpochMillis: Long? = null,
            note: String? = null,
        ): ManualTransactionDraft = ManualTransactionDraft(
            type = TransactionType.EXPENSE,
            amount = Money(minorUnits = 10_000L),
            accountId = accountId,
            categoryId = categoryId,
            paymentMethod = PaymentMethod.CASH,
            occurredAtEpochMillis = occurredAtEpochMillis,
            note = note,
        )

        fun existingTransaction(id: String): LedgerTransaction = LedgerTransaction(
            id = id,
            type = TransactionType.EXPENSE,
            amount = Money(minorUnits = 100L),
            accountId = "account-cash",
            categoryId = "category-food",
            occurredAtEpochMillis = 1_000L,
            paymentMethod = PaymentMethod.CASH,
        )
    }

    private class FakeAccountRepository(
        accounts: List<FinancialAccount>,
    ) : AccountRepository {
        private val records = accounts.associateBy { account ->
            account.id
        }

        override fun observeAll(): Flow<List<FinancialAccount>> = MutableStateFlow(records.values.toList())

        override suspend fun findById(id: String): FinancialAccount? = records[id]

        override suspend fun save(account: FinancialAccount) {
            error("Saving accounts is not expected in this test.")
        }
    }

    private class FakeCategoryRepository(
        categories: List<TransactionCategory>,
    ) : CategoryRepository {
        private val records = categories.associateBy { category ->
            category.id
        }
        private val state = MutableStateFlow(records.values.toList())

        override fun observeActive(type: CategoryType): Flow<List<TransactionCategory>> = state.map { categories ->
            categories.filter { category ->
                category.type == type && !category.isArchived
            }
        }

        override suspend fun findById(id: String): TransactionCategory? = records[id]

        override suspend fun save(category: TransactionCategory) {
            error("Saving categories is not expected in this test.")
        }
    }

    private class FakeMerchantRepository(
        merchants: List<Merchant>,
    ) : MerchantRepository {
        private val records = merchants.associateBy { merchant ->
            merchant.searchKey
        }

        override fun observeActive(): Flow<List<Merchant>> = MutableStateFlow(records.values.toList())

        override suspend fun findBySearchKey(searchKey: String): Merchant? = records[searchKey]

        override suspend fun save(merchant: Merchant) {
            error("Saving merchants is not expected in this test.")
        }
    }

    private class RecordingTransactionRepository :
        TransactionRepository {
        val saved = mutableListOf<LedgerTransaction>()
        val atomicWrites = mutableListOf<AtomicWrite>()

        override fun observeAll(): Flow<List<LedgerTransaction>> = MutableStateFlow(saved)

        override fun observeRecent(limit: Int): Flow<List<LedgerTransaction>> = MutableStateFlow(saved.take(limit))

        override suspend fun findById(id: String): LedgerTransaction? = saved.firstOrNull { transaction ->
            transaction.id == id
        }

        override suspend fun save(transaction: LedgerTransaction) {
            seed(transaction = transaction)
        }

        override suspend fun saveWithUpdatedAccount(transaction: LedgerTransaction, updatedAccount: FinancialAccount) {
            seed(transaction = transaction)
            atomicWrites += AtomicWrite(
                transaction = transaction,
                updatedAccount = updatedAccount,
            )
        }

        override suspend fun deleteById(id: String): Boolean = saved.removeAll { transaction ->
            transaction.id == id
        }

        fun seed(transaction: LedgerTransaction) {
            saved.removeAll { item ->
                item.id == transaction.id
            }
            saved += transaction
        }
    }

    private data class AtomicWrite(
        val transaction: LedgerTransaction,
        val updatedAccount: FinancialAccount,
    )

    private companion object {
        const val FIXED_TIME: Long = 10_000L

        fun defaultAccounts(): List<FinancialAccount> = listOf(
            FinancialAccount(
                id = "account-cash",
                name = "Cash",
                type = AccountType.CASH,
                openingBalance = Money(minorUnits = 50_000L),
                currentBalance = Money(minorUnits = 50_000L),
            ),
        )

        fun defaultCategories(): List<TransactionCategory> = listOf(
            TransactionCategory(
                id = "category-food",
                name = "Food and Dining",
                type = CategoryType.EXPENSE,
                iconKey = "restaurant",
            ),
            TransactionCategory(
                id = "category-salary",
                name = "Salary",
                type = CategoryType.INCOME,
                iconKey = "payments",
            ),
        )

        fun defaultMerchants(): List<Merchant> = listOf(
            Merchant(
                id = "merchant-swiggy",
                name = "Swiggy",
            ),
        )
    }
}
