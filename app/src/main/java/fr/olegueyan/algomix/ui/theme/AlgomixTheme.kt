package fr.olegueyan.algomix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AlgomixLightColors = lightColorScheme(
    primary = AlgomixPalette.OrangePrimary,
    onPrimary = AlgomixPalette.SurfaceWhite,
    primaryContainer = AlgomixPalette.OrangeSoftBg,
    onPrimaryContainer = AlgomixPalette.OrangeOnSoft,
    secondary = AlgomixPalette.BrandBlue,
    onSecondary = AlgomixPalette.SurfaceWhite,
    secondaryContainer = AlgomixPalette.BlueGhostBg,
    onSecondaryContainer = AlgomixPalette.BlueGhostText,
    tertiary = AlgomixPalette.Success,
    background = AlgomixPalette.BackgroundLight,
    onBackground = AlgomixPalette.TextPrimary,
    surface = AlgomixPalette.SurfaceWhite,
    onSurface = AlgomixPalette.TextPrimary,
    surfaceVariant = AlgomixPalette.SurfaceSoftBlue,
    onSurfaceVariant = AlgomixPalette.TextMuted,
    outline = AlgomixPalette.Divider,
    outlineVariant = AlgomixPalette.Divider,
    error = AlgomixPalette.Danger,
    onError = AlgomixPalette.SurfaceWhite,
    errorContainer = AlgomixPalette.DangerSoftBg,
    onErrorContainer = AlgomixPalette.DangerOnSoft,
)

private val AlgomixDarkColors = darkColorScheme(
    primary = AlgomixPalette.OrangePrimary,
    onPrimary = AlgomixPalette.TextPrimary,
    primaryContainer = AlgomixPalette.OrangeOnSoft,
    onPrimaryContainer = AlgomixPalette.OrangeSoftBg,
    secondary = AlgomixPalette.BlueGhostText,
    onSecondary = AlgomixPalette.SurfaceWhite,
    secondaryContainer = AlgomixPalette.BlueGhostText,
    onSecondaryContainer = AlgomixPalette.BlueGhostBg,
    tertiary = AlgomixPalette.Success,
    background = AlgomixPalette.BackgroundDark,
    onBackground = AlgomixPalette.TextOnDark,
    surface = AlgomixPalette.SurfaceDark,
    onSurface = AlgomixPalette.TextOnDark,
    surfaceVariant = AlgomixPalette.SurfaceDark,
    onSurfaceVariant = AlgomixPalette.TextMutedDark,
    outline = AlgomixPalette.DividerDark,
    outlineVariant = AlgomixPalette.DividerDark,
    error = AlgomixPalette.Danger,
    onError = AlgomixPalette.SurfaceWhite,
)

@Composable
fun AlgomixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AlgomixDarkColors else AlgomixLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AlgomixTypography,
        shapes = AlgomixShapes,
        content = content,
    )
}
