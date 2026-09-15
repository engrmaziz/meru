package app.meru.android.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.meru.android.core.network.ScoresMeResponse
import app.meru.android.core.network.TripAwardsDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.progressionDataStore: DataStore<Preferences> by preferencesDataStore("meru_progression")

@Singleton
class ProgressionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) {
    private val dataStore = context.progressionDataStore

    val snapshot: Flow<ProgressionSnapshot> = dataStore.data.map { prefs ->
        val awardsJson = prefs[Keys.LAST_AWARDS]
        ProgressionSnapshot(
            xpTotal = prefs[Keys.XP_TOTAL] ?: 0,
            level = prefs[Keys.LEVEL] ?: 1,
            title = prefs[Keys.TITLE] ?: "Novice Driver",
            xpIntoLevel = prefs[Keys.XP_INTO] ?: 0,
            xpForNextLevel = prefs[Keys.XP_NEXT] ?: 1000,
            adventureScore = prefs[Keys.ADVENTURE] ?: 0,
            driverRating = prefs[Keys.DRIVER] ?: 70,
            quality = prefs[Keys.QUALITY] ?: 0,
            exploration = prefs[Keys.EXPLORATION] ?: 0,
            activity = prefs[Keys.ACTIVITY] ?: 0,
            streakDays = prefs[Keys.STREAK] ?: 0,
            competitiveEligible = prefs[Keys.ELIGIBLE] ?: true,
            weightsVersion = prefs[Keys.WEIGHTS] ?: 0,
            notifLevelUp = prefs[Keys.NOTIF_LEVEL] ?: true,
            notifChallenge = prefs[Keys.NOTIF_CHALLENGE] ?: true,
            notifStreak = prefs[Keys.NOTIF_STREAK] ?: true,
            lastAwards = awardsJson?.let {
                runCatching { json.decodeFromString(TripAwardsDto.serializer(), it) }.getOrNull()
            },
        )
    }

    suspend fun applyScores(me: ScoresMeResponse) {
        dataStore.edit { prefs ->
            prefs[Keys.XP_TOTAL] = me.xpTotal
            prefs[Keys.LEVEL] = me.level
            prefs[Keys.TITLE] = me.title
            prefs[Keys.XP_INTO] = me.xpIntoLevel
            prefs[Keys.XP_NEXT] = me.xpForNextLevel
            prefs[Keys.ADVENTURE] = me.adventureScore
            prefs[Keys.DRIVER] = me.driverRating
            prefs[Keys.QUALITY] = me.components.quality
            prefs[Keys.EXPLORATION] = me.components.exploration
            prefs[Keys.ACTIVITY] = me.components.activity
            prefs[Keys.STREAK] = me.streakDays
            prefs[Keys.ELIGIBLE] = me.competitiveEligible
            prefs[Keys.WEIGHTS] = me.weightsVersion
            me.lastTripAwards?.let {
                prefs[Keys.LAST_AWARDS] = json.encodeToString(TripAwardsDto.serializer(), it)
            }
        }
    }

    suspend fun applyAwards(awards: TripAwardsDto) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_AWARDS] = json.encodeToString(TripAwardsDto.serializer(), awards)
            prefs[Keys.LEVEL] = awards.level
            prefs[Keys.TITLE] = awards.title
            prefs[Keys.STREAK] = awards.streakDays
            prefs[Keys.ELIGIBLE] = awards.competitiveEligible
            prefs[Keys.WEIGHTS] = awards.weightsVersion
            prefs[Keys.ADVENTURE] = awards.adventureScore
            prefs[Keys.DRIVER] = awards.driverRating
            prefs[Keys.QUALITY] = awards.qualityScore
            prefs[Keys.EXPLORATION] = awards.explorationScore
            prefs[Keys.ACTIVITY] = awards.activityScore
            val prev = prefs[Keys.XP_TOTAL] ?: 0
            prefs[Keys.XP_TOTAL] = prev + awards.xpAwarded
            val total = prefs[Keys.XP_TOTAL] ?: 0
            val step = prefs[Keys.XP_NEXT] ?: 1000
            prefs[Keys.LEVEL] = 1 + total / step
            prefs[Keys.XP_INTO] = total % step
        }
    }

    suspend fun setNotif(levelUp: Boolean? = null, challenge: Boolean? = null, streak: Boolean? = null) {
        dataStore.edit { prefs ->
            levelUp?.let { prefs[Keys.NOTIF_LEVEL] = it }
            challenge?.let { prefs[Keys.NOTIF_CHALLENGE] = it }
            streak?.let { prefs[Keys.NOTIF_STREAK] = it }
        }
    }

    private object Keys {
        val XP_TOTAL = intPreferencesKey("xp_total")
        val LEVEL = intPreferencesKey("level")
        val TITLE = stringPreferencesKey("title")
        val XP_INTO = intPreferencesKey("xp_into")
        val XP_NEXT = intPreferencesKey("xp_next")
        val ADVENTURE = intPreferencesKey("adventure")
        val DRIVER = intPreferencesKey("driver")
        val QUALITY = intPreferencesKey("quality")
        val EXPLORATION = intPreferencesKey("exploration")
        val ACTIVITY = intPreferencesKey("activity")
        val STREAK = intPreferencesKey("streak")
        val ELIGIBLE = booleanPreferencesKey("eligible")
        val WEIGHTS = intPreferencesKey("weights")
        val LAST_AWARDS = stringPreferencesKey("last_awards")
        val NOTIF_LEVEL = booleanPreferencesKey("notif_level")
        val NOTIF_CHALLENGE = booleanPreferencesKey("notif_challenge")
        val NOTIF_STREAK = booleanPreferencesKey("notif_streak")
    }
}

data class ProgressionSnapshot(
    val xpTotal: Int = 0,
    val level: Int = 1,
    val title: String = "Novice Driver",
    val xpIntoLevel: Int = 0,
    val xpForNextLevel: Int = 1000,
    val adventureScore: Int = 0,
    val driverRating: Int = 70,
    val quality: Int = 0,
    val exploration: Int = 0,
    val activity: Int = 0,
    val streakDays: Int = 0,
    val competitiveEligible: Boolean = true,
    val weightsVersion: Int = 0,
    val notifLevelUp: Boolean = true,
    val notifChallenge: Boolean = true,
    val notifStreak: Boolean = true,
    val lastAwards: TripAwardsDto? = null,
)
