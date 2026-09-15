package app.meru.android.engine.telemetry

data class LiveTelemetry(
    val tripId: String? = null,
    val active: Boolean = false,
    val speedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val distanceM: Double = 0.0,
    val durationMs: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pointCount: Int = 0,
    val rejectedJumps: Int = 0,
    val gpsWeak: Boolean = false,
)

class TelemetryAccumulator(
    private val jumpFilter: JumpFilter = JumpFilter(),
) {
    private var tripId: String? = null
    private var startedAtMs: Long = 0L
    private var lastAccepted: GpsSample? = null
    private var distanceM: Double = 0.0
    private var maxSpeedKmh: Double = 0.0
    private var speedSumKmh: Double = 0.0
    private var speedSamples: Int = 0
    private var pointCount: Int = 0
    private var rejectedJumps: Int = 0
    private var latest: GpsSample? = null

    fun start(tripId: String, startedAtMs: Long) {
        reset()
        this.tripId = tripId
        this.startedAtMs = startedAtMs
    }

    fun reset() {
        tripId = null
        startedAtMs = 0L
        lastAccepted = null
        distanceM = 0.0
        maxSpeedKmh = 0.0
        speedSumKmh = 0.0
        speedSamples = 0
        pointCount = 0
        rejectedJumps = 0
        latest = null
    }

    /** @return true if sample was accepted into the distance chain */
    fun onSample(sample: GpsSample): Boolean {
        latest = sample
        if (!jumpFilter.accept(lastAccepted, sample)) {
            rejectedJumps++
            return false
        }
        lastAccepted?.let { prev ->
            distanceM += GeoMath.haversineMeters(
                prev.latitude,
                prev.longitude,
                sample.latitude,
                sample.longitude,
            )
        }
        lastAccepted = sample
        pointCount++

        val speedKmh = when {
            sample.speedMps != null && sample.speedMps >= 0 ->
                GeoMath.metersPerSecondToKmh(sample.speedMps)
            else -> 0.0
        }
        if (speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh
        speedSumKmh += speedKmh
        speedSamples++
        return true
    }

    fun snapshot(nowMs: Long = System.currentTimeMillis()): LiveTelemetry {
        val sample = latest
        val speedKmh = sample?.speedMps?.let { GeoMath.metersPerSecondToKmh(it.coerceAtLeast(0.0)) } ?: 0.0
        val avg = if (speedSamples > 0) speedSumKmh / speedSamples else 0.0
        val duration = if (startedAtMs > 0) (nowMs - startedAtMs).coerceAtLeast(0) else 0L
        val weak = sample?.accuracyM?.let { it > 40f } ?: false
        return LiveTelemetry(
            tripId = tripId,
            active = tripId != null,
            speedKmh = speedKmh,
            avgSpeedKmh = avg,
            maxSpeedKmh = maxSpeedKmh,
            distanceM = distanceM,
            durationMs = duration,
            latitude = sample?.latitude,
            longitude = sample?.longitude,
            pointCount = pointCount,
            rejectedJumps = rejectedJumps,
            gpsWeak = weak,
        )
    }
}
