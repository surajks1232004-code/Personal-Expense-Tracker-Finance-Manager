package com.example.data.repository

import com.example.data.dao.AdvancedDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserProfileDao
import com.example.data.model.RecurringTransaction
import com.example.data.model.SplitUdaari
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val userProfileDao: UserProfileDao,
    private val advancedDao: AdvancedDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()

    // Recurring Transactions
    val allRecurringTransactions: Flow<List<RecurringTransaction>> = advancedDao.getAllRecurringTransactions()

    suspend fun insertRecurringTransaction(item: RecurringTransaction) =
        advancedDao.insertRecurringTransaction(item)

    suspend fun updateRecurringTransaction(item: RecurringTransaction) =
        advancedDao.updateRecurringTransaction(item)

    suspend fun deleteRecurringTransaction(item: RecurringTransaction) =
        advancedDao.deleteRecurringTransaction(item)

    // Splits & Udaari
    val allSplits: Flow<List<SplitUdaari>> = advancedDao.getAllSplits()
    val totalYouWillGet: Flow<Double?> = advancedDao.getTotalYouWillGet()
    val totalYouOwe: Flow<Double?> = advancedDao.getTotalYouOwe()

    suspend fun insertSplit(split: SplitUdaari) =
        advancedDao.insertSplit(split)

    suspend fun updateSplit(split: SplitUdaari) =
        advancedDao.updateSplit(split)

    suspend fun deleteSplit(split: SplitUdaari) =
        advancedDao.deleteSplit(split)

    fun getTransactionsByType(type: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category)

    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBetweenDates(startDate, endDate)

    fun getTotalAmountByType(type: String): Flow<Double?> =
        transactionDao.getTotalAmountByType(type)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun deleteAllTransactions() =
        transactionDao.deleteAllTransactions()

    suspend fun saveUserProfile(profile: UserProfile) =
        userProfileDao.insertOrUpdateProfile(profile)

    suspend fun getUserProfileDirect(): UserProfile? =
        userProfileDao.getUserProfileSync()

    suspend fun deleteUserProfile() =
        userProfileDao.deleteProfile()
}
