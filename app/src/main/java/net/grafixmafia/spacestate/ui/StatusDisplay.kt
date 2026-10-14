package net.grafixmafia.spacestate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import net.grafixmafia.spacestate.R
import net.grafixmafia.spacestate.ui.theme.LocalSpaceStatusColors
import net.grafixmafia.spacestatekit.MainframeRoom
import net.grafixmafia.spacestatekit.MainframeRoomState
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.SpaceStatus

val SpaceStatus.title: String
    @Composable @ReadOnlyComposable
    get() = stringResource(
        when (this) {
            SpaceStatus.OPEN -> R.string.status_open
            SpaceStatus.CLOSED -> R.string.status_closed
            SpaceStatus.UNKNOWN -> R.string.status_unknown
        }
    )

val SpaceStatus.icon: ImageVector
    get() = when (this) {
        SpaceStatus.OPEN -> Icons.Outlined.MeetingRoom
        SpaceStatus.CLOSED -> Icons.Outlined.DoorFront
        SpaceStatus.UNKNOWN -> Icons.AutoMirrored.Outlined.HelpOutline
    }

val SpaceStatus.color: Color
    @Composable @ReadOnlyComposable
    get() = when (this) {
        SpaceStatus.OPEN -> LocalSpaceStatusColors.current.open
        SpaceStatus.CLOSED -> LocalSpaceStatusColors.current.closed
        SpaceStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

val MainframeRoom.stateTitle: String
    @Composable @ReadOnlyComposable
    get() = when (state) {
        MainframeRoomState.CLOSED -> stringResource(R.string.room_closed)
        MainframeRoomState.KEYHOLDER -> stringResource(R.string.room_keyholder)
        MainframeRoomState.MEMBER -> stringResource(R.string.room_member)
        MainframeRoomState.OPEN -> stringResource(R.string.room_open)
        MainframeRoomState.OPEN_PLUS -> stringResource(R.string.room_open_plus)
        MainframeRoomState.CLOSING -> stringResource(R.string.room_closing)
        MainframeRoomState.UNKNOWN -> if (rawState.isBlank()) stringResource(R.string.status_unknown) else stringResource(R.string.room_unknown, rawState)
    }

/** Restricted/closing labels are more specific than the generic closed-to-guests state. */
val MainframeRoomState.color: Color
    @Composable @ReadOnlyComposable
    get() = when (this) {
        MainframeRoomState.KEYHOLDER, MainframeRoomState.MEMBER, MainframeRoomState.CLOSING ->
            LocalSpaceStatusColors.current.restricted
        else -> status.color
    }

/** A small colored dot for status lists. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(color, CircleShape))
}

val SpaceStateException.text: String
    @Composable @ReadOnlyComposable
    get() = when (this) {
        is SpaceStateException.Unreachable -> stringResource(R.string.error_unreachable)
        is SpaceStateException.Timeout -> stringResource(R.string.error_timeout)
        is SpaceStateException.HttpStatus -> stringResource(R.string.error_http, code)
        is SpaceStateException.ResponseTooLarge -> stringResource(R.string.error_too_large)
        is SpaceStateException.InvalidData -> stringResource(R.string.error_invalid_data)
        is SpaceStateException.InvalidEndpoint -> stringResource(R.string.error_invalid_endpoint)
        is SpaceStateException.Other -> stringResource(R.string.error_other, message.orEmpty())
    }
