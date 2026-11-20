package com.jetbrains.kmpapp.screens

import androidx.compose.runtime.Composable

@Composable
actual fun RideControls(running: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    MeruButton(if (running) "Complete the drive" else "Start drive") {
        if (running) onStop() else onStart()
    }
}
