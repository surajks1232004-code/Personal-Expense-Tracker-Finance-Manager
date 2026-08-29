package com.example.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupRestoreManager {

    suspend fun createJsonBackup(context: Context): File = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val transactions = database.transactionDao().getAllTransactionsSync()
        val userProfile = database.userProfileDao().getUserProfileSync()

        val rootObject = JSONObject()
        rootObject.put("version", 1)
        rootObject.put("exportTime", System.currentTimeMillis())

        // User Profile JSON
        userProfile?.let {
            val userObj = JSONObject().apply {
                put("name", it.name)
                put("email", it.email)
                put("salary", it.salary)
                put("budget", it.budget)
                put("currency", it.currency)
                put("country", it.country)
                put("language", it.language)
            }
            rootObject.put("userProfile", userObj)
        }

        // Transactions JSON
        val txArray = JSONArray()
        transactions.forEach { tx ->
            val item = JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("amount", tx.amount)
                put("type", tx.type)
                put("category", tx.category)
                put("paymentMethod", tx.paymentMethod)
                put("currency", tx.currency)
                put("timestamp", tx.timestamp)
            }
            txArray.put(item)
        }
        rootObject.put("transactions", txArray)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, "ExpenseManager_Backup_$timeStamp.json")

        backupFile.writeText(rootObject.toString(2))
        backupFile
    }

    suspend fun restoreFromJsonStream(context: Context, inputStream: InputStream): Int = withContext(Dispatchers.IO) {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)
        val database = AppDatabase.getDatabase(context)

        // Restore user profile if present
        if (root.has("userProfile")) {
            val userObj = root.getJSONObject("userProfile")
            val profile = UserProfile(
                id = 1,
                name = userObj.optString("name", ""),
                email = userObj.optString("email", ""),
                salary = userObj.optDouble("salary", 50000.0),
                budget = userObj.optDouble("budget", 25000.0),
                currency = userObj.optString("currency", "INR"),
                country = userObj.optString("country", "India"),
                language = userObj.optString("language", "English")
            )
            database.userProfileDao().insertOrUpdateProfile(profile)
        }

        // Restore transactions
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        val restoredList = mutableListOf<Transaction>()

        for (i in 0 until txArray.length()) {
            val item = txArray.getJSONObject(i)
            restoredList.add(
                Transaction(
                    title = item.getString("title"),
                    amount = item.getDouble("amount"),
                    type = item.getString("type"),
                    category = item.getString("category"),
                    paymentMethod = item.getString("paymentMethod"),
                    currency = item.optString("currency", "INR"),
                    timestamp = item.getLong("timestamp")
                )
            )
        }

        if (restoredList.isNotEmpty()) {
            database.transactionDao().insertAllTransactions(restoredList)
        }

        restoredList.size
    }
}

object StatementGenerator {

    suspend fun generateCsv(
        context: Context,
        transactions: List<Transaction>,
        startDate: Long?,
        endDate: Long?
    ): File = withContext(Dispatchers.IO) {
        val filtered = transactions.filter { tx ->
            (startDate == null || tx.timestamp >= startDate) &&
            (endDate == null || tx.timestamp <= endDate)
        }.sortedByDescending { it.timestamp }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvHeader = "ID,Date,Title,Type,Category,Payment Method,Amount,Currency\n"

        val sb = StringBuilder(csvHeader)
        for (tx in filtered) {
            val dateStr = dateFormatter.format(Date(tx.timestamp))
            val cleanTitle = tx.title.replace(",", " ")
            sb.append("${tx.id},\"$dateStr\",\"$cleanTitle\",${tx.type},\"${tx.category}\",${tx.paymentMethod},${tx.amount},${tx.currency}\n")
        }

        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val csvFile = File(reportsDir, "Statement_$timestamp.csv")
        csvFile.writeText(sb.toString())

        csvFile
    }

    suspend fun generatePdf(
        context: Context,
        transactions: List<Transaction>,
        startDate: Long?,
        endDate: Long?,
        userName: String = "User"
    ): File = withContext(Dispatchers.IO) {
        val filtered = transactions.filter { tx ->
            (startDate == null || tx.timestamp >= startDate) &&
            (endDate == null || tx.timestamp <= endDate)
        }.sortedByDescending { it.timestamp }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        val paintHeader = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isFakeBoldText = true
        }

        val paintBody = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val paintSub = Paint().apply {
            color = Color.GRAY
            textSize = 9f
        }

        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 40f
        canvas.drawText("EXPENSE MANAGER STATEMENT", 40f, y, paintTitle)
        y += 18f

        val dateDisplayFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val dateRangeText = if (startDate != null && endDate != null) {
            "Range: ${dateDisplayFormatter.format(Date(startDate))} to ${dateDisplayFormatter.format(Date(endDate))}"
        } else {
            "Range: Complete Transaction History"
        }

        canvas.drawText("Generated for: $userName | $generatedAt", 40f, y, paintSub)
        y += 14f
        canvas.drawText(dateRangeText, 40f, y, paintSub)
        y += 16f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 20f

        // Table Header
        canvas.drawText("Date", 40f, y, paintHeader)
        canvas.drawText("Description", 130f, y, paintHeader)
        canvas.drawText("Category", 280f, y, paintHeader)
        canvas.drawText("Method", 400f, y, paintHeader)
        canvas.drawText("Amount", 480f, y, paintHeader)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 16f

        val dtFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        var totalExpense = 0.0
        var totalIncome = 0.0

        for (tx in filtered.take(35)) { // First page limit
            if (tx.type.equals("EXPENSE", ignoreCase = true)) totalExpense += tx.amount
            else totalIncome += tx.amount

            canvas.drawText(dtFormat.format(Date(tx.timestamp)), 40f, y, paintBody)
            canvas.drawText(tx.title.take(20), 130f, y, paintBody)
            canvas.drawText(tx.category.take(16), 280f, y, paintBody)
            canvas.drawText(tx.paymentMethod.take(12), 400f, y, paintBody)

            val amountStr = "${if (tx.type == "EXPENSE") "-" else "+"}₹${tx.amount.toInt()}"
            canvas.drawText(amountStr, 480f, y, paintBody)
            y += 18f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 20f
        canvas.drawText("Total Inflow (Income): ₹$totalIncome", 40f, y, paintHeader)
        y += 16f
        canvas.drawText("Total Outflow (Expense): ₹$totalExpense", 40f, y, paintHeader)

        pdfDocument.finishPage(page)

        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val pdfFile = File(reportsDir, "Statement_$timestamp.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        pdfFile
    }
}

object FileSharingUtil {
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Share File") {
        try {
            val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

