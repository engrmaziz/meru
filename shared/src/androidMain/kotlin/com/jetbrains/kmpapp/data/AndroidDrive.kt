package com.jetbrains.kmpapp.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import java.io.File

object AndroidApp {
    lateinit var context: Context
}

actual fun meruFiles(): MeruFiles = object : MeruFiles {
    private val file: File
        get() = File(AndroidApp.context.filesDir, "meru.json")

    override fun read(): String? = file.takeIf { it.exists() }?.readText()

    override fun write(text: String) {
        file.writeText(text)
    }
}

actual fun platformHasGps(): Boolean = true

actual fun startHardwareRide() {
    val context = AndroidApp.context
    val intent = Intent(context, MeruDriveService::class.java)
    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
}

actual fun stopHardwareRide() {
    AndroidApp.context.stopService(Intent(AndroidApp.context, MeruDriveService::class.java))
}

class MeruDriveService : Service() {
    private var manager: LocationManager? = null
    private var sensors: SensorManager? = null
    private var longG = 0f
    private var latG = 0f
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            LiveDriveBus.emit(
                RideFix(
                    t = location.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    lat = location.latitude,
                    lon = location.longitude,
                    alt = if (location.hasAltitude()) location.altitude else null,
                    speedMps = if (location.hasSpeed()) location.speed else -1f,
                    bearing = if (location.hasBearing()) location.bearing else -1f,
                    longG = longG,
                    latG = latG,
                ),
            )
        }
    }
    private val motion = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            latG = event.values.getOrElse(0) { 0f } / 9.81f
            longG = -event.values.getOrElse(1) { 0f } / 9.81f
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            promote()
            listen()
        } catch (_: SecurityException) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        manager?.removeUpdates(listener)
        sensors?.unregisterListener(motion)
        super.onDestroy()
    }

    private fun promote() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("meru-drive", "Drive", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val built = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "meru-drive")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setContentTitle("Meru is recording")
            .setContentText("The drive keeps going")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(7, built, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(7, built)
        }
    }

    private fun listen() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }
        manager = getSystemService(LocationManager::class.java)
        val gps = manager
        if (gps != null) {
            runCatching {
                if (gps.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener, Looper.getMainLooper())
                }
                if (gps.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    gps.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5f, listener, Looper.getMainLooper())
                }
            }
        }
        sensors = getSystemService(SensorManager::class.java)
        sensors?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
            sensors?.registerListener(motion, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
}
