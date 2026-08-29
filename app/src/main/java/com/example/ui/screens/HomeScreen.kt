package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.ui.components.AddSplitDialog
import com.example.ui.components.BudgetVsExpenseCard
import com.example.ui.components.SplitUdaariCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.viewmodel.ExpenseViewModel

@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val totalExpense by viewModel.totalExpenses.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()

    val allSplits by viewModel.allSplits.collectAsState()
    val totalYouWillGet by viewModel.totalYouWillGet.collectAsState()
    val totalYouOwe by viewModel.totalYouOwe.collectAsState()

    var showAddSplitDialog by remember { mutableStateOf(false) }

    val recentTransactions = allTransactions.take(5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Dashboard Budget vs Expense Card
            BudgetVsExpenseCard(
                budget = userProfile?.budget ?: 25000.0,
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                currencySymbol = "₹"
            )
        }

        item {
            // Split / Udaari Tracker Card
            SplitUdaariCard(
                totalYouWillGet = totalYouWillGet,
                totalYouOwe = totalYouOwe,
                activeSplits = allSplits,
                onAddSplitClick = { showAddSplitDialog = true },
                onSettleClick = { viewModel.settleSplit(it) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (allTransactions.isNotEmpty()) {
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("View All")
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
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
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add manual transactions or auto-log via bank SMS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                TransactionItemCard(
                    transaction = tx,
                    onDelete = { viewModel.deleteTransaction(it) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Padding for Bottom FAB / Nav
        }
    }

    if (showAddSplitDialog) {
        AddSplitDialog(
            onDismiss = { showAddSplitDialog = false },
            onConfirm = { person, phone, amount, type, note ->
                viewModel.addSplit(person, phone, amount, type, note)
                showAddSplitDialog = false
            }
        )
    }
}
