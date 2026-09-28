package com.fidfanstudios.fidfancullr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fidfanstudios.fidfancullr.R

/**
 * Google Sans Flex, bundled as a single variable font file
 * (res/font/google_sans_flex.ttf) with a "wght" axis from 100–900. Each
 * named weight below points at the SAME file with a different
 * FontVariation weight setting, so we get real Light/Regular/Medium/
 * SemiBold/Bold/Black instances from one file instead of faux-bold and
 * instead of shipping duplicate static font files per weight.
 *
 * FontVariation requires API 26+ (this app's minSdk), so no separate
 * fallback path is needed for variable-axis support.
 */
@OptIn(ExperimentalTextApi::class)
private fun flexWeight(weight: FontWeight, wght: Int) = Font(
    resId = R.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(wght))
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    flexWeight(FontWeight.Light, 300),
    flexWeight(FontWeight.Normal, 400),
    flexWeight(FontWeight.Medium, 500),
    flexWeight(FontWeight.SemiBold, 600),
    flexWeight(FontWeight.Bold, 700),
    flexWeight(FontWeight.Black, 900)
)

// Sizes/line-heights re-tuned for Google Sans Flex's metrics (a bit taller
// x-height than the platform default), so headings and body text don't feel
// cramped or clipped compared to the previous system-font values.
val FidFanTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 29.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 25.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp,
        letterSpacing = 0.1.sp
    )
)
