package com.jetbrains.kmpapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun RideControls(running: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val location = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (location) onStart()
    }
    MeruButton(if (running) "Complete the drive" else "Start drive") {
        if (running) {
            onStop()
        } else {
            val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fine) {
                onStart()
            } else {
                val permissions = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
                if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
                launcher.launch(permissions.toTypedArray())
            }
        }
    }
}
