package com.princevekariya.projectledger.domain.transactions.account

import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateFinancialAccountUseCaseTest {
    @Test
    fun createsAccountWithTrimmedNameAndOpeningBalance() = runBlocking {
        val repository = FakeAccountRepository()
        val useCase = CreateFinancialAccountUseCase(
            accountRepository = repository,
            idGenerator = AccountIdGenerator {
                "account-bank"
            },
        )

        val account = useCase(
            draft = CreateFinancialAccountDraft(
                name = "  HDFC Bank  ",
                type = AccountType.BANK_ACCOUNT,
                openingBalance = Money(minorUnits = 125_050L),
            ),
        )

        assertEquals("account-bank", account.id)
        assertEquals("HDFC Bank", account.name)
        assertEquals(AccountType.BANK_ACCOUNT, account.type)
        assertEquals(125_050L, account.openingBalance.minorUnits)
        assertEquals(125_050L, account.currentBalance.minorUnits)
        assertEquals(account, repository.accounts.value.single())
    }

    @Test
    fun duplicateNameIsRejectedIgnoringCase() = runBlocking {
        val repository = FakeAccountRepository(
            initialAccounts = listOf(
                account(
                    id = "account-existing",
                    name = "Cash",
                ),
            ),
        )
        val useCase = CreateFinancialAccountUseCase(
            accountRepository = repository,
            idGenerator = AccountIdGenerator {
                "account-new"
            },
        )

        val failure = runCatching {
            useCase(
                draft = CreateFinancialAccountDraft(
                    name = " cash ",
                    type = AccountType.CASH,
                    openingBalance = Money.zero(),
                ),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(
            failure?.message.orEmpty().contains("already exists"),
        )
        assertEquals(1, repository.accounts.value.size)
    }

    @Test
    fun generatedIdentifierCollisionIsRetried() = runBlocking {
        val repository = FakeAccountRepository(
            initialAccounts = listOf(
                account(
                    id = "duplicate-id",
                    name = "Cash",
                ),
            ),
        )
        val generatedIds = mutableListOf(
            "duplicate-id",
            "unique-id",
        )
        val useCase = CreateFinancialAccountUseCase(
            accountRepository = repository,
            idGenerator = AccountIdGenerator {
                generatedIds.removeFirst()
            },
        )

        val account = useCase(
            draft = CreateFinancialAccountDraft(
                name = "Wallet",
                type = AccountType.DIGITAL_WALLET,
                openingBalance = Money(minorUnits = 5_000L),
            ),
        )

        assertEquals("unique-id", account.id)
        assertEquals(2, repository.accounts.value.size)
    }

    @Test
    fun accountNameLengthIsValidated() = runBlocking {
        val repository = FakeAccountRepository()
        val useCase = CreateFinancialAccountUseCase(
            accountRepository = repository,
            idGenerator = AccountIdGenerator {
                "unused-id"
            },
        )

        val failure = runCatching {
            useCase(
                draft = CreateFinancialAccountDraft(
                    name = "A".repeat(41),
                    type = AccountType.OTHER,
                    openingBalance = Money.zero(),
                ),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(repository.accounts.value.isEmpty())
    }

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

    private companion object {
        fun account(id: String, name: String): FinancialAccount = FinancialAccount(
            id = id,
            name = name,
            type = AccountType.CASH,
        )
    }
}
