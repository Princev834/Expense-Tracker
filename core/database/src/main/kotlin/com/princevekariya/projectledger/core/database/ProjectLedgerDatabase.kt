package com.princevekariya.projectledger.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.princevekariya.projectledger.core.database.dao.AccountDao
import com.princevekariya.projectledger.core.database.dao.BudgetDao
import com.princevekariya.projectledger.core.database.dao.CategoryDao
import com.princevekariya.projectledger.core.database.dao.MerchantDao
import com.princevekariya.projectledger.core.database.dao.TransactionDao
import com.princevekariya.projectledger.core.database.entity.AccountEntity
import com.princevekariya.projectledger.core.database.entity.BudgetEntity
import com.princevekariya.projectledger.core.database.entity.CategoryEntity
import com.princevekariya.projectledger.core.database.entity.MerchantEntity
import com.princevekariya.projectledger.core.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
    ],
    version = ProjectLedgerDatabase.DATABASE_VERSION,
    exportSchema = true,
)
abstract class ProjectLedgerDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun merchantDao(): MerchantDao

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME: String = "project-ledger.db"
        const val DATABASE_VERSION: Int = 1

        fun create(context: Context): ProjectLedgerDatabase = Room.databaseBuilder(
            context.applicationContext,
            ProjectLedgerDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }
}
