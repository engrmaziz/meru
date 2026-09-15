package app.meru.android.engine.telemetry

/**
 * Geohash encoder — precision 7 ≈ ~150m cells (exploration v1).
 * ponytail: no full OSM graph yet (DEC-015).
 */
object GeoHash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(lat: Double, lon: Double, precision: Int = 7): String {
        var minLat = -90.0
        var maxLat = 90.0
        var minLon = -180.0
        var maxLon = 180.0
        val hash = StringBuilder()
        var bit = 0
        var ch = 0
        var even = true
        while (hash.length < precision) {
            if (even) {
                val mid = (minLon + maxLon) / 2
                if (lon >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    minLon = mid
                } else {
                    maxLon = mid
                }
            } else {
                val mid = (minLat + maxLat) / 2
                if (lat >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    minLat = mid
                } else {
                    maxLat = mid
                }
            }
            even = !even
            if (bit < 4) {
                bit++
            } else {
                hash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return hash.toString()
    }
}
