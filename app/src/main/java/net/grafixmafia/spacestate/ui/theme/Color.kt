package net.grafixmafia.spacestate.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Shared paper/slate and amber roles from the Apple app and website.
val Paper = Color(0xFFF4F3EE)
val PaperSurface = Color(0xFFFBFAF7)
val Ink = Color(0xFF252D38)
val Slate = Color(0xFF151A22)
val SlateSurface = Color(0xFF1E2530)
val AmberLight = Color(0xFF925400)
val AmberDark = Color(0xFFFAC276)

data class SpaceStatusColors(val open: Color, val closed: Color, val restricted: Color)

val LightStatusColors = SpaceStatusColors(Color(0xFF4F723B), Color(0xFFAD3D31), AmberLight)
val DarkStatusColors = SpaceStatusColors(Color(0xFFAAD77C), Color(0xFFFF9B8B), AmberDark)

// Follows the app theme, including previews with an explicit appearance override.
val LocalSpaceStatusColors = staticCompositionLocalOf { LightStatusColors }
