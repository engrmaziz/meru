package app.meru.android.engine.sensors

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MotionMathTest {
    @Test
    fun buildProfile_picksForwardAsLeastGravityAxis() {
        // Phone flat-ish: Z is up
        val profile = MotionMath.buildProfile(0.2f, 0.3f, 9.7f, 1L)
        assertThat(profile.forwardAxis).isEqualTo(0) // X least
        assertThat(profile.lateralAxis).isEqualTo(1)
    }

    @Test
    fun project_usesCalibrationAxes() {
        val profile = CalibrationProfile(
            gravityX = 0f,
            gravityY = 0f,
            gravityZ = 9.8f,
            forwardAxis = 1,
            forwardSign = 1f,
            lateralAxis = 0,
            lateralSign = 1f,
            calibratedAtMs = 1L,
        )
        val sample = MotionMath.project(floatArrayOf(4.9f, 9.8f, 0f), profile)
        assertThat(sample.calibrated).isTrue()
        assertThat(sample.longitudinalG).isWithin(0.05f).of(1f)
        assertThat(sample.lateralG).isWithin(0.05f).of(0.5f)
    }

    @Test
    fun project_withoutProfile_marksUncalibrated() {
        val sample = MotionMath.project(floatArrayOf(1f, 0f, 0f), null)
        assertThat(sample.calibrated).isFalse()
    }
}
