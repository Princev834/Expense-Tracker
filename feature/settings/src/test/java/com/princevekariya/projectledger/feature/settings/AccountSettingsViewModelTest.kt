package com.princevekariya.projectledger.feature.settings

import com.princevekariya.projectledger.core.common.AppLogLevel
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.domain.transactions.account.AccountIdGenerator
import com.princevekariya.projectledger.domain.transactions.account.CreateFinancialAccountUseCase
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun observedAccountsAreSortedAndMapped() = runTest {
        val repository = FakeAccountRepository(
            initialAccounts = listOf(
                account(
                    id = "wallet",
                    name = "Wallet",
                    type = AccountType.DIGITAL_WALLET,
                ),
                account(
                    id = "cash",
                    name = "Cash",
                    type = AccountType.CASH,
                ),
            ),
        )
        val viewModel = createViewModel(repository = repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UiLoadState.Content, state.loadState)
        assertEquals(
            listOf("Cash", "Wallet"),
            state.accounts.map { item -> item.name },
        )
        assertEquals(
            AccountType.CASH,
            state.accounts.first().type,
        )
    }

    @Test
    fun completedFormCreatesAccountAndResetsInputs() = runTest {
        val repository = FakeAccountRepository()
        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.onAction(
            AccountSettingsAction.AddAccountRequested,
        )
        viewModel.onAction(
            AccountSettingsAction.AccountNameChanged(
                value = "HDFC Bank",
            ),
        )
        viewModel.onAction(
            AccountSettingsAction.OpeningBalanceChanged(
                value = "1250.50",
            ),
        )
        viewModel.onAction(
            AccountSettingsAction.AccountTypeSelected(
                value = AccountType.BANK_ACCOUNT,
            ),
        )
        viewModel.onAction(
            AccountSettingsAction.SaveAccountRequested,
        )
        advanceUntilIdle()

        val account = repository.accounts.value.single()
        val state = viewModel.uiState.value
        assertEquals("HDFC Bank", account.name)
        assertEquals(AccountType.BANK_ACCOUNT, account.type)
        assertEquals(125_050L, account.currentBalance.minorUnits)
        assertFalse(state.isFormVisible)
        assertEquals("", state.accountNameInput)
        assertEquals("0", state.openingBalanceInput)
        assertTrue(state.userMessage?.text.orEmpty().contains("created"))
    }

    @Test
    fun invalidFormDoesNotCreateAccount() = runTest {
        val repository = FakeAccountRepository()
        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.onAction(
            AccountSettingsAction.AddAccountRequested,
        )
        viewModel.onAction(
            AccountSettingsAction.OpeningBalanceChanged(
                value = "invalid",
            ),
        )
        viewModel.onAction(
            AccountSettingsAction.SaveAccountRequested,
        )
        advanceUntilIdle()

        assertTrue(repository.accounts.value.isEmpty())
        assertTrue(
            viewModel.uiState.value.userMessage
                ?.text
                .orEmpty()
                .contains("valid opening balance"),
        )
    }

    @Test
    fun sourceFailureProducesErrorStateAndLog() = runTest {
        val logger = RecordingAppLogger()
        val repository = FailingAccountRepository()
        val viewModel = AccountSettingsViewModel(
            accountRepository = repository,
            createFinancialAccount = CreateFinancialAccountUseCase(
                accountRepository = repository,
                idGenerator = AccountIdGenerator {
                    "unused-id"
                },
            ),
            appLogger = logger,
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loadState is UiLoadState.Error)
        assertEquals(AppLogLevel.ERROR, logger.entries.single().level)
        assertEquals(
            "account_settings_load_failed",
            logger.entries.single().event,
        )
    }

    private fun createViewModel(repository: FakeAccountRepository): AccountSettingsViewModel = AccountSettingsViewModel(
        accountRepository = repository,
        createFinancialAccount = CreateFinancialAccountUseCase(
            accountRepository = repository,
            idGenerator = AccountIdGenerator {
                "account-generated"
            },
        ),
        appLogger = RecordingAppLogger(),
    )

    private class FakeAccountRepository(
        initialAccounts: List<FinancialAccount> = emptyList(),
    ) : AccountRepository {
        val accounts = MutableStateFlow(initialAccounts)

        override fun observeAll(): Flow<List<FinancialAccount>> = accounts

        override suspend fun findById(id: String): FinancialAccount? = accounts.value.firstOrNull { account ->
            account.id == id
        }

        override suspend fun save(account: FinancialAccount) {
            accounts.value = accounts.value
                .filterNot { item -> item.id == account.id } + account
        }
    }

    private class FailingAccountRepository : AccountRepository {
        override fun observeAll(): Flow<List<FinancialAccount>> = flow {
            throw IllegalStateException("Database unavailable")
        }

        override suspend fun findById(id: String): FinancialAccount? = null

        override suspend fun save(account: FinancialAccount) = Unit
    }

    private class RecordingAppLogger : AppLogger {
        val entries = mutableListOf<LogEntry>()

        override fun log(level: AppLogLevel, event: String, message: String, throwable: Throwable?) {
            entries += LogEntry(
                level = level,
                event = event,
            )
        }
    }

    private data class LogEntry(
        val level: AppLogLevel,
        val event: String,
    )

    private companion object {
        fun account(id: String, name: String, type: AccountType): FinancialAccount = FinancialAccount(
            id = id,
            name = name,
            type = type,
            openingBalance = Money.zero(),
        )
    }
}
