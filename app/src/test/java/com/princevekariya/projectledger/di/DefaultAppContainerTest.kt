package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.NoOpAppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories
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
    fun containerReturnsTheDependenciesProvidedByTheCompositionRoot() {
        val repositories = LedgerRepositories(
            accounts = unusedProxy<AccountRepository>(),
            budgets = unusedProxy<BudgetRepository>(),
            categories = unusedProxy<CategoryRepository>(),
            merchants = unusedProxy<MerchantRepository>(),
            transactions = unusedProxy<TransactionRepository>(),
        )
        val container = DefaultAppContainer(
            appLogger = NoOpAppLogger,
            repositories = repositories,
        )

        assertSame(NoOpAppLogger, container.appLogger)
        assertSame(repositories, container.repositories)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> unusedProxy(): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, _ ->
        error("Unexpected repository call: ${method.name}")
    } as T
}
