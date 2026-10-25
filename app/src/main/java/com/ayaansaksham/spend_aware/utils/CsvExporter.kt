package com.ayaansaksham.spend_aware.utils

import android.content.Context
import android.net.Uri
import com.ayaansaksham.spend_aware.data.local.entity.Category
import com.ayaansaksham.spend_aware.data.local.entity.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    suspend fun exportExpensesToCsv(
        context: Context,
        uri: Uri,
        expenses: List<Expense>,
        categories: List<Category>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val categoryMap = categories.associateBy { it.categoryId }
            val sortedExpenses = expenses.sortedByDescending { it.dateMillis }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    // Write BOM for Excel UTF-8 recognition
                    writer.write("\uFEFF")
                    
                    // Write Header
                    writer.write("Date,Category,Amount,Note\n")

                    // Write Rows
                    for (expense in sortedExpenses) {
                        val dateStr = dateFormat.format(Date(expense.dateMillis))
                        val categoryName = expense.categoryId?.let { categoryMap[it]?.name } ?: "Uncategorized"
                        val amountStr = expense.amount.toString()
                        val noteStr = expense.note ?: ""

                        val line = "${escapeCsv(dateStr)},${escapeCsv(categoryName)},${escapeCsv(amountStr)},${escapeCsv(noteStr)}\n"
                        writer.write(line)
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream."))
            
            Result.success(sortedExpenses.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun escapeCsv(value: String): String {
        var escaped = value
        // If the value contains commas, quotes, or newlines, it must be enclosed in quotes.
        // Any internal quotes must be doubled.
        if (escaped.contains("\"") || escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r")) {
            escaped = escaped.replace("\"", "\"\"")
            escaped = "\"$escaped\""
        }
        return escaped
    }
}