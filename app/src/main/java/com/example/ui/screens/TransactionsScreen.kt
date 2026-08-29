package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.TransactionItemCard
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.TransactionFilter

@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()

    var showFilterPanel by remember { mutableStateOf(false) }

    val paymentMethods = listOf("All", "UPI", "Credit Card", "Debit Card", "Cash", "Net Banking")
    val types = listOf("All", "EXPENSE", "INCOME")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar with Filter Toggle
        OutlinedTextField(
            value = currentFilter.searchQuery,
            onValueChange = { newQuery ->
                viewModel.updateFilter(currentFilter.copy(searchQuery = newQuery))
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search merchant, category, or note...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentFilter.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateFilter(currentFilter.copy(searchQuery = "")) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Options",
                            tint = if (showFilterPanel || hasActiveFilters(currentFilter)) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Expandable Filter Panel
        AnimatedVisibility(
            visible = showFilterPanel,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Transactions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { viewModel.clearFilters() }) {
                            Text("Reset")
                        }
                    }

                    // Transaction Type Chips
                    Text("Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { typeOption ->
                            val isSelected = if (typeOption == "All") currentFilter.type == null
                            else currentFilter.type.equals(typeOption, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newType = if (typeOption == "All") null else typeOption
                                    viewModel.updateFilter(currentFilter.copy(type = newType))
                                },
                                label = { Text(if (typeOption == "All") "All Types" else typeOption.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    // Payment Method Chips
                    Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentMethods.forEach { method ->
                            val isSelected = if (method == "All") currentFilter.paymentMethod == null
                            else currentFilter.paymentMethod.equals(method, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newMethod = if (method == "All") null else method
                                    viewModel.updateFilter(currentFilter.copy(paymentMethod = newMethod))
                                },
                                label = { Text(method) }
                            )
                        }
                    }

                    // Amount Range Fields (Min & Max)
                    Text("Amount Range (₹)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentFilter.minAmount?.toString() ?: "",
                            onValueChange = {
                                val min = it.toDoubleOrNull()
                                viewModel.updateFilter(currentFilter.copy(minAmount = min))
                            },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = currentFilter.maxAmount?.toString() ?: "",
                            onValueChange = {
                                val max = it.toDoubleOrNull()
                                viewModel.updateFilter(currentFilter.copy(maxAmount = max))
                            },
                            label = { Text("Max") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Results Header
        Text(
            text = "${filteredTransactions.size} Transaction${if (filteredTransactions.size == 1) "" else "s"}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching transactions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try adjusting your filters or search keywords",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onDelete = { viewModel.deleteTransaction(it) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(90.dp)) // Nav & FAB padding
                }
            }
        }
    }
}

private fun hasActiveFilters(filter: TransactionFilter): Boolean {
    return filter.type != null || filter.paymentMethod != null ||
            filter.category != null || filter.minAmount != null || filter.maxAmount != null
}
