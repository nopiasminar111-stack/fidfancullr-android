package com.fidfanstudios.fidfancullr.util

import android.content.Context
import com.fidfanstudios.fidfancullr.data.ExifData
import com.fidfanstudios.fidfancullr.data.PhotoFile
import androidx.exifinterface.media.ExifInterface

/**
 * Reads EXIF metadata for JPEG and common RAW formats. androidx.exifinterface
 * can parse EXIF tags directly out of most RAW containers (NEF, CR2, ARW, DNG,
 * ORF, PEF, RAF, RW2, SRW) without needing a full RAW decoder.
 *
 * Preview bitmap loading lives in [com.fidfanstudios.fidfancullr.util.PreviewLoader]
 * instead of here, since that needs downsampling, caching and RAW-specific
 * embedded-JPEG scanning that don't belong in a metadata reader.
 */
object ExifReader {

    fun read(context: Context, file: PhotoFile): ExifData {
        return try {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                ExifData(
                    camera = listOfNotNull(
                        exif.getAttribute(ExifInterface.TAG_MAKE),
                        exif.getAttribute(ExifInterface.TAG_MODEL)
                    ).joinToString(" ").ifBlank { null },
                    lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL),
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                        ?.let { "ISO $it" },
                    shutterSpeed = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let(::formatShutter),
                    aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "$it mm" },
                    dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                )
            } ?: ExifData()
        } catch (e: Exception) {
            ExifData()
        }
    }

    private fun formatShutter(exposureTime: String): String {
        val seconds = exposureTime.toFloatOrNull() ?: return exposureTime
        return if (seconds < 1f && seconds > 0f) {
            "1/${Math.round(1f / seconds)}s"
        } else {
            "${seconds}s"
        }
    }
}
