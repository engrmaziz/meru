package app.meru.android.engine.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class MotionEngine @Inject constructor(
    @ApplicationContext context: Context,
    private val calibrationStore: CalibrationStore,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _motion = MutableStateFlow(MotionSample())
    val motion: StateFlow<MotionSample> = _motion.asStateFlow()

    private var profile: CalibrationProfile? = null
    private var listening = false
    private var gravityBuf = FloatArray(3)
    private var linearBuf = FloatArray(3)

    init {
        scope.launch {
            calibrationStore.profile.collect { profile = it }
        }
    }

    fun start() {
        if (listening) return
        listening = true
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
    }

    fun stop() {
        if (!listening) return
        listening = false
        sensorManager.unregisterListener(this)
        _motion.value = MotionSample(calibrated = profile != null)
    }

    /**
     * Hold phone still in mount for [durationMs], average gravity, persist profile.
     */
    suspend fun calibrate(durationMs: Long = 2_000L): CalibrationProfile {
        start()
        val samples = mutableListOf<FloatArray>()
        val end = System.currentTimeMillis() + durationMs
        while (System.currentTimeMillis() < end) {
            samples.add(gravityBuf.copyOf())
            delay(50)
        }
        val n = samples.size.coerceAtLeast(1)
        var sx = 0f
        var sy = 0f
        var sz = 0f
        samples.forEach {
            sx += it[0]
            sy += it[1]
            sz += it[2]
        }
        val built = MotionMath.buildProfile(sx / n, sy / n, sz / n, System.currentTimeMillis())
        // Require a plausible gravity magnitude (~9.8)
        val mag = kotlin.math.sqrt(built.gravityX * built.gravityX + built.gravityY * built.gravityY + built.gravityZ * built.gravityZ)
        check(abs(mag - 9.8f) < 3.5f) { "Keep the phone still while calibrating." }
        calibrationStore.save(built)
        profile = built
        return built
    }

    suspend fun hasCalibration(): Boolean = calibrationStore.profile.first() != null

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                System.arraycopy(event.values, 0, gravityBuf, 0, 3)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                System.arraycopy(event.values, 0, linearBuf, 0, 3)
                _motion.value = MotionMath.project(linearBuf, profile)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Fallback: remove gravity approx
                val ax = event.values[0] - gravityBuf[0]
                val ay = event.values[1] - gravityBuf[1]
                val az = event.values[2] - gravityBuf[2]
                linearBuf[0] = ax
                linearBuf[1] = ay
                linearBuf[2] = az
                _motion.value = MotionMath.project(linearBuf, profile)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
