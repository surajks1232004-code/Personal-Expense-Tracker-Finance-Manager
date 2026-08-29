package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val paymentMethod: String,
    val currency: String,
    val timestamp: Long = System.currentTimeMillis()
)
