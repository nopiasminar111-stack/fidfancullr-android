package com.fidfanstudios.fidfancullr.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.fidfanstudios.fidfancullr.data.PhotoFile
import com.fidfanstudios.fidfancullr.data.PhotoFormats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class PreviewResult {
    data class Success(val bitmap: Bitmap) : PreviewResult()
    object NoPreviewAvailable : PreviewResult()
    data class Failed(val reason: String) : PreviewResult()
}

/**
 * Loads a preview bitmap for a photo, downsampled to fit [reqWidth] x
 * [reqHeight] so a 40+ MP source image never gets fully decoded into memory
 * just to be shown on a phone screen.
 *
 * For JPEGs this is a normal downsampled decode. For RAW files, instead of
 * only reading the small EXIF thumbnail (~160x120), this scans the file for
 * every embedded JPEG segment (RAW containers commonly embed a large
 * "camera LCD" preview alongside the tiny EXIF thumbnail) and picks the
 * largest one available, falling back to the small EXIF thumbnail only if
 * no larger preview is found. This is still not a full sensor-data RAW
 * decode (that needs a native decoder like LibRaw) but it's a meaningfully
 * bigger, sharper image than the old thumbnail-only approach.
 */
object PreviewLoader {

    suspend fun load(context: Context, file: PhotoFile, reqWidth: Int, reqHeight: Int): PreviewResult =
        withContext(Dispatchers.IO) {
            val cacheKey = cacheKeyFor(file, reqWidth, reqHeight)
            BitmapMemoryCache.get(cacheKey)?.let { return@withContext PreviewResult.Success(it) }

            val isRaw = file.extension.lowercase() in PhotoFormats.RAW_EXTENSIONS

            val bitmap = try {
                if (isRaw) {
                    loadLargestEmbeddedJpeg(context, file, reqWidth, reqHeight)
                        ?: loadExifThumbnail(context, file)
                } else {
                    loadDownsampled(context, file, reqWidth, reqHeight)
                }
            } catch (e: Exception) {
                return@withContext PreviewResult.Failed(e.message ?: "Unknown decode error")
            }

            if (bitmap == null) {
                PreviewResult.NoPreviewAvailable
            } else {
                BitmapMemoryCache.put(cacheKey, bitmap)
                PreviewResult.Success(bitmap)
            }
        }

    /** Fire-and-forget warm-up used to preload the next/previous image in the list. */
    suspend fun preload(context: Context, file: PhotoFile, reqWidth: Int, reqHeight: Int) {
        val cacheKey = cacheKeyFor(file, reqWidth, reqHeight)
        if (BitmapMemoryCache.contains(cacheKey)) return
        load(context, file, reqWidth, reqHeight)
    }

    private fun cacheKeyFor(file: PhotoFile, reqWidth: Int, reqHeight: Int) =
        "${file.uri}|${file.lastModified}|${reqWidth}x${reqHeight}"

    private fun loadDownsampled(context: Context, file: PhotoFile, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bytes = context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() } ?: return null
        return decodeSampled(bytes, 0, bytes.size, reqWidth, reqHeight)
    }

    private fun loadExifThumbnail(context: Context, file: PhotoFile): Bitmap? {
        return context.contentResolver.openInputStream(file.uri)?.use { stream ->
            ExifInterface(stream).thumbnailBitmap
        }
    }

    private fun loadLargestEmbeddedJpeg(
        context: Context,
        file: PhotoFile,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val bytes = context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() } ?: return null
        val segment = EmbeddedJpegScanner.findLargest(bytes) ?: return null
        return decodeSampled(bytes, segment.offset, segment.length, reqWidth, reqHeight)
    }

    private fun decodeSampled(bytes: ByteArray, offset: Int, length: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, offset, length, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, reqWidth, reqHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, offset, length, decodeOptions)
    }

    private fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
