package net.grafixmafia.spacestate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = AmberLight, onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4B7), onPrimaryContainer = Color(0xFF382100),
    secondary = Color(0xFF596574), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEE3EA), onSecondaryContainer = Ink,
    tertiary = LightStatusColors.open, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDEEDCF), onTertiaryContainer = Color(0xFF213816),
    background = Paper, onBackground = Ink,
    surface = PaperSurface, onSurface = Ink,
    surfaceVariant = Color(0xFFECEAE4), onSurfaceVariant = Color(0xFF5E6570),
    surfaceDim = Color(0xFFE0DFD9), surfaceBright = PaperSurface,
    surfaceContainerLowest = Color.White, surfaceContainerLow = PaperSurface,
    surfaceContainer = Paper, surfaceContainerHigh = Color(0xFFECEAE4),
    surfaceContainerHighest = Color(0xFFE2E1DB),
    outline = Color(0xFF747A83), outlineVariant = Color(0xFFCED0D2),
    error = LightStatusColors.closed, onError = Color.White,
    errorContainer = Color(0xFFFFDAD4), onErrorContainer = Color(0xFF45110B),
    inverseSurface = Ink, inverseOnSurface = Paper, inversePrimary = AmberDark,
)

private val DarkColorScheme = darkColorScheme(
    primary = AmberDark, onPrimary = Color(0xFF382100),
    primaryContainer = Color(0xFF684000), onPrimaryContainer = Color(0xFFFFE4B7),
    secondary = Color(0xFFB7C0CE), onSecondary = Ink,
    secondaryContainer = Color(0xFF394353), onSecondaryContainer = Color(0xFFDEE3EA),
    tertiary = DarkStatusColors.open, onTertiary = Color(0xFF213816),
    tertiaryContainer = Color(0xFF354F25), onTertiaryContainer = Color(0xFFDEEDCF),
    background = Slate, onBackground = Paper,
    surface = SlateSurface, onSurface = Paper,
    surfaceVariant = Color(0xFF303947), onSurfaceVariant = Color(0xFFB7C0CE),
    surfaceDim = Slate, surfaceBright = Color(0xFF394353),
    surfaceContainerLowest = Color(0xFF10151C), surfaceContainerLow = SlateSurface,
    surfaceContainer = Color(0xFF242C38), surfaceContainerHigh = Color(0xFF303947),
    surfaceContainerHighest = Color(0xFF394353),
    outline = Color(0xFF8C96A5), outlineVariant = Color(0xFF394353),
    error = DarkStatusColors.closed, onError = Color(0xFF45110B),
    errorContainer = Color(0xFF733026), onErrorContainer = Color(0xFFFFDAD4),
    inverseSurface = Paper, inverseOnSurface = Ink, inversePrimary = AmberLight,
)

@Composable
fun SpaceStateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep Android wallpaper colors for surfaces/actions; status roles retain their meaning.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(LocalSpaceStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes(large = RoundedCornerShape(16.dp)),
            content = content,
        )
    }
}
