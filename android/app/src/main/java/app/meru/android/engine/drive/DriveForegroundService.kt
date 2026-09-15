package app.meru.android.engine.drive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.meru.android.MainActivity
import app.meru.android.R
import app.meru.android.engine.location.FusedLocationClient
import app.meru.android.engine.sync.TripSyncWorker
import app.meru.android.engine.trip.TripProcessor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DriveForegroundService : Service() {

    @Inject lateinit var session: DriveSessionController
    @Inject lateinit var locationClient: FusedLocationClient
    @Inject lateinit var tripProcessor: TripProcessor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var locationJob: Job? = null
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    val completed = session.endDrive()
                    if (completed != null && completed.status == "completed") {
                        runCatching {
                            tripProcessor.process(completed.id)
                            TripSyncWorker.enqueue(applicationContext)
                        }
                    }
                    stopSelfSafe()
                }
                return START_NOT_STICKY
            }
            else -> startTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        ensureChannel()
        val notification = buildNotification("Drive in progress")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )

        if (locationJob?.isActive == true) return

        locationJob = scope.launch {
            locationClient.locationUpdates().collect { sample ->
                session.onGpsSample(sample)
                val snap = session.telemetry.value
                updateNotification(
                    "Meru · ${snap.speedKmh.toInt()} km/h · ${"%.1f".format(snap.distanceM / 1000.0)} km",
                )
            }
        }
        tickJob = scope.launch {
            while (isActive) {
                delay(1_000)
                session.publishTick()
            }
        }
    }

    private fun stopSelfSafe() {
        locationJob?.cancel()
        tickJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        locationJob?.cancel()
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meru Drive",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing drive telemetry"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, DriveForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meru")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(open)
            .addAction(0, "End drive", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(content))
    }

    companion object {
        const val CHANNEL_ID = "meru_drive"
        const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "app.meru.android.action.STOP_DRIVE"

        fun start(context: Context) {
            val intent = Intent(context, DriveForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DriveForegroundService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
