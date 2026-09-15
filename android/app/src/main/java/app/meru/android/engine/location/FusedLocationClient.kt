package app.meru.android.engine.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import app.meru.android.engine.telemetry.GpsSample
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class FusedLocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<GpsSample> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    GpsSample(
                        timestampMs = loc.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitudeM = if (loc.hasAltitude()) loc.altitude else null,
                        speedMps = if (loc.hasSpeed()) loc.speed.toDouble() else null,
                        bearing = if (loc.hasBearing()) loc.bearing else null,
                        accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                    ),
                )
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
