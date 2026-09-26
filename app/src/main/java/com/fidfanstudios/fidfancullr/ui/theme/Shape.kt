package com.fidfanstudios.fidfancullr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive leans into bigger, bolder corner radii than "classic" Material 3.
 * These values are noticeably rounder than the M3 defaults (4/8/12/16/28dp).
 */
val FidFanShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/** Pill shape used for the big sort-action buttons and chips, Pixel-style. */
val PillShape = RoundedCornerShape(50)
