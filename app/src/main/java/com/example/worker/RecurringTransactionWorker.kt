package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.model.RecurringTransaction
import com.example.data.model.Transaction
import java.util.Calendar

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val advancedDao = database.advancedDao()
        val transactionDao = database.transactionDao()

        val currentTime = System.currentTimeMillis()
        val dueRecurringList = advancedDao.getDueRecurringTransactions(currentTime)

        for (recurring in dueRecurringList) {
            // 1. Insert generated transaction into main ledger
            val transaction = Transaction(
                title = recurring.title,
                amount = recurring.amount,
                type = recurring.type,
                category = recurring.category,
                paymentMethod = recurring.paymentMethod,
                currency = "INR",
                timestamp = System.currentTimeMillis()
            )
            transactionDao.insertTransaction(transaction)

            // 2. Compute next due date based on frequency
            val nextDue = calculateNextDueDate(recurring.nextDueDate, recurring.frequency)

            // 3. Update the recurring rule in DB
            val updatedRecurring = recurring.copy(
                nextDueDate = nextDue,
                lastExecutedDate = currentTime
            )
            advancedDao.updateRecurringTransaction(updatedRecurring)
        }

        return Result.success()
    }

    private fun calculateNextDueDate(currentDue: Long, frequency: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDue
        }
        when (frequency.uppercase()) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
