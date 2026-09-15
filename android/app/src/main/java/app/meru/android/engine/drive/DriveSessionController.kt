package app.meru.android.engine.drive

import app.meru.android.core.database.PendingSyncDao
import app.meru.android.core.database.PendingSyncEntity
import app.meru.android.core.database.TripDao
import app.meru.android.core.database.TripEntity
import app.meru.android.core.database.TripLocationEntity
import app.meru.android.engine.telemetry.GpsSample
import app.meru.android.engine.telemetry.LiveTelemetry
import app.meru.android.engine.telemetry.TelemetryAccumulator
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

@Singleton
class DriveSessionController @Inject constructor(
    private val tripDao: TripDao,
    private val pendingSyncDao: PendingSyncDao,
    private val drivingMode: DrivingMode,
) {
    private val mutex = Mutex()
    private val accumulator = TelemetryAccumulator()

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    val isActive: Boolean get() = _telemetry.value.active

    suspend fun startDrive(vehicleId: String? = null): String = mutex.withLock {
        check(!_telemetry.value.active) { "Drive already active" }
        val tripId = UUID.randomUUID().toString()
        val start = System.currentTimeMillis()
        tripDao.upsertTrip(
            TripEntity(
                id = tripId,
                vehicleId = vehicleId,
                startAtMs = start,
                status = "active",
                syncStatus = "pending",
            ),
        )
        accumulator.start(tripId, start)
        drivingMode.setActive(true)
        _telemetry.value = accumulator.snapshot(start)
        tripId
    }

    suspend fun onGpsSample(sample: GpsSample) = mutex.withLock {
        val tripId = _telemetry.value.tripId ?: return@withLock
        val accepted = accumulator.onSample(sample)
        if (accepted) {
            tripDao.insertLocation(
                TripLocationEntity(
                    tripId = tripId,
                    timestampMs = sample.timestampMs,
                    latitude = sample.latitude,
                    longitude = sample.longitude,
                    altitudeM = sample.altitudeM,
                    speedMps = sample.speedMps,
                    bearing = sample.bearing,
                    accuracyM = sample.accuracyM,
                ),
            )
        }
        _telemetry.value = accumulator.snapshot(sample.timestampMs)
    }

    suspend fun endDrive(): TripEntity? = mutex.withLock {
        val snap = accumulator.snapshot()
        val tripId = snap.tripId ?: return@withLock null
        val end = System.currentTimeMillis()
        val completed = TripEntity(
            id = tripId,
            vehicleId = tripDao.getTrip(tripId)?.vehicleId,
            startAtMs = tripDao.getTrip(tripId)?.startAtMs ?: (end - snap.durationMs),
            endAtMs = end,
            distanceM = snap.distanceM,
            durationMs = snap.durationMs,
            avgSpeedKmh = snap.avgSpeedKmh,
            maxSpeedKmh = snap.maxSpeedKmh,
            pointCount = snap.pointCount,
            rejectedJumps = snap.rejectedJumps,
            status = "completed",
            syncStatus = "pending",
        )
        tripDao.upsertTrip(completed)
        pendingSyncDao.upsert(
            PendingSyncEntity(
                id = tripId,
                type = "trip_complete",
                payloadJson = JSONObject()
                    .put("tripId", tripId)
                    .put("distanceM", completed.distanceM)
                    .toString(),
            ),
        )
        accumulator.reset()
        drivingMode.setActive(false)
        _telemetry.value = LiveTelemetry()
        completed
    }

    fun publishTick(nowMs: Long = System.currentTimeMillis()) {
        if (_telemetry.value.active) {
            _telemetry.value = accumulator.snapshot(nowMs)
        }
    }
}
