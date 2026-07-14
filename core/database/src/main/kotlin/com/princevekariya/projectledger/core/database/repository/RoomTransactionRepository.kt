package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.AccountDao
import com.princevekariya.projectledger.core.database.dao.TransactionDao
import com.princevekariya.projectledger.core.database.mapper.toEntity
import com.princevekariya.projectledger.core.database.mapper.toModel
import com.princevekariya.projectledger.core.database.transaction.DatabaseTransactionRunner
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val transactionRunner: DatabaseTransactionRunner,
) : TransactionRepository {
    override fun observeAll(): Flow<List<LedgerTransaction>> = transactionDao.observeAll().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    override fun observeRecent(limit: Int): Flow<List<LedgerTransaction>> {
        require(limit > 0) {
            "Recent transaction limit must be greater than zero."
        }
        return transactionDao.observeRecent(limit = limit).map { entities ->
            entities.map { entity -> entity.toModel() }
        }
    }

    override suspend fun findById(id: String): LedgerTransaction? {
        require(id.isNotBlank()) {
            "Transaction identifier cannot be blank."
        }
        return transactionDao.findById(id = id)?.toModel()
    }

    override suspend fun save(transaction: LedgerTransaction) {
        transactionDao.upsert(transaction = transaction.toEntity())
    }

    override suspend fun saveWithUpdatedAccount(transaction: LedgerTransaction, updatedAccount: FinancialAccount) {
        require(transaction.type != TransactionType.TRANSFER) {
            "A single-account write cannot persist a transfer."
        }
        require(transaction.accountId == updatedAccount.id) {
            "The updated account must match the transaction account."
        }
        require(
            transaction.amount.currency ==
                updatedAccount.currentBalance.currency,
        ) {
            "The transaction and updated account currencies must match."
        }

        transactionRunner.run {
            accountDao.upsert(account = updatedAccount.toEntity())
            transactionDao.upsert(transaction = transaction.toEntity())
        }
    }

    override suspend fun deleteById(id: String): Boolean {
        require(id.isNotBlank()) {
            "Transaction identifier cannot be blank."
        }
        return transactionDao.deleteById(id = id) > 0
    }
}
