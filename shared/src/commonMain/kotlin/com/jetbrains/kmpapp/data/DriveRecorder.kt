package com.jetbrains.kmpapp.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object LiveDriveBus {
    val fixes = MutableSharedFlow<RideFix>(extraBufferCapacity = 128)
    fun emit(fix: RideFix) {
        fixes.tryEmit(fix)
    }
}

class PlaceClient(private val http: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sky(lat: Double, lon: Double): Sky = runCatching {
        val body = http.get(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,wind_speed_10m",
        ).bodyAsText()
        val current = json.parseToJsonElement(body).jsonObject["current"]?.jsonObject
        val code = current?.get("weather_code")?.jsonPrimitive?.intOrNull
        Sky(
            label = weatherLabel(code),
            tempC = current?.get("temperature_2m")?.jsonPrimitive?.doubleOrNull,
            windKmh = current?.get("wind_speed_10m")?.jsonPrimitive?.doubleOrNull,
            aqi = air(lat, lon),
        )
    }.getOrDefault(Sky())

    suspend fun here(lat: Double, lon: Double): Here = runCatching {
        val body = http.get(
            "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json",
        ) {
            header("User-Agent", "Meru/1.0 (driving companion)")
        }.bodyAsText()
        val address = json.parseToJsonElement(body).jsonObject["address"]?.jsonObject
        fun pick(vararg keys: String) = keys.firstNotNullOfOrNull { key ->
            address?.get(key)?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        }.orEmpty()
        Here(
            city = pick("city", "town", "village", "municipality", "state_district"),
            province = pick("state", "province", "region"),
            country = pick("country"),
        )
    }.getOrDefault(Here())

    private suspend fun air(lat: Double, lon: Double): Int? = runCatching {
        val body = http.get(
            "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi",
        ).bodyAsText()
        val current = json.parseToJsonElement(body).jsonObject["current"]?.jsonObject
        current?.get("us_aqi")?.jsonPrimitive?.intOrNull
    }.getOrNull()
}

class DriveRecorder(
    private val backend: AscentBackend,
    private val places: PlaceClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val samples = mutableListOf<RideSample>()
    private val marks = mutableListOf<RideMark>()
    private var startedAt = 0L
    private var lastSpeed = 0.0
    private var lastBearing = 0f
    private var lastLaneAt = 0L
    private var lastBrakeAt = 0L
    private var sim: Job? = null
    private var sky = Sky()
    private var here = Here()
    private var lookedUp = false
    private var realMoved = false
    private var watch: Job? = null
    private var lastRealLat = Double.NaN
    private var lastRealLon = Double.NaN
    private var usingTrace = false

    private val _live = MutableStateFlow(LiveDrive())
    val live: StateFlow<LiveDrive> = _live.asStateFlow()

    init {
        scope.launch {
            LiveDriveBus.fixes.collect { onFix(it) }
        }
    }

    fun start() {
        if (_live.value.active) return
        samples.clear()
        marks.clear()
        startedAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
        lastSpeed = 0.0
        lastBearing = -1f
        lookedUp = false
        realMoved = false
        sky = Sky(label = "Reading the sky…")
        here = Here()
        lastRealLat = Double.NaN
        lastRealLon = Double.NaN
        usingTrace = !platformHasGps()
        _live.value = LiveDrive(active = true, startedAt = startedAt, sky = sky)
        if (!platformHasGps()) {
            startSimulator()
        } else {
            startHardwareRide()
            watch = scope.launch {
                delay(4000)
                if (_live.value.active && !realMoved) startSimulator()
            }
        }
    }

    fun stop(): SavedDrive? {
        stopHardwareRide()
        watch?.cancel()
        sim?.cancel()
        sim = null
        if (!_live.value.active) return null
        val draft = draft(ended = true)
        _live.value = LiveDrive()
        samples.clear()
        marks.clear()
        if (draft.points.isEmpty()) {
            backend.clearCheckpoint()
            return null
        }
        return backend.acceptDrive(draft)
    }

    private fun onFix(fix: RideFix) {
        if (!_live.value.active) return
        if (!fix.synthetic) {
            if (!lastRealLat.isNaN()) {
                val moved = haversineKm(lastRealLat, lastRealLon, fix.lat, fix.lon) * 1000
                if (moved > 25) {
                    realMoved = true
                    usingTrace = false
                    sim?.cancel()
                    sim = null
                }
            }
            lastRealLat = fix.lat
            lastRealLon = fix.lon
        } else if (realMoved) {
            return
        }
        val previous = samples.lastOrNull()
        val speed = when {
            fix.speedMps >= 0f -> fix.speedMps * 3.6
            previous == null -> 0.0
            else -> {
                val seconds = (fix.t - previous.t) / 1000.0
                val metres = haversineKm(previous.lat, previous.lon, fix.lat, fix.lon) * 1000
                if (seconds < 0.4 || metres > 500) lastSpeed else metres / seconds * 3.6
            }
        }
        val now = fix.t
        if (lastSpeed - speed > 8 && lastSpeed > 12 && now - lastBrakeAt > 2_500) {
            marks += RideMark(now, "brake", fix.lat, fix.lon, "Brakes at ${speed.toInt()} km/h")
            lastBrakeAt = now
        }
        if (fix.bearing >= 0f && lastBearing >= 0f) {
            val turn = bearingDelta(lastBearing, fix.bearing)
            if (speed > 20 && abs(turn) in 8f..32f && now - lastLaneAt > 4_000) {
                val side = if (turn > 0) "right" else "left"
                marks += RideMark(now, "lane", fix.lat, fix.lon, "Lane change $side")
                lastLaneAt = now
            }
        }
        lastSpeed = speed
        if (fix.bearing >= 0f) lastBearing = fix.bearing
        val g = kotlin.math.sqrt(fix.longG * fix.longG + fix.latG * fix.latG).toDouble()
        samples += RideSample(fix.t, fix.lat, fix.lon, fix.alt, speed, fix.bearing, fix.longG.toDouble(), fix.latG.toDouble())
        if (!lookedUp) {
            lookedUp = true
            scope.launch {
                sky = places.sky(fix.lat, fix.lon)
                here = places.here(fix.lat, fix.lon)
                publish()
            }
        }
        if (samples.size % 5 == 0) backend.checkpoint(draft(ended = false))
        publish(g)
    }

    private fun publish(latestG: Double = _live.value.peakG) {
        val distance = distanceM(samples)
        val speeds = samples.map { it.speedKmh }
        _live.value = LiveDrive(
            active = true,
            startedAt = startedAt,
            samples = samples.toList(),
            marks = marks.toList(),
            distanceM = distance,
            speedKmh = speeds.lastOrNull() ?: 0.0,
            maxSpeedKmh = speeds.maxOrNull() ?: 0.0,
            avgSpeedKmh = if (speeds.isEmpty()) 0.0 else speeds.average(),
            peakG = maxOf(_live.value.peakG, latestG),
            longG = samples.lastOrNull()?.longG ?: 0.0,
            latG = samples.lastOrNull()?.latG ?: 0.0,
            elevationM = samples.lastOrNull()?.alt,
            elevationGainM = climb(samples),
            brakeCount = marks.count { it.kind == "brake" },
            laneChanges = marks.count { it.kind == "lane" },
            sky = sky,
            here = here,
            note = if (usingTrace) "The phone has not moved yet, so this trace is moving for you until the road does." else "",
        )
    }

    private fun draft(ended: Boolean) = SavedDrive(
        id = if (ended) "drv-" + samples.firstOrNull()?.t.toString() else "open",
        startedAt = startedAt,
        endedAt = if (ended) kotlin.time.Clock.System.now().toEpochMilliseconds() else 0L,
        points = samples.toList(),
        marks = marks.toList(),
        distanceM = distanceM(samples),
        maxSpeedKmh = samples.maxOfOrNull { it.speedKmh } ?: 0.0,
        avgSpeedKmh = if (samples.isEmpty()) 0.0 else samples.map { it.speedKmh }.average(),
        peakG = _live.value.peakG,
        elevationGainM = climb(samples),
        brakeCount = marks.count { it.kind == "brake" },
        laneChanges = marks.count { it.kind == "lane" },
        weather = sky.label,
        tempC = sky.tempC,
        aqi = sky.aqi,
        windKmh = sky.windKmh,
        city = here.city,
        province = here.province,
        country = here.country,
    )

    private fun startSimulator() {
        if (sim?.isActive == true) return
        usingTrace = !realMoved
        sim = scope.launch {
            var lat = if (lastRealLat.isNaN()) 31.5204 else lastRealLat
            var lon = if (lastRealLon.isNaN()) 74.3587 else lastRealLon
            var alt = 217.0
            var heading = 0.4
            var tick = 0
            while (true) {
                delay(1000)
                tick += 1
                heading += if (tick % 9 == 0) 0.45 else sin(lat) * 0.08
                lat += cos(heading) * 0.00018
                lon += sin(heading) * 0.00018
                alt += if (tick % 5 == 0) 6.0 else sin(heading) * 0.8
                val speed = when (tick % 12) {
                    8 -> 4.0
                    9 -> 16.0
                    else -> 14.0 + sin(heading) * 3
                }
                LiveDriveBus.emit(
                    RideFix(
                        t = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                        lat = lat,
                        lon = lon,
                        alt = alt,
                        speedMps = speed.toFloat(),
                        bearing = (((heading * 180 / PI) % 360 + 360) % 360).toFloat(),
                        longG = if (tick % 12 == 8) -0.45f else (sin(heading) * 0.2).toFloat(),
                        latG = if (tick % 9 == 0) 0.35f else (cos(heading) * 0.12).toFloat(),
                        synthetic = true,
                    ),
                )
            }
        }
    }
}

data class LiveDrive(
    val active: Boolean = false,
    val startedAt: Long = 0L,
    val samples: List<RideSample> = emptyList(),
    val marks: List<RideMark> = emptyList(),
    val distanceM: Double = 0.0,
    val speedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val peakG: Double = 0.0,
    val longG: Double = 0.0,
    val latG: Double = 0.0,
    val elevationM: Double? = null,
    val elevationGainM: Double = 0.0,
    val brakeCount: Int = 0,
    val laneChanges: Int = 0,
    val sky: Sky = Sky(),
    val here: Here = Here(),
    val note: String = "",
)

private fun bearingDelta(from: Float, to: Float): Float {
    var d = to - from
    while (d > 180) d -= 360
    while (d < -180) d += 360
    return d
}

private fun distanceM(samples: List<RideSample>): Double {
    var m = 0.0
    for (i in 1 until samples.size) {
        val km = haversineKm(samples[i - 1].lat, samples[i - 1].lon, samples[i].lat, samples[i].lon)
        if (km < 0.5) m += km * 1000
    }
    return m
}

private fun climb(samples: List<RideSample>): Double {
    var gain = 0.0
    for (i in 1 until samples.size) {
        val a = samples[i - 1].alt ?: continue
        val b = samples[i].alt ?: continue
        val d = b - a
        if (d in 0.0..40.0) gain += d
    }
    return gain
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * kotlin.math.asin(kotlin.math.min(1.0, kotlin.math.sqrt(a)))
}

private fun weatherLabel(code: Int?): String = when (code) {
    0 -> "Clear"
    1, 2 -> "Mostly clear"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55, 61, 63, 65 -> "Rain"
    71, 73, 75 -> "Snow"
    80, 81, 82 -> "Showers"
    95, 96, 99 -> "Storm"
    null -> "Sky unread"
    else -> "Weather $code"
}
