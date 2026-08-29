package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String,
    val paymentMethod: String,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val nextDueDate: Long,
    val isActive: Boolean = true,
    val lastExecutedDate: Long? = null
)

@Entity(tableName = "splits_udaari")
data class SplitUdaari(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val personName: String,
    val phoneNumber: String? = null,
    val amount: Double,
    val type: String, // "YOU_LENT" (Udaari Given / They owe you) or "YOU_BORROWED" (You owe them)
    val description: String,
    val isSettled: Boolean = false,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val settledAt: Long? = null
)
