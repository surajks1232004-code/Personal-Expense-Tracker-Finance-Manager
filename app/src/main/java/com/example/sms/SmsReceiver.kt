package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.sms.SmsTransactionParser.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val fullBody = StringBuilder()
                var sender = ""

                for (sms in messages) {
                    sms?.let {
                        if (sender.isEmpty()) {
                            sender = it.displayOriginatingAddress ?: ""
                        }
                        fullBody.append(it.displayMessageBody ?: "")
                    }
                }

                val messageContent = fullBody.toString()
                val parsedResult = SmsTransactionParser.parse(sender, messageContent)

                if (parsedResult != null) {
                    val db = AppDatabase.getDatabase(context)
                    val transaction = parsedResult.toEntity()
                    val id = db.transactionDao().insertTransaction(transaction)
                    Log.d(TAG, "Successfully recorded transaction #$id: ${transaction.title} - ${transaction.amount}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing or persisting SMS transaction", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
