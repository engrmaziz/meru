package app.meru.android.engine.sensors

import kotlin.math.abs
import kotlin.math.sqrt

data class CalibrationProfile(
    val gravityX: Float,
    val gravityY: Float,
    val gravityZ: Float,
    /** 0=X, 1=Y, 2=Z — axis most horizontal at rest (vehicle forward proxy). */
    val forwardAxis: Int,
    val forwardSign: Float,
    /** Axis orthogonal to forward in the horizontal plane (lateral). */
    val lateralAxis: Int,
    val lateralSign: Float,
    val calibratedAtMs: Long,
)

data class MotionSample(
    val longitudinalG: Float = 0f,
    val lateralG: Float = 0f,
    val magnitudeG: Float = 0f,
    val calibrated: Boolean = false,
)

/**
 * Projects device linear acceleration into vehicle-ish axes using a rest calibration.
 * ponytail: axis heuristic, not full IMU fusion — good enough for cockpit G gauge.
 */
object MotionMath {
    private const val G = 9.80665f

    fun buildProfile(gx: Float, gy: Float, gz: Float, atMs: Long): CalibrationProfile {
        val absG = floatArrayOf(abs(gx), abs(gy), abs(gz))
        val upAxis = absG.indices.maxBy { absG[it] }
        val candidates = (0..2).filter { it != upAxis }
        val forwardAxis = candidates.minBy { absG[it] }
        val lateralAxis = candidates.first { it != forwardAxis }
        val g = floatArrayOf(gx, gy, gz)
        // Forward sign: positive accel in travel direction later; default +1.
        val forwardSign = 1f
        // Lateral: choose sign so right-hand-ish relative to up×forward
        val lateralSign = 1f
        return CalibrationProfile(
            gravityX = gx,
            gravityY = gy,
            gravityZ = gz,
            forwardAxis = forwardAxis,
            forwardSign = forwardSign,
            lateralAxis = lateralAxis,
            lateralSign = lateralSign,
            calibratedAtMs = atMs,
        )
    }

    fun project(linearAccel: FloatArray, profile: CalibrationProfile?): MotionSample {
        if (profile == null || linearAccel.size < 3) {
            val mag = magnitude(linearAccel) / G
            return MotionSample(magnitudeG = mag, calibrated = false)
        }
        val long = profile.forwardSign * linearAccel[profile.forwardAxis] / G
        val lat = profile.lateralSign * linearAccel[profile.lateralAxis] / G
        return MotionSample(
            longitudinalG = long,
            lateralG = lat,
            magnitudeG = sqrt(long * long + lat * lat),
            calibrated = true,
        )
    }

    private fun magnitude(v: FloatArray): Float {
        if (v.isEmpty()) return 0f
        var s = 0f
        for (x in v) s += x * x
        return sqrt(s)
    }
}
