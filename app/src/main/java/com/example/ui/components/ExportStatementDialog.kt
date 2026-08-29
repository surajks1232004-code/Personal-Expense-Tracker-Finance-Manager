package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun ExportStatementDialog(
    onDismiss: () -> Unit,
    onExport: (format: String, startDate: Long?, endDate: Long?) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("PDF") } // "PDF" or "CSV"
    var selectedRange by remember { mutableStateOf("LAST_30_DAYS") } // "ALL", "THIS_MONTH", "LAST_30_DAYS", "LAST_90_DAYS"

    val calendar = Calendar.getInstance()
    val now = calendar.timeInMillis

    val (startDate, endDate) = when (selectedRange) {
        "THIS_MONTH" -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            Pair(calendar.timeInMillis, now)
        }
        "LAST_30_DAYS" -> {
            Pair(now - (30L * 24 * 60 * 60 * 1000), now)
        }
        "LAST_90_DAYS" -> {
            Pair(now - (90L * 24 * 60 * 60 * 1000), now)
        }
        else -> Pair(null, null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Statement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Format",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == "PDF",
                        onClick = { selectedFormat = "PDF" },
                        label = { Text("PDF Document") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedFormat == "CSV",
                        onClick = { selectedFormat = "CSV" },
                        label = { Text("Excel / CSV") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Select Date Range",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRange == "LAST_30_DAYS",
                            onClick = { selectedRange = "LAST_30_DAYS" },
                            label = { Text("Last 30 Days") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedRange == "THIS_MONTH",
                            onClick = { selectedRange = "THIS_MONTH" },
                            label = { Text("This Month") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRange == "LAST_90_DAYS",
                            onClick = { selectedRange = "LAST_90_DAYS" },
                            label = { Text("Last 90 Days") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedRange == "ALL",
                            onClick = { selectedRange = "ALL" },
                            label = { Text("All Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedFormat, startDate, endDate) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Export File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
