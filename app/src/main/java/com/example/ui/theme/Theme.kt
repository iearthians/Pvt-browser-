package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GhostCyanPrimary,
    onPrimary = Color(0xFF00262E),
    primaryContainer = GhostCyanContainer,
    onPrimaryContainer = GhostOnCyanContainer,
    secondary = GhostEmeraldSecondary,
    onSecondary = Color(0xFF002116),
    secondaryContainer = GhostEmeraldContainer,
    onSecondaryContainer = GhostOnEmeraldContainer,
    tertiary = GhostAmberTertiary,
    onTertiary = Color(0xFF2A1200),
    tertiaryContainer = GhostAmberContainer,
    onTertiaryContainer = GhostOnAmberContainer,
    error = GhostRoseError,
    onError = Color.White,
    errorContainer = GhostRoseErrorContainer,
    onErrorContainer = GhostOnRoseErrorContainer,
    background = GhostObsidianBg,
    onBackground = GhostSlateTextPrimary,
    surface = GhostObsidianSurface,
    onSurface = GhostSlateTextPrimary,
    surfaceVariant = GhostObsidianSurfaceVariant,
    onSurfaceVariant = GhostSlateTextSecondary,
    outline = GhostSlateOutline
)

private val LightColorScheme = lightColorScheme(
    primary = GhostLightPrimary,
    onPrimary = Color.White,
    primaryContainer = GhostLightPrimaryContainer,
    onPrimaryContainer = GhostLightOnPrimaryContainer,
    secondary = GhostLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = GhostLightSecondaryContainer,
    onSecondaryContainer = GhostLightOnSecondaryContainer,
    tertiary = GhostLightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GhostLightTertiaryContainer,
    onTertiaryContainer = GhostLightOnTertiaryContainer,
    error = GhostLightError,
    onError = Color.White,
    errorContainer = GhostLightErrorContainer,
    onErrorContainer = GhostLightOnErrorContainer,
    background = GhostLightBg,
    onBackground = GhostLightTextPrimary,
    surface = GhostLightSurface,
    onSurface = GhostLightTextPrimary,
    surfaceVariant = GhostLightSurfaceVariant,
    onSurfaceVariant = GhostLightTextSecondary,
    outline = GhostLightOutline
)

val GhostShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GhostSearchTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GhostShapes,
        content = content
    )
}
