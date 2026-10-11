package net.grafixmafia.spacestate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    /** The selected space's SpaceAPI endpoint. */
    val endpoint: String,
    val refreshSeconds: Int,
)

/** The user's choices, kept across launches. */
interface SettingsStore {
    val settings: Flow<Settings>

    suspend fun setEndpoint(endpoint: String)

    suspend fun setRefreshSeconds(seconds: Int)

    companion object {
        /** The intervals offered in the settings, as in the Apple apps. */
        val refreshIntervals = listOf(60, 120, 300, 600, 900)
        const val DEFAULT_REFRESH_SECONDS = 120
    }
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreSettings(private val context: Context, private val defaultEndpoint: String) : SettingsStore {
    private val endpointKey = stringPreferencesKey("spaceEndpoint")
    private val refreshKey = intPreferencesKey("refreshSeconds")

    override val settings: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            endpoint = preferences[endpointKey] ?: defaultEndpoint,
            refreshSeconds = preferences[refreshKey]?.takeIf { it > 0 } ?: SettingsStore.DEFAULT_REFRESH_SECONDS,
        )
    }

    override suspend fun setEndpoint(endpoint: String) {
        context.dataStore.edit { it[endpointKey] = endpoint }
    }

    override suspend fun setRefreshSeconds(seconds: Int) {
        context.dataStore.edit { it[refreshKey] = seconds }
    }
}
