package app.meru.android.engine.telemetry

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoHashTest {
    @Test
    fun encodesStablePrecision7() {
        val a = GeoHash.encode(24.8607, 67.0011, 7)
        val b = GeoHash.encode(24.8607, 67.0011, 7)
        assertThat(a).isEqualTo(b)
        assertThat(a).hasLength(7)
    }

    @Test
    fun nearbyPointsCanShareCell() {
        val a = GeoHash.encode(24.86070, 67.00110, 7)
        val b = GeoHash.encode(24.86072, 67.00112, 7)
        assertThat(a).isEqualTo(b)
    }
}
