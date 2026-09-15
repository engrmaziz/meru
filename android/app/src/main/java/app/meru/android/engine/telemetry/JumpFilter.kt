package app.meru.android.engine.telemetry

data class GpsSample(
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double? = null,
    val speedMps: Double? = null,
    val bearing: Float? = null,
    val accuracyM: Float? = null,
)

/**
 * Rejects impossible GPS jumps (e.g. Lahore → Karachi in seconds).
 * ponytail: simple speed ceiling; upgrade with map-matching later.
 */
class JumpFilter(
    private val maxImpliedSpeedMps: Double = 70.0, // ~252 km/h
    private val maxAccuracyM: Float = 80f,
) {
    fun accept(previous: GpsSample?, next: GpsSample): Boolean {
        next.accuracyM?.let { if (it > maxAccuracyM) return false }
        if (previous == null) return true
        val dtSec = (next.timestampMs - previous.timestampMs) / 1000.0
        if (dtSec <= 0) return false
        val distance = GeoMath.haversineMeters(
            previous.latitude,
            previous.longitude,
            next.latitude,
            next.longitude,
        )
        val implied = distance / dtSec
        return implied <= maxImpliedSpeedMps
    }
}
