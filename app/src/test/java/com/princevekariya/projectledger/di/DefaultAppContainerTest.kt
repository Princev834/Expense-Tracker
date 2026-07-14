package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.NoOpAppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories
import com.princevekariya.projectledger.domain.transactions.bootstrap.EnsureDefaultLedgerDataUseCase
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.BudgetRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository
import java.lang.reflect.Proxy
import org.junit.Assert.assertSame
import org.junit.Test

class DefaultAppContainerTest {
    @Test
    fun containerReturnsEveryDependencyProvidedByTheCompositionRoot() {
        val accounts = unusedProxy<AccountRepository>()
        val categories = unusedProxy<CategoryRepository>()
        val repositories = LedgerRepositories(
            accounts = accounts,
            budgets = unusedProxy<BudgetRepository>(),
            categories = categories,
            merchants = unusedProxy<MerchantRepository>(),
            transactions = unusedProxy<TransactionRepository>(),
        )
        val initializer = EnsureDefaultLedgerDataUseCase(
            accountRepository = accounts,
            categoryRepository = categories,
        )
        val container = DefaultAppContainer(
            appLogger = NoOpAppLogger,
            repositories = repositories,
            ensureDefaultLedgerData = initializer,
        )

        assertSame(NoOpAppLogger, container.appLogger)
        assertSame(repositories, container.repositories)
        assertSame(initializer, container.ensureDefaultLedgerData)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> unusedProxy(): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, _ ->
        error("Unexpected repository call: ${method.name}")
    } as T
}
