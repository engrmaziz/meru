package app.meru.android.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.meruDataStore: DataStore<Preferences> by preferencesDataStore("meru_prefs")

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.meruDataStore

    val session: Flow<SessionSnapshot> = dataStore.data.map { prefs ->
        SessionSnapshot(
            accessToken = prefs[Keys.ACCESS_TOKEN],
            displayName = prefs[Keys.DISPLAY_NAME],
            email = prefs[Keys.EMAIL],
            onboarded = prefs[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun saveSession(
        accessToken: String,
        displayName: String,
        email: String,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.DISPLAY_NAME] = displayName
            prefs[Keys.EMAIL] = email
            prefs[Keys.ONBOARDED] = true
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val EMAIL = stringPreferencesKey("email")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }
}

data class SessionSnapshot(
    val accessToken: String?,
    val displayName: String?,
    val email: String?,
    val onboarded: Boolean,
) {
    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()
}
