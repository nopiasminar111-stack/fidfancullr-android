package com.fidfanstudios.fidfancullr.data

import android.net.Uri

/** A single photo-related file (JPEG or a RAW variant) inside the inbox folder. */
data class PhotoFile(
    val uri: Uri,
    val documentId: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long
)

/** A group of files that share the same filename stem, e.g. DSC_1042.JPG + DSC_1042.NEF */
data class PhotoGroup(
    val stem: String,
    val files: List<PhotoFile>
) {
    val primaryFile: PhotoFile
        get() = files.firstOrNull { it.extension.lowercase() in PhotoFormats.JPEG_EXTENSIONS }
            ?: files.first()

    val hasRaw: Boolean
        get() = files.any { it.extension.lowercase() in PhotoFormats.RAW_EXTENSIONS }

    val hasJpeg: Boolean
        get() = files.any { it.extension.lowercase() in PhotoFormats.JPEG_EXTENSIONS }
}

object PhotoFormats {
    // Common RAW formats across major camera brands.
    val RAW_EXTENSIONS = setOf(
        "nef", "nrw",        // Nikon
        "cr2", "cr3",        // Canon
        "arw", "srf", "sr2", // Sony
        "raf",               // Fujifilm
        "orf",               // Olympus
        "rw2",               // Panasonic
        "pef",               // Pentax
        "srw",               // Samsung
        "dng",               // Adobe / generic
        "raw", "3fr", "erf"  // misc
    )
    val JPEG_EXTENSIONS = setOf("jpg", "jpeg")
    val SUPPORTED_EXTENSIONS = RAW_EXTENSIONS + JPEG_EXTENSIONS
}

data class ExifData(
    val camera: String? = null,
    val lens: String? = null,
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val dateTime: String? = null
)

data class SortDestination(
    val id: String,
    val label: String,
    val folderName: String,
    val gestureKey: String // digit key or swipe binding, user-configurable
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class ColorTag { RED, YELLOW, GREEN, NONE }

enum class AccentPalette {
    VIOLET_PIXEL, OCEAN_BLUE, MINT_GREEN, CORAL_PEACH
}

val DEFAULT_DESTINATIONS = listOf(
    SortDestination("keep", "KEEP", "KEEP", "1"),
    SortDestination("select", "SELECT", "SELECT", "2"),
    SortDestination("reject", "REJECT", "REJECT", "3")
)

data class AppSettings(
    val inboxUri: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.VIOLET_PIXEL,
    // Pixel-style wallpaper-based color, on by default like stock Pixel apps.
    // Only takes effect on Android 12+; ignored (falls back to accentPalette)
    // on older versions.
    val useDynamicColor: Boolean = true,
    val language: String = "en",
    val destinations: List<SortDestination> = DEFAULT_DESTINATIONS,
    val onboardingComplete: Boolean = false,
    val pureBlackTheme: Boolean = false,
    val hapticFeedback: Boolean = true,
    val xmpSidecars: Boolean = true,
    val customPrefix: String = "",
    val customSuffix: String = ""
)
