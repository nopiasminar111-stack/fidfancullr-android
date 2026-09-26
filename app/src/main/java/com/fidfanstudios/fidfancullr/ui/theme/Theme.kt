package com.fidfanstudios.fidfancullr.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.fidfanstudios.fidfancullr.data.AccentPalette
import com.fidfanstudios.fidfancullr.data.ThemeMode

private fun colorSchemeFor(accent: AccentPalette, dark: Boolean) = when (accent) {
    AccentPalette.VIOLET_PIXEL -> if (dark)
        darkColorScheme(primary = VioletPixelPrimaryDark, surface = SurfaceDark, onSurface = OnSurfaceDark)
    else
        lightColorScheme(primary = VioletPixelPrimaryLight, surface = SurfaceLight, onSurface = OnSurfaceLight)

    AccentPalette.OCEAN_BLUE -> if (dark)
        darkColorScheme(primary = OceanBluePrimaryDark, surface = SurfaceDark, onSurface = OnSurfaceDark)
    else
        lightColorScheme(primary = OceanBluePrimaryLight, surface = SurfaceLight, onSurface = OnSurfaceLight)

    AccentPalette.MINT_GREEN -> if (dark)
        darkColorScheme(primary = MintGreenPrimaryDark, surface = SurfaceDark, onSurface = OnSurfaceDark)
    else
        lightColorScheme(primary = MintGreenPrimaryLight, surface = SurfaceLight, onSurface = OnSurfaceLight)

    AccentPalette.CORAL_PEACH -> if (dark)
        darkColorScheme(primary = CoralPeachPrimaryDark, surface = SurfaceDark, onSurface = OnSurfaceDark)
    else
        lightColorScheme(primary = CoralPeachPrimaryLight, surface = SurfaceLight, onSurface = OnSurfaceLight)
}

@Composable
fun FidFanCullrTheme(
    accent: AccentPalette,
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = colorSchemeFor(accent, useDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FidFanTypography,
        content = content
    )
}
