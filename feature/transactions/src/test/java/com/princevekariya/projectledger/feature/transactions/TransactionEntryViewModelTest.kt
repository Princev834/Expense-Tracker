package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.common.NoOpAppLogger
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Merchant
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import com.princevekariya.projectledger.domain.transactions.command.SaveManualTransactionUseCase
import com.princevekariya.projectledger.domain.transactions.command.TransactionIdGenerator
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEntryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsActiveAccountsAndExpenseCategories() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("account-cash"), state.accounts.map { it.id })
        assertEquals(
            listOf("category-food"),
            state.categories.map { it.id },
        )
        assertEquals("account-cash", state.selectedAccountId)
        assertEquals("category-food", state.selectedCategoryId)
        assertFalse(state.isLoadingReferences)
    }

    @Test
    fun changingToIncomeLoadsIncomeCategories() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        viewModel.onAction(
            TransactionEntryAction.TransactionTypeChanged(
                value = TransactionType.INCOME,
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TransactionType.INCOME, state.transactionType)
        assertEquals(
            listOf("category-salary"),
            state.categories.map { it.id },
        )
        assertEquals("category-salary", state.selectedCategoryId)
    }

    @Test
    fun validFormSavesAndClearsEditableInputs() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        viewModel.onAction(
            TransactionEntryAction.AmountChanged(value = "125.50"),
        )
        viewModel.onAction(
            TransactionEntryAction.NoteChanged(value = "College lunch"),
        )
        viewModel.onAction(
            TransactionEntryAction.PaymentMethodSelected(
                value = PaymentMethod.UPI,
            ),
        )
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onAction(TransactionEntryAction.SaveClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val saved = fixture.transactions.saved.single()
        assertEquals(12_550L, saved.amount.minorUnits)
        assertEquals("College lunch", saved.note)
        assertEquals(PaymentMethod.UPI, saved.paymentMethod)
        assertEquals("", state.amountInput)
        assertEquals("", state.noteInput)
        assertFalse(state.isSaving)
        assertEquals("Transaction saved.", state.userMessage?.text)
    }

    @Test
    fun incompleteFormDoesNotCallTheSaveCommand() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        viewModel.onAction(TransactionEntryAction.SaveClicked)
        advanceUntilIdle()

        assertTrue(fixture.transactions.saved.isEmpty())
        assertEquals(
            "Complete the required transaction details.",
            viewModel.uiState.value.userMessage?.text,
        )
    }

    @Test
    fun unavailableSelectionsAreIgnored() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        viewModel.onAction(
            TransactionEntryAction.AccountSelected(
                accountId = "missing-account",
            ),
        )
        viewModel.onAction(
            TransactionEntryAction.CategorySelected(
                categoryId = "missing-category",
            ),
        )

        val state = viewModel.uiState.value
        assertEquals("account-cash", state.selectedAccountId)
        assertEquals("category-food", state.selectedCategoryId)
    }

    @Test
    fun consumedMessageIsRemovedOnlyWhenIdentifiersMatch() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        viewModel.onAction(TransactionEntryAction.SaveClicked)
        val message = viewModel.uiState.value.userMessage
        requireNotNull(message)

        viewModel.onAction(
            TransactionEntryAction.MessageShown(id = message.id + 1L),
        )
        assertEquals(message, viewModel.uiState.value.userMessage)

        viewModel.onAction(
            TransactionEntryAction.MessageShown(id = message.id),
        )
        assertNull(viewModel.uiState.value.userMessage)
    }

    private class Fixture {
        private val accounts = FakeAccountRepository(
            initialAccounts = listOf(
                FinancialAccount(
                    id = "account-cash",
                    name = "Cash",
                    type = AccountType.CASH,
                ),
                FinancialAccount(
                    id = "account-archived",
                    name = "Archived",
                    type = AccountType.OTHER,
                    isArchived = true,
                ),
            ),
        )
        private val categories = FakeCategoryRepository(
            initialCategories = listOf(
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
            ),
        )
        private val merchants = FakeMerchantRepository()
        val transactions = RecordingTransactionRepository()

        fun createViewModel(): TransactionEntryViewModel {
            val saver = SaveManualTransactionUseCase(
                accountRepository = accounts,
                categoryRepository = categories,
                merchantRepository = merchants,
                transactionRepository = transactions,
                idGenerator = TransactionIdGenerator {
                    "transaction-test"
                },
                timeProvider = EpochTimeProvider {
                    FIXED_TIME
                },
            )
            return TransactionEntryViewModel(
                accountRepository = accounts,
                categoryRepository = categories,
                saveManualTransaction = saver,
                appLogger = NoOpAppLogger,
            )
        }
    }

    private class FakeAccountRepository(
        initialAccounts: List<FinancialAccount>,
    ) : AccountRepository {
        private val records = initialAccounts.associateBy { account ->
            account.id
        }
        private val state = MutableStateFlow(initialAccounts)

        override fun observeAll(): Flow<List<FinancialAccount>> = state

        override suspend fun findById(id: String): FinancialAccount? = records[id]

        override suspend fun save(account: FinancialAccount) {
            error("Saving accounts is not expected in this test.")
        }
    }

    private class FakeCategoryRepository(
        initialCategories: List<TransactionCategory>,
    ) : CategoryRepository {
        private val records = initialCategories.associateBy { category ->
            category.id
        }
        private val state = MutableStateFlow(initialCategories)

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

    private class FakeMerchantRepository : MerchantRepository {
        override fun observeActive(): Flow<List<Merchant>> = MutableStateFlow(emptyList())

        override suspend fun findBySearchKey(searchKey: String): Merchant? = null

        override suspend fun save(merchant: Merchant) {
            error("Saving merchants is not expected in this test.")
        }
    }

    private class RecordingTransactionRepository : TransactionRepository {
        val saved = mutableListOf<LedgerTransaction>()

        override fun observeAll(): Flow<List<LedgerTransaction>> = MutableStateFlow(saved)

        override fun observeRecent(limit: Int): Flow<List<LedgerTransaction>> = MutableStateFlow(saved.take(limit))

        override suspend fun findById(id: String): LedgerTransaction? = saved.firstOrNull { transaction ->
            transaction.id == id
        }

        override suspend fun save(transaction: LedgerTransaction) {
            saved += transaction
        }

        override suspend fun saveWithUpdatedAccount(transaction: LedgerTransaction, updatedAccount: FinancialAccount) {
            saved += transaction
        }

        override suspend fun deleteById(id: String): Boolean = saved.removeAll { transaction ->
            transaction.id == id
        }
    }

    private companion object {
        const val FIXED_TIME: Long = 20_000L
    }
}
