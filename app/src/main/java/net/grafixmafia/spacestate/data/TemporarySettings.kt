package net.grafixmafia.spacestate.data

import kotlinx.coroutines.flow.flowOf

/** A linked space has an isolated, read-only selection; it never reaches DataStore. */
class TemporarySettings(endpoint: String, refreshSeconds: Int) : SettingsStore {
    override val settings = flowOf(Settings(endpoint,
        refreshSeconds.takeIf { it in SettingsStore.refreshIntervals } ?: SettingsStore.DEFAULT_REFRESH_SECONDS))
    override suspend fun setEndpoint(endpoint: String) = Unit
    override suspend fun setRefreshSeconds(seconds: Int) = Unit
}
