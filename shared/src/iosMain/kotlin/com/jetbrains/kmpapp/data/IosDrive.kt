package com.jetbrains.kmpapp.data

private var memory: String? = null

actual fun meruFiles(): MeruFiles = object : MeruFiles {
    override fun read(): String? = memory
    override fun write(text: String) {
        memory = text
    }
}

actual fun platformHasGps(): Boolean = false

actual fun startHardwareRide() = Unit

actual fun stopHardwareRide() = Unit
