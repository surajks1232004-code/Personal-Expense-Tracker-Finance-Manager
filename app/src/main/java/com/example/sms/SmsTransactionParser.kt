package com.example.sms

import com.example.data.model.Transaction
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsResult(
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String,
    val paymentMethod: String,
    val currency: String = "INR",
    val timestamp: Long = System.currentTimeMillis()
)

object SmsTransactionParser {

    // Filter keywords for non-transactional / security messages
    private val OTP_SPAM_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|one time password|verification code|verification pin|secret code|auth code|login code|do not share|valid for|expires in)\\b"),
        Pattern.compile("(?i)\\b(congratulations|claim your|won a prize|apply for loan|pre-approved loan|credit limit increase|offer valid till)\\b"),
        Pattern.compile("(?i)\\b(password reset|reset your password|kyc expired|kyc update|block your card)\\b")
    )

    // Regex for amounts (supports INR, Rs, Rs., INR., ₹) with optional commas
    // e.g., "Rs. 1,500.00", "INR 450", "Rs 120.50", "₹5,000"
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?i)(?:rs\\.?|inr|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)"
    )

    // Regex for Transaction Types
    private val DEBIT_PATTERN = Pattern.compile(
        "(?i)\\b(debited|spent|paid|sent|withdrawn|deducted|txn of|transferred to|purchase of|charged)\\b"
    )
    private val CREDIT_PATTERN = Pattern.compile(
        "(?i)\\b(credited|received|deposited|refunded|cashback of|added to|salary)\\b"
    )

    // Bank identification patterns from sender address or message body
    private val SENDER_HDFC_PATTERN = Pattern.compile("(?i)(HDFCBK|HDFC|HDFCBANK)")
    private val SENDER_SBI_PATTERN = Pattern.compile("(?i)(SBINB|SBIPSG|SBIUPI|SBIINB|SBICRD|STATE BANK)")
    private val SENDER_PAYTM_PATTERN = Pattern.compile("(?i)(PAYTM|PYTM|PAYTMB)")

    fun isSpamOrOtp(body: String): Boolean {
        for (pattern in OTP_SPAM_PATTERNS) {
            if (pattern.matcher(body).find()) {
                return true
            }
        }
        return false
    }

    fun parse(sender: String, messageBody: String): ParsedSmsResult? {
        if (messageBody.isBlank()) return null
        if (isSpamOrOtp(messageBody)) return null

        val isDebit = DEBIT_PATTERN.matcher(messageBody).find()
        val isCredit = CREDIT_PATTERN.matcher(messageBody).find()

        // Must match either debit or credit pattern
        if (!isDebit && !isCredit) {
            return null
        }

        // Extract amount
        val amountMatcher = AMOUNT_PATTERN.matcher(messageBody)
        if (!amountMatcher.find()) {
            return null
        }

        val rawAmountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val amount = rawAmountStr.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val bankName = identifyBank(sender, messageBody)
        val transactionType = if (isDebit) "EXPENSE" else "INCOME"
        val paymentMethod = identifyPaymentMethod(messageBody, bankName)
        val category = inferCategory(messageBody, isDebit)
        val title = generateTitle(bankName, messageBody, transactionType)

        return ParsedSmsResult(
            title = title,
            amount = amount,
            type = transactionType,
            category = category,
            paymentMethod = paymentMethod,
            currency = "INR",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun identifyBank(sender: String, body: String): String {
        val combined = "$sender $body"
        return when {
            SENDER_HDFC_PATTERN.matcher(combined).find() -> "HDFC Bank"
            SENDER_SBI_PATTERN.matcher(combined).find() -> "State Bank of India"
            SENDER_PAYTM_PATTERN.matcher(combined).find() -> "Paytm"
            combined.contains("ICICI", ignoreCase = true) -> "ICICI Bank"
            combined.contains("AXIS", ignoreCase = true) -> "Axis Bank"
            combined.contains("KOTAK", ignoreCase = true) -> "Kotak Bank"
            else -> "Bank Alert"
        }
    }

    private fun identifyPaymentMethod(body: String, bankName: String): String {
        val lower = body.lowercase(Locale.getDefault())
        return when {
            lower.contains("upi") || lower.contains("vpa") -> "UPI"
            lower.contains("credit card") || lower.contains("card ending") -> "Credit Card"
            lower.contains("debit card") -> "Debit Card"
            lower.contains("atm") -> "ATM Withdrawal"
            lower.contains("wallet") -> "Wallet"
            lower.contains("netbanking") || lower.contains("neft") || lower.contains("imps") || lower.contains("rtgs") -> "Net Banking"
            else -> bankName
        }
    }

    private fun inferCategory(body: String, isDebit: Boolean): String {
        if (!isDebit) return "Income / Refund"
        val lower = body.lowercase(Locale.getDefault())
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("restaurant") ||
                    lower.contains("cafe") || lower.contains("food") || lower.contains("dine") -> "Food & Dining"
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                    lower.contains("fuel") || lower.contains("petrol") || lower.contains("metro") -> "Transportation"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
                    lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") ||
                    lower.contains("groceries") || lower.contains("mart") -> "Shopping & Groceries"
            lower.contains("electricity") || lower.contains("bill") || lower.contains("recharge") ||
                    lower.contains("airtel") || lower.contains("jio") || lower.contains("broadband") ||
                    lower.contains("dth") || lower.contains("utility") -> "Bills & Utilities"
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("prime") ||
                    lower.contains("movie") || lower.contains("bookmyshow") -> "Entertainment"
            lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("chemist") ||
                    lower.contains("clinic") || lower.contains("apollo") -> "Healthcare"
            else -> "General Expense"
        }
    }

    private fun generateTitle(bankName: String, body: String, type: String): String {
        // Look for merchant/beneficiary after "at", "to", "info"
        val merchantPattern = Pattern.compile("(?i)(?:at|to|info)\\s+([A-Za-z0-9*&._ -]{3,24})")
        val matcher = merchantPattern.matcher(body)
        if (matcher.find()) {
            val merchant = matcher.group(1)?.trim()
            if (!merchant.isNullOrEmpty() && !merchant.equals("your", ignoreCase = true)) {
                return merchant.split(" ").take(3).joinToString(" ")
            }
        }
        return if (type == "EXPENSE") "$bankName Debit" else "$bankName Credit"
    }

    fun ParsedSmsResult.toEntity(): Transaction {
        return Transaction(
            title = this.title,
            amount = this.amount,
            type = this.type,
            category = this.category,
            paymentMethod = this.paymentMethod,
            currency = this.currency,
            timestamp = this.timestamp
        )
    }
}
