package com.jetbrains.kmpapp.screens

import androidx.compose.material3.Button
import androidx.compose.runtime.Composable

@Composable
expect fun RideControls(running: Boolean, onStart: () -> Unit, onStop: () -> Unit)
