package app.meru.android.engine.trip

import app.meru.android.core.database.ExploredCellEntity
import app.meru.android.core.database.ExplorationDao
import app.meru.android.core.database.PendingSyncDao
import app.meru.android.core.database.PendingSyncEntity
import app.meru.android.core.database.TripDao
import app.meru.android.core.database.TripEntity
import app.meru.android.core.database.TripEventEntity
import app.meru.android.core.database.TripLocationEntity
import app.meru.android.core.network.TripEventDto
import app.meru.android.core.network.TripLocationDto
import app.meru.android.core.network.TripUpsertRequest
import app.meru.android.engine.telemetry.GeoHash
import app.meru.android.engine.telemetry.GeoMath
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ProcessedTripResult(
    val trip: TripEntity,
    val events: List<TripEventEntity>,
    val newCells: Int,
    val explorationXp: Int,
)

@Singleton
class TripProcessor @Inject constructor(
    private val tripDao: TripDao,
    private val explorationDao: ExplorationDao,
    private val pendingSyncDao: PendingSyncDao,
    private val json: Json,
) {
    suspend fun process(tripId: String): ProcessedTripResult {
        val trip = tripDao.getTrip(tripId) ?: error("Trip missing")
        if (trip.status == "processed") {
            return ProcessedTripResult(
                trip = trip,
                events = tripDao.eventsForTrip(tripId),
                newCells = trip.newCells,
                explorationXp = trip.explorationXp,
            )
        }
        val locations = tripDao.locationsForTrip(tripId)
        val events = mutableListOf<TripEventEntity>()

        events += TripEventEntity(
            tripId = tripId,
            timestampMs = trip.startAtMs,
            type = "START",
            latitude = locations.firstOrNull()?.latitude,
            longitude = locations.firstOrNull()?.longitude,
            label = "Drive started",
        )

        var elevGain = 0.0
        var elevLoss = 0.0
        var stops = 0
        var hardBrakes = 0
        var prevAlt: Double? = null
        var prevSpeed: Double? = null
        var moving = false

        for (loc in locations) {
            val speedKmh = loc.speedMps?.let { GeoMath.metersPerSecondToKmh(it) } ?: 0.0
            loc.altitudeM?.let { alt ->
                prevAlt?.let { p ->
                    val d = alt - p
                    if (d > 0) elevGain += d else elevLoss += -d
                    if (abs(d) > 15) {
                        events += TripEventEntity(
                            tripId = tripId,
                            timestampMs = loc.timestampMs,
                            type = "ELEVATION_CHANGE",
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            severity = if (abs(d) > 40) 2 else 1,
                            label = if (d > 0) "Elevation ascent" else "Elevation descent",
                        )
                    }
                }
                prevAlt = alt
            }
            prevSpeed?.let { ps ->
                val delta = speedKmh - ps
                if (delta <= -15) {
                    hardBrakes++
                    events += TripEventEntity(
                        tripId = tripId,
                        timestampMs = loc.timestampMs,
                        type = "BRAKING",
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        severity = if (delta <= -25) 3 else 2,
                        label = "Braking detected",
                    )
                } else if (delta >= 15) {
                    events += TripEventEntity(
                        tripId = tripId,
                        timestampMs = loc.timestampMs,
                        type = "ACCELERATION",
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        severity = 1,
                        label = "Acceleration",
                    )
                }
            }
            if (speedKmh < 3) {
                if (moving) {
                    stops++
                    events += TripEventEntity(
                        tripId = tripId,
                        timestampMs = loc.timestampMs,
                        type = "STOP",
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        label = "Stop",
                    )
                }
                moving = false
            } else {
                moving = true
            }
            prevSpeed = speedKmh
        }

        val known = explorationDao.allCellIds().toHashSet()
        var newCells = 0
        val step = max(1, locations.size / 200)
        var i = 0
        while (i < locations.size) {
            val loc = locations[i]
            val cell = GeoHash.encode(loc.latitude, loc.longitude, 7)
            if (cell !in known) {
                val inserted = explorationDao.insertIgnore(
                    ExploredCellEntity(cell, tripId, loc.timestampMs),
                )
                if (inserted != -1L) {
                    known += cell
                    newCells++
                    if (newCells <= 12) {
                        events += TripEventEntity(
                            tripId = tripId,
                            timestampMs = loc.timestampMs,
                            type = "NEW_ROAD",
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            label = "New area discovered",
                        )
                    }
                }
            }
            i += step
        }

        events += TripEventEntity(
            tripId = tripId,
            timestampMs = trip.endAtMs ?: System.currentTimeMillis(),
            type = "STOP",
            latitude = locations.lastOrNull()?.latitude,
            longitude = locations.lastOrNull()?.longitude,
            label = "Drive completed",
        )

        val quality = provisionalQuality(trip, hardBrakes, locations)
        val xp = newCells * 10
        tripDao.insertEvents(events)

        val processed = trip.copy(
            status = "processed",
            qualityScore = quality,
            explorationXp = xp,
            newCells = newCells,
            elevationGainM = elevGain,
            elevationLossM = elevLoss,
            stopCount = stops,
            syncStatus = "pending",
        )
        tripDao.upsertTrip(processed)

        val payload = buildSyncPayload(processed, locations, events)
        pendingSyncDao.upsert(
            PendingSyncEntity(
                id = tripId,
                type = "trip_complete",
                payloadJson = payload,
            ),
        )
        return ProcessedTripResult(processed, events, newCells, xp)
    }

    private fun provisionalQuality(
        trip: TripEntity,
        hardBrakes: Int,
        locations: List<TripLocationEntity>,
    ): Int {
        var score = 92
        score -= min(30, hardBrakes * 4)
        score -= min(15, trip.rejectedJumps * 2)
        if (locations.isEmpty()) score -= 20
        if (trip.distanceM < 200) score -= 5
        return score.coerceIn(40, 99)
    }

    private fun buildSyncPayload(
        trip: TripEntity,
        locations: List<TripLocationEntity>,
        events: List<TripEventEntity>,
    ): String {
        val step = max(1, locations.size / 400)
        val locs = locations.filterIndexed { index, _ -> index % step == 0 }.map { l ->
            TripLocationDto(
                ts = l.timestampMs,
                lat = l.latitude,
                lon = l.longitude,
                alt = l.altitudeM,
                speed = l.speedMps,
                bearing = l.bearing,
                acc = l.accuracyM,
            )
        }
        val ev = events.map { e ->
            TripEventDto(
                ts = e.timestampMs,
                type = e.type,
                label = e.label,
                severity = e.severity,
                lat = e.latitude,
                lon = e.longitude,
            )
        }
        val body = TripUpsertRequest(
            clientTripId = trip.id,
            startAtMs = trip.startAtMs,
            endAtMs = trip.endAtMs,
            distanceM = trip.distanceM,
            durationMs = trip.durationMs,
            avgSpeedKmh = trip.avgSpeedKmh,
            maxSpeedKmh = trip.maxSpeedKmh,
            qualityScore = trip.qualityScore,
            explorationXp = trip.explorationXp,
            newCells = trip.newCells,
            elevationGainM = trip.elevationGainM,
            stopCount = trip.stopCount,
            pointCount = trip.pointCount,
            locations = locs,
            events = ev,
        )
        return json.encodeToString(body)
    }
}
