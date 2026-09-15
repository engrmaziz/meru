package app.meru.android.engine.telemetry

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoMathTest {
    @Test
    fun haversine_samePoint_isZero() {
        val d = GeoMath.haversineMeters(31.5204, 74.3587, 31.5204, 74.3587)
        assertThat(d).isWithin(0.01).of(0.0)
    }

    @Test
    fun haversine_lahoreToNearby_isReasonable() {
        // ~1.1 km north of a Lahore point
        val d = GeoMath.haversineMeters(31.5204, 74.3587, 31.5304, 74.3587)
        assertThat(d).isGreaterThan(1000.0)
        assertThat(d).isLessThan(1300.0)
    }

    @Test
    fun mpsToKmh() {
        assertThat(GeoMath.metersPerSecondToKmh(10.0)).isWithin(0.01).of(36.0)
    }
}

class JumpFilterTest {
    private val filter = JumpFilter(maxImpliedSpeedMps = 70.0)

    @Test
    fun acceptsFirstSample() {
        val s = GpsSample(1_000, 31.52, 74.35, accuracyM = 10f)
        assertThat(filter.accept(null, s)).isTrue()
    }

    @Test
    fun rejectsInsaneJump() {
        val a = GpsSample(1_000, 31.52, 74.35, accuracyM = 10f)
        // Karachi-ish, 2 seconds later
        val b = GpsSample(3_000, 24.86, 67.00, accuracyM = 10f)
        assertThat(filter.accept(a, b)).isFalse()
    }

    @Test
    fun acceptsNormalHighwayStep() {
        val a = GpsSample(1_000, 31.5200, 74.3587, accuracyM = 8f)
        // ~28m north in 1s ≈ 100 km/h
        val b = GpsSample(2_000, 31.52025, 74.3587, speedMps = 28.0, accuracyM = 8f)
        assertThat(filter.accept(a, b)).isTrue()
    }

    @Test
    fun rejectsPoorAccuracy() {
        val s = GpsSample(1_000, 31.52, 74.35, accuracyM = 120f)
        assertThat(filter.accept(null, s)).isFalse()
    }
}

class TelemetryAccumulatorTest {
    @Test
    fun accumulatesDistanceAndIgnoresJump() {
        val acc = TelemetryAccumulator()
        acc.start("t1", 0)
        assertThat(acc.onSample(GpsSample(0, 31.5200, 74.3587, speedMps = 10.0, accuracyM = 5f))).isTrue()
        assertThat(acc.onSample(GpsSample(1_000, 31.52025, 74.3587, speedMps = 12.0, accuracyM = 5f))).isTrue()
        assertThat(acc.onSample(GpsSample(2_000, 24.86, 67.00, speedMps = 12.0, accuracyM = 5f))).isFalse()
        val snap = acc.snapshot(2_000)
        assertThat(snap.pointCount).isEqualTo(2)
        assertThat(snap.rejectedJumps).isEqualTo(1)
        assertThat(snap.distanceM).isGreaterThan(20.0)
        assertThat(snap.distanceM).isLessThan(50.0)
        assertThat(snap.maxSpeedKmh).isWithin(0.1).of(43.2)
    }
}
