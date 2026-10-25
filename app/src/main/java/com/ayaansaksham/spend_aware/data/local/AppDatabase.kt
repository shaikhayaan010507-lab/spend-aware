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
import com.ayaansaksham.spend_aware.data.local.dao.SavingsGoalDao
import com.ayaansaksham.spend_aware.data.local.dao.UserSettingsDao
import com.ayaansaksham.spend_aware.data.local.dao.UserDao
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.data.local.entity.SavingsGoal
import com.ayaansaksham.spend_aware.data.local.entity.User
import com.ayaansaksham.spend_aware.data.local.entity.UserSettings
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

@Database(
    entities = [
        Category::class,
        Expense::class,
        Budget::class,
        SavingsGoal::class,
        UserSettings::class,
        com.ayaansaksham.spend_aware.data.local.entity.Profile::class,
        User::class
    ],
    version = 8, // Bumped to 8, added categories migration
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun profileDao(): com.ayaansaksham.spend_aware.data.local.dao.ProfileDao
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
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8) // Explicit migrations
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dbInstance = getDatabase(context)
                                val profileDao = dbInstance.profileDao()
                                val categoryDao = dbInstance.categoryDao()
                                val userSettingsDao = dbInstance.userSettingsDao()
                                val budgetDao = dbInstance.budgetDao()

                                // Create UserSettings (Defaults to Profile 1)
                                userSettingsDao.insertOrUpdateSettings(UserSettings(id = 1, firebaseUid = "local_only", activeProfileId = 1))

                                // Create Profiles
                                val p1 = com.ayaansaksham.spend_aware.data.local.entity.Profile(profileId = 1, name = "Ayaan", role = "STUDENT")
                                val p2 = com.ayaansaksham.spend_aware.data.local.entity.Profile(profileId = 2, name = "John Doe", role = "EMPLOYEE")
                                profileDao.insertProfile(p1)
                                profileDao.insertProfile(p2)

                                // --- CATEGORIES ---
                                // Categories for Student (Profile 1)
                                categoryDao.insertCategory(Category(categoryId = 1, name = "Canteen", colorHex = "#00685D", isDefault = true, profileId = 1))
                                categoryDao.insertCategory(Category(categoryId = 2, name = "Commute", colorHex = "#F89C00", isDefault = true, profileId = 1))
                                categoryDao.insertCategory(Category(categoryId = 3, name = "Stationery", colorHex = "#6D7A77", isDefault = true, profileId = 1))
                                categoryDao.insertCategory(Category(categoryId = 4, name = "Events", colorHex = "#875200", isDefault = true, profileId = 1))
                                categoryDao.insertCategory(Category(categoryId = 5, name = "Hostel Fees", colorHex = "#B02528", isDefault = true, profileId = 1))

                                // Categories for Employee (Profile 2)
                                categoryDao.insertCategory(Category(categoryId = 6, name = "Groceries", colorHex = "#16A34A", isDefault = true, profileId = 2))
                                categoryDao.insertCategory(Category(categoryId = 7, name = "Rent", colorHex = "#B02528", isDefault = true, profileId = 2))
                                categoryDao.insertCategory(Category(categoryId = 8, name = "Fuel", colorHex = "#F89C00", isDefault = true, profileId = 2))
                                categoryDao.insertCategory(Category(categoryId = 9, name = "Investments", colorHex = "#008376", isDefault = true, profileId = 2))
                                categoryDao.insertCategory(Category(categoryId = 10, name = "Dining", colorHex = "#00685D", isDefault = true, profileId = 2))

                                // --- HELPER DATES ---
                                val startOfMonth = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis

                                // ==========================================
                                // INJECT EMPTY BASELINE FOR PROFILE 1
                                // ==========================================

                                // 1. Master Budget (Allowance) - Initialize to 0 so the UI isn't broken
                                budgetDao.insertBudget(Budget(limitAmount = 0.0, periodType = "MONTHLY", startDateMillis = startOfMonth, categoryId = null, profileId = 1))
                                
                                // ==========================================
                                // INJECT EMPTY BASELINE FOR PROFILE 2
                                // ==========================================

                                // 1. Master Budget (Salary) - Initialize to 0
                                budgetDao.insertBudget(Budget(limitAmount = 0.0, periodType = "MONTHLY", startDateMillis = startOfMonth, categoryId = null, profileId = 2))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}