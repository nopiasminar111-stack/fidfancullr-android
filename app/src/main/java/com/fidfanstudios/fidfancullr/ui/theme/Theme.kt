package com.fidfanstudios.fidfancullr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.fidfanstudios.fidfancullr.data.AccentPalette
import com.fidfanstudios.fidfancullr.data.ThemeMode

private fun staticColorSchemeFor(accent: AccentPalette, dark: Boolean) = when (accent) {
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

/**
 * Pixel-style theming: on Android 12+ (API 31+) colors are derived from the
 * device wallpaper (dynamic color), matching how Pixel's own apps behave.
 * On older Android versions, or if the user turned dynamic color off, we
 * fall back to one of the four hand-picked accent palettes.
 */
@Composable
fun FidFanCullrTheme(
    accent: AccentPalette,
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = if (useDynamicColor && dynamicAvailable) {
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        staticColorSchemeFor(accent, useDark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FidFanTypography,
        shapes = FidFanShapes,
        content = content
    )
}
