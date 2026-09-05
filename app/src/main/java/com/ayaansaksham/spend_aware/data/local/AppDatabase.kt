package com.ayaansaksham.spend_aware.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        Category::class,
        Expense::class,
        Budget::class,
        SavingsGoal::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spend_aware_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}