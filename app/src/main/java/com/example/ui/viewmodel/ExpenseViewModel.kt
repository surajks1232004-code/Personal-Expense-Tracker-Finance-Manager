package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.analytics.AnalyticsEngine
import com.example.analytics.CategoryInsight
import com.example.analytics.MonthlyTrend
import com.example.data.model.RecurringTransaction
import com.example.data.model.SplitUdaari
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionFilter(
    val searchQuery: String = "",
    val type: String? = null, // "EXPENSE", "INCOME" or null for all
    val category: String? = null,
    val paymentMethod: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Splits & Udaari Flows
    val allSplits: StateFlow<List<SplitUdaari>> = repository.allSplits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalYouWillGet: StateFlow<Double> = repository.totalYouWillGet
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalYouOwe: StateFlow<Double> = repository.totalYouOwe
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Recurring Transactions Flow
    val allRecurring: StateFlow<List<RecurringTransaction>> = repository.allRecurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Analytics
    val categoryInsights: StateFlow<List<CategoryInsight>> = allTransactions
        .map { AnalyticsEngine.computeCategoryInsights(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTrends: StateFlow<List<MonthlyTrend>> = allTransactions
        .map { AnalyticsEngine.computeMonthlyTrends(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _filter
    ) { transactions, filter ->
        transactions.filter { tx ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                    tx.title.contains(filter.searchQuery, ignoreCase = true) ||
                    tx.category.contains(filter.searchQuery, ignoreCase = true) ||
                    tx.paymentMethod.contains(filter.searchQuery, ignoreCase = true)

            val matchesType = filter.type == null || tx.type.equals(filter.type, ignoreCase = true)
            val matchesCategory = filter.category == null || tx.category.equals(filter.category, ignoreCase = true)
            val matchesPaymentMethod = filter.paymentMethod == null || tx.paymentMethod.equals(filter.paymentMethod, ignoreCase = true)

            val matchesMinAmount = filter.minAmount == null || tx.amount >= filter.minAmount
            val matchesMaxAmount = filter.maxAmount == null || tx.amount <= filter.maxAmount

            matchesQuery && matchesType && matchesCategory && matchesPaymentMethod && matchesMinAmount && matchesMaxAmount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenses: StateFlow<Double> = allTransactions.combine(_filter) { list, _ ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = allTransactions.combine(_filter) { list, _ ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun updateFilter(newFilter: TransactionFilter) {
        _filter.value = newFilter
    }

    fun clearFilters() {
        _filter.value = TransactionFilter()
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        paymentMethod: String,
        currency: String = "INR"
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category,
                paymentMethod = paymentMethod,
                currency = currency,
                timestamp = System.currentTimeMillis()
            )
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Split / Udaari Management
    fun addSplit(
        personName: String,
        phoneNumber: String?,
        amount: Double,
        type: String, // "YOU_LENT" or "YOU_BORROWED"
        description: String,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val split = SplitUdaari(
                personName = personName.trim(),
                phoneNumber = phoneNumber?.trim(),
                amount = amount,
                type = type,
                description = description.trim(),
                dueDate = dueDate
            )
            repository.insertSplit(split)
        }
    }

    fun settleSplit(split: SplitUdaari) {
        viewModelScope.launch {
            val updated = split.copy(
                isSettled = true,
                settledAt = System.currentTimeMillis()
            )
            repository.updateSplit(updated)
        }
    }

    fun deleteSplit(split: SplitUdaari) {
        viewModelScope.launch {
            repository.deleteSplit(split)
        }
    }

    // Recurring Transaction Management
    fun addRecurringTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        paymentMethod: String,
        frequency: String,
        firstDueDate: Long
    ) {
        viewModelScope.launch {
            val recurring = RecurringTransaction(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category,
                paymentMethod = paymentMethod,
                frequency = frequency,
                nextDueDate = firstDueDate
            )
            repository.insertRecurringTransaction(recurring)
        }
    }

    fun deleteRecurringTransaction(item: RecurringTransaction) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(item)
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
        }
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
