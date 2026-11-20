package com.jetbrains.kmpapp.data

import java.io.File

actual fun meruFiles(): MeruFiles = object : MeruFiles {
    private val file = File(System.getProperty("user.home"), ".meru/meru.json")

    override fun read(): String? = file.takeIf { it.exists() }?.readText()

    override fun write(text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }
}

actual fun platformHasGps(): Boolean = false

actual fun startHardwareRide() = Unit

actual fun stopHardwareRide() = Unit
