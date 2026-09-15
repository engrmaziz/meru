package app.meru.android.engine.sensors

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calibrationStore by preferencesDataStore("meru_calibration")

@Singleton
class CalibrationStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.calibrationStore

    val profile: Flow<CalibrationProfile?> = dataStore.data.map { prefs ->
        val at = prefs[Keys.AT] ?: return@map null
        CalibrationProfile(
            gravityX = prefs[Keys.GX] ?: return@map null,
            gravityY = prefs[Keys.GY] ?: return@map null,
            gravityZ = prefs[Keys.GZ] ?: return@map null,
            forwardAxis = prefs[Keys.FWD] ?: 1,
            forwardSign = prefs[Keys.FWD_SIGN] ?: 1f,
            lateralAxis = prefs[Keys.LAT] ?: 0,
            lateralSign = prefs[Keys.LAT_SIGN] ?: 1f,
            calibratedAtMs = at,
        )
    }

    suspend fun save(profile: CalibrationProfile) {
        dataStore.edit { prefs ->
            prefs[Keys.GX] = profile.gravityX
            prefs[Keys.GY] = profile.gravityY
            prefs[Keys.GZ] = profile.gravityZ
            prefs[Keys.FWD] = profile.forwardAxis
            prefs[Keys.FWD_SIGN] = profile.forwardSign
            prefs[Keys.LAT] = profile.lateralAxis
            prefs[Keys.LAT_SIGN] = profile.lateralSign
            prefs[Keys.AT] = profile.calibratedAtMs
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val GX = floatPreferencesKey("gx")
        val GY = floatPreferencesKey("gy")
        val GZ = floatPreferencesKey("gz")
        val FWD = intPreferencesKey("fwd")
        val FWD_SIGN = floatPreferencesKey("fwd_sign")
        val LAT = intPreferencesKey("lat")
        val LAT_SIGN = floatPreferencesKey("lat_sign")
        val AT = longPreferencesKey("at")
    }
}
