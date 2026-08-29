package com.example.security

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

const val SMS_PERMISSION_REQ_CODE = 1001

val SMS_PERMISSIONS = arrayOf(
    Manifest.permission.READ_SMS,
    Manifest.permission.RECEIVE_SMS
)

fun Context.hasSmsPermissions(): Boolean {
    return SMS_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
}

@Composable
fun SmsPermissionHandler(
    onPermissionsResult: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember { mutableStateOf(context.hasSmsPermissions()) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }
    var requestedOnce by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = context.hasSmsPermissions()
                hasPermission = granted
                if (granted) {
                    showRationaleDialog = false
                    showDeniedDialog = false
                    onPermissionsResult(true)
                } else if (requestedOnce) {
                    showDeniedDialog = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!context.hasSmsPermissions()) {
            showRationaleDialog = true
        } else {
            onPermissionsResult(true)
        }
    }

    fun requestPermissionsDirectly() {
        if (activity != null) {
            requestedOnce = true
            ActivityCompat.requestPermissions(activity, SMS_PERMISSIONS, SMS_PERMISSION_REQ_CODE)
        }
    }

    if (showRationaleDialog && !hasPermission) {
        SmsPermissionExplanationDialog(
            onConfirm = {
                showRationaleDialog = false
                requestPermissionsDirectly()
            },
            onDismiss = {
                showRationaleDialog = false
                onPermissionsResult(false)
            }
        )
    }

    if (showDeniedDialog && !hasPermission) {
        val isPermanentlyDenied = activity != null && SMS_PERMISSIONS.any {
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        SmsPermissionDeniedDialog(
            isPermanentlyDenied = isPermanentlyDenied,
            onRetryOrSettings = {
                showDeniedDialog = false
                if (isPermanentlyDenied) {
                    context.openAppSettings()
                } else {
                    requestPermissionsDirectly()
                }
            },
            onDismiss = {
                showDeniedDialog = false
                onPermissionsResult(false)
            }
        )
    }
}

