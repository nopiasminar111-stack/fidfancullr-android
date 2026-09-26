package com.fidfanstudios.fidfancullr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// NOTE on Google Sans Flex: using it for real needs either (a) the actual
// .ttf files bundled under res/font/, or (b) Google Play Services'
// Downloadable Fonts API, which requires a provider certificate hash that
// must exactly match Google's signing certificate — a wrong hand-typed
// value silently breaks font loading rather than failing loudly. Neither
// can be produced reliably without network access from this environment,
// so this uses the platform's default sans-serif for now, tuned with the
// bolder weights and larger sizes M3 Expressive favors. See README for how
// to drop in the real Google Sans Flex files later (it's a two-file change:
// add the .ttf files under res/font/, then reference them here).
private val ExpressiveFontFamily = FontFamily.SansSerif

val FidFanTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ExpressiveFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = ExpressiveFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ExpressiveFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ExpressiveFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ExpressiveFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ExpressiveFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ExpressiveFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ExpressiveFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp
    )
)
