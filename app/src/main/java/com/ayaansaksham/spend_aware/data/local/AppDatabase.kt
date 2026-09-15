package com.ayaansaksham.spend_aware.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayaansaksham.spend_aware.data.local.dao.BudgetDao
import com.ayaansaksham.spend_aware.data.local.dao.CategoryDao
import com.ayaansaksham.spend_aware.data.local.dao.ExpenseDao
import com.ayaansaksham.spend_aware.data.local.dao.SavingsGoalDao
import com.ayaansaksham.spend_aware.data.local.dao.UserSettingsDao
import com.ayaansaksham.spend_aware.data.local.entity.Budget
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import com.ayaansaksham.spend_aware.data.local.entity.SavingsGoal
import com.ayaansaksham.spend_aware.data.local.entity.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Category::class,
        Expense::class,
        Budget::class,
        SavingsGoal::class,
        UserSettings::class,
        com.ayaansaksham.spend_aware.data.local.entity.Profile::class
    ],
    version = 4, // Bumping to 4 for Profiles
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun profileDao(): com.ayaansaksham.spend_aware.data.local.dao.ProfileDao

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
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dbInstance = getDatabase(context)
                                val profileDao = dbInstance.profileDao()
                                val categoryDao = dbInstance.categoryDao()
                                val userSettingsDao = dbInstance.userSettingsDao()

                                // Create UserSettings
                                userSettingsDao.insertOrUpdateSettings(UserSettings(id = 1, firebaseUid = "local_only", activeProfileId = 1))

                                // Create Profiles
                                val p1 = com.ayaansaksham.spend_aware.data.local.entity.Profile(profileId = 1, name = "Ayaan", role = "STUDENT")
                                val p2 = com.ayaansaksham.spend_aware.data.local.entity.Profile(profileId = 2, name = "John Doe", role = "EMPLOYEE")
                                profileDao.insertProfile(p1)
                                profileDao.insertProfile(p2)

                                // Categories for Student (Profile 1)
                                categoryDao.insertCategory(Category(name = "Canteen", colorHex = "#00685D", isDefault = true, profileId = 1))
                                categoryDao.insertCategory(Category(name = "Commute", colorHex = "#F89C00", isDefault = true, profileId = 1))
                                categoryDao.insertCategory(Category(name = "Stationery", colorHex = "#6D7A77", isDefault = true, profileId = 1))
                                categoryDao.insertCategory(Category(name = "Events", colorHex = "#875200", isDefault = true, profileId = 1))

                                // Categories for Employee (Profile 2)
                                categoryDao.insertCategory(Category(name = "Groceries", colorHex = "#16A34A", isDefault = true, profileId = 2))
                                categoryDao.insertCategory(Category(name = "Rent", colorHex = "#B02528", isDefault = true, profileId = 2))
                                categoryDao.insertCategory(Category(name = "Fuel", colorHex = "#F89C00", isDefault = true, profileId = 2))
                                categoryDao.insertCategory(Category(name = "Investments", colorHex = "#008376", isDefault = true, profileId = 2))
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