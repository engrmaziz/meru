package com.jetbrains.kmpapp.screens

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun meruAllowsMotion(): Boolean {
    val scale = Settings.Global.getFloat(
        LocalContext.current.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale != 0f
}
