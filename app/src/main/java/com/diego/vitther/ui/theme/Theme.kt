package com.diego.vitther.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * VITTHER tiene un único esquema: museo nocturno.
 * No hay modo claro ni color dinámico, por decisión de identidad.
 */
private val VitherColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = BackgroundPrimary,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldOnContainer,
    secondary = AccentGoldDim,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceHigh,
    onSecondaryContainer = AccentGold,
    tertiary = CapsuleViolet,
    onTertiary = BackgroundPrimary,
    tertiaryContainer = CapsuleContainer,
    onTertiaryContainer = CapsuleOnContainer,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardAlt,
    onSurfaceVariant = TextSecondary,
    surfaceTint = AccentGold,
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundPrimary,
    inversePrimary = AccentGoldDim,
    error = ErrorRed,
    onError = BackgroundPrimary,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorOnContainer,
    outline = BorderGold,
    outlineVariant = OutlineVariant,
    scrim = Color.Black,
    surfaceBright = SurfaceHighest,
    surfaceDim = SurfaceLowest,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    surfaceContainerLow = SurfaceLow,
    surfaceContainerLowest = SurfaceLowest
)

@Composable
fun VITTHERTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VitherColorScheme,
        typography = Typography,
        content = content
    )
}
