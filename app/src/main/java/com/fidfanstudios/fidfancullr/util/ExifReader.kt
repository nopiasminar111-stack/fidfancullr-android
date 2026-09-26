package com.fidfanstudios.fidfancullr.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.fidfanstudios.fidfancullr.data.ExifData
import com.fidfanstudios.fidfancullr.data.PhotoFile
import androidx.exifinterface.media.ExifInterface

/**
 * Reads EXIF metadata for JPEG and common RAW formats. androidx.exifinterface
 * can parse EXIF tags directly out of most RAW containers (NEF, CR2, ARW, DNG,
 * ORF, PEF, RAF, RW2, SRW) without needing a full RAW decoder.
 *
 * Full RAW pixel decoding is intentionally out of scope here: it needs a
 * native library such as LibRaw via the NDK. Instead, for RAW files we show
 * the embedded preview/thumbnail image that virtually every RAW file carries,
 * which is what most mobile culling apps do in practice.
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

    /** Returns a preview bitmap: the full JPEG for JPEGs, or the embedded thumbnail for RAW. */
    fun loadPreviewBitmap(context: Context, file: PhotoFile): Bitmap? {
        return try {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val thumb = exif.thumbnailBitmap
                if (thumb != null) return thumb
                null
            }
        } catch (e: Exception) {
            null
        } ?: runCatching {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
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
