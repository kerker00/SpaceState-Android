package net.grafixmafia.spacestate.data

import android.content.Context
import android.os.Build
import java.io.File
import net.grafixmafia.spacestate.BuildConfig

/**
 * How the app describes itself to SpacePush, for its usage statistics: a user agent such
 * as `SpaceState/1.0 (Android 16)` and which day, week and month a request is the first of.
 * No ID is sent. Widgets will report no periods, as on iOS, so they don't count as installs.
 */
object ClientIdentity {
    const val PLATFORM = "android"

    val userAgent: String
        get() = "SpaceState/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE})"

    fun activePeriodsFile(context: Context): String = File(context.filesDir, "reportedPeriods.json").path
}
