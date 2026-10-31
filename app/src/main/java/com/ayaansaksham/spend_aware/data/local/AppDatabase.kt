package com.ayaansaksham.spend_aware.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayaansaksham.spend_aware.data.local.dao.BudgetDao
import com.ayaansaksham.spend_aware.data.local.dao.CategoryDao
import com.ayaansaksham.spend_aware.data.local.dao.ExpenseDao
import com.ayaansaksham.spend_aware.data.local.dao.ProfileDao
import com.ayaansaksham.spend_aware.data.local.dao.SavingsGoalDao
import com.ayaansaksham.spend_aware.data.local.dao.UserDao
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.data.local.entity.Profile
import com.ayaansaksham.spend_aware.data.local.entity.SavingsGoal
import com.ayaansaksham.spend_aware.data.local.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fullName` TEXT NOT NULL, `email` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, `salt` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories_new` (`categoryId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT, `colorHex` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `profileId` INTEGER NOT NULL)"
        )
        db.execSQL("INSERT INTO `categories_new` (`categoryId`, `name`, `icon`, `colorHex`, `isDefault`, `profileId`) SELECT `categoryId`, `name`, `icon`, `colorHex`, `isDefault`, `profileId` FROM `categories`")
        db.execSQL("DROP TABLE `categories`")
        db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name_profileId` ON `categories` (`name`, `profileId`)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add activeProfileId column to existing users table from v7
        db.execSQL("ALTER TABLE `users` ADD COLUMN `activeProfileId` INTEGER NOT NULL DEFAULT 1")

        // 2. Ensure baseline User #1 exists for any pre-migration profiles/expenses
        db.execSQL(
            "INSERT OR IGNORE INTO `users` (`id`, `fullName`, `email`, `passwordHash`, `salt`, `activeProfileId`, `createdAt`) " +
            "VALUES (1, 'Ayaan', 'default@spendaware.local', '', '', 1, 1700000000000)"
        )

        // 3. Recreate profiles table with userId and FK
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `profiles_new` (" +
            "`profileId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT NOT NULL, " +
            "`role` TEXT NOT NULL, " +
            "`userId` INTEGER NOT NULL DEFAULT 1, " +
            "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE)"
        )
        db.execSQL("INSERT INTO `profiles_new` (`profileId`, `name`, `role`, `userId`) SELECT `profileId`, `name`, `role`, 1 FROM `profiles`")
        db.execSQL("DROP TABLE `profiles`")
        db.execSQL("ALTER TABLE `profiles_new` RENAME TO `profiles`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_profiles_userId` ON `profiles` (`userId`)")

        // 4. Recreate expenses table without isRecurring
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expenses_new` (" +
            "`expenseId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`amount` REAL NOT NULL, " +
            "`categoryId` INTEGER, " +
            "`note` TEXT, " +
            "`dateMillis` INTEGER NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, " +
            "`profileId` INTEGER NOT NULL DEFAULT 1, " +
            "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`categoryId`) ON DELETE SET_NULL)"
        )
        db.execSQL("INSERT INTO `expenses_new` (`expenseId`, `amount`, `categoryId`, `note`, `dateMillis`, `createdAt`, `profileId`) SELECT `expenseId`, `amount`, `categoryId`, `note`, `dateMillis`, `createdAt`, `profileId` FROM `expenses`")
        db.execSQL("DROP TABLE `expenses`")
        db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")

        // 5. Drop user_settings table
        db.execSQL("DROP TABLE IF EXISTS `user_settings`")
    }
}

@Database(
    entities = [
        Category::class,
        Expense::class,
        Budget::class,
        SavingsGoal::class,
        Profile::class,
        User::class
    ],
    version = 9, // Bumped to 9, multi-user schema update
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun profileDao(): ProfileDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spend_aware_database"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9) // Explicit migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}