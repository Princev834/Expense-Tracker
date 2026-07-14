package com.princevekariya.projectledger.core.database.transaction

import androidx.room.RoomDatabase
import androidx.room.withTransaction

interface DatabaseTransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

internal class RoomDatabaseTransactionRunner(
    private val database: RoomDatabase,
) : DatabaseTransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction {
        block()
    }
}
