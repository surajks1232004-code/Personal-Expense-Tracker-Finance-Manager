package com.example.data.dao

import androidx.room.*
import com.example.data.model.RecurringTransaction
import com.example.data.model.SplitUdaari
import kotlinx.coroutines.flow.Flow

@Dao
interface AdvancedDao {

    // Recurring Transactions
    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDate ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :currentTime")
    suspend fun getDueRecurringTransactions(currentTime: Long): List<RecurringTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(item: RecurringTransaction): Long

    @Update
    suspend fun updateRecurringTransaction(item: RecurringTransaction)

    @Delete
    suspend fun deleteRecurringTransaction(item: RecurringTransaction)

    // Splits & Udaari
    @Query("SELECT * FROM splits_udaari ORDER BY isSettled ASC, createdAt DESC")
    fun getAllSplits(): Flow<List<SplitUdaari>>

    @Query("SELECT * FROM splits_udaari WHERE isSettled = 0")
    fun getActiveSplits(): Flow<List<SplitUdaari>>

    @Query("SELECT SUM(amount) FROM splits_udaari WHERE type = 'YOU_LENT' AND isSettled = 0")
    fun getTotalYouWillGet(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM splits_udaari WHERE type = 'YOU_BORROWED' AND isSettled = 0")
    fun getTotalYouOwe(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: SplitUdaari): Long

    @Update
    suspend fun updateSplit(split: SplitUdaari)

    @Delete
    suspend fun deleteSplit(split: SplitUdaari)
}
