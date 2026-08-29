package com.example.analytics

import com.example.data.model.Transaction
import java.util.Calendar

data class CategoryInsight(
    val category: String,
    val totalAmount: Double,
    val percentageOfTotal: Float,
    val transactionCount: Int,
    val averagePerTransaction: Double
)

data class MonthlyTrend(
    val monthName: String,
    val totalExpense: Double,
    val totalIncome: Double,
    val netSavings: Double
)

object AnalyticsEngine {

    fun computeCategoryInsights(transactions: List<Transaction>): List<CategoryInsight> {
        val expenses = transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
        val totalExpenseSum = expenses.sumOf { it.amount }

        if (totalExpenseSum == 0.0) return emptyList()

        return expenses.groupBy { it.category }
            .map { (category, txList) ->
                val sum = txList.sumOf { it.amount }
                CategoryInsight(
                    category = category,
                    totalAmount = sum,
                    percentageOfTotal = (sum / totalExpenseSum).toFloat(),
                    transactionCount = txList.size,
                    averagePerTransaction = sum / txList.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    fun computeMonthlyTrends(transactions: List<Transaction>): List<MonthlyTrend> {
        val calendar = Calendar.getInstance()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        return transactions.groupBy { tx ->
            calendar.timeInMillis = tx.timestamp
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            "$year-${month.toString().padStart(2, '0')}"
        }
            .toSortedMap()
            .map { (yearMonth, txList) ->
                val monthIndex = yearMonth.substringAfter("-").toInt()
                val monthName = "${monthNames[monthIndex]} ${yearMonth.substringBefore("-")}"
                val expense = txList.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
                val income = txList.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
                MonthlyTrend(
                    monthName = monthName,
                    totalExpense = expense,
                    totalIncome = income,
                    netSavings = income - expense
                )
            }
    }
}
