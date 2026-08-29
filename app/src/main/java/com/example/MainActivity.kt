package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.security.AppLockScreen
import com.example.security.BiometricAuthManager
import com.example.ui.navigation.MainExpenseApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.ExpenseViewModelFactory

class MainActivity : FragmentActivity() {

    private var isAuthenticated = mutableStateOf(false)
    private var authErrorMessage = mutableStateOf<String?>(null)
    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = ExpenseRepository(
            database.transactionDao(),
            database.userProfileDao(),
            database.advancedDao()
        )
        viewModel = ViewModelProvider(this, ExpenseViewModelFactory(repository))[ExpenseViewModel::class.java]

        // Schedule recurring transaction background worker
        com.example.worker.WorkManagerScheduler.scheduleRecurringTransactionWorker(this)

        checkAndAuthenticate()

        setContent {
            MyApplicationTheme {
                val authenticated by remember { isAuthenticated }
                val errorMessage by remember { authErrorMessage }

                if (!authenticated) {
                    AppLockScreen(
                        onUnlockClick = { checkAndAuthenticate() },
                        errorMessage = errorMessage
                    )
                } else {
                    MainExpenseApp(
                        viewModel = viewModel,
                        onLockApp = {
                            isAuthenticated.value = false
                            checkAndAuthenticate()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthenticated.value) {
            checkAndAuthenticate()
        }
    }

    private fun checkAndAuthenticate() {
        when (BiometricAuthManager.canAuthenticate(this)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricAuthManager.promptBiometricAuth(
                    activity = this,
                    title = "Unlock Expense Manager",
                    subtitle = "Verify your identity using Biometric, PIN, or Pattern",
                    onSuccess = {
                        isAuthenticated.value = true
                        authErrorMessage.value = null
                    },
                    onError = { errorCode, errString ->
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            authErrorMessage.value = errString.toString()
                        }
                    },
                    onFailed = {
                        authErrorMessage.value = "Authentication failed. Please try again."
                    }
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                isAuthenticated.value = true
            }
            else -> {
                isAuthenticated.value = true
            }
        }
    }
}
