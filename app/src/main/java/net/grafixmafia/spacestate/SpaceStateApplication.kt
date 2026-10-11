package net.grafixmafia.spacestate

import android.app.Application
import net.grafixmafia.spacestate.data.ClientIdentity
import net.grafixmafia.spacestate.data.DataStoreSettings
import net.grafixmafia.spacestate.data.KitStatusSource
import net.grafixmafia.spacestate.data.SettingsStore
import net.grafixmafia.spacestate.data.StatusSource
import net.grafixmafia.spacestatekit.SpaceStateClient

/** Holds the app-wide services; screens reach them through their ViewModel factories. */
class SpaceStateApplication : Application() {
    val statusSource: StatusSource by lazy {
        KitStatusSource(
            SpaceStateClient(
                spacePushUrl = BuildConfig.SPACEPUSH_URL.ifEmpty { null },
                userAgent = ClientIdentity.userAgent,
                activePeriodsFile = ClientIdentity.activePeriodsFile(this),
            )
        )
    }

    val settings: SettingsStore by lazy { DataStoreSettings(this, statusSource.mainframeEndpoint) }
}
