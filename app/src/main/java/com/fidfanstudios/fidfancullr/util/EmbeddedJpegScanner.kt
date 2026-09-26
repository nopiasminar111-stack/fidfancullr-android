package com.fidfanstudios.fidfancullr.util

import android.graphics.BitmapFactory

/**
 * Scans a raw byte buffer for embedded JPEG images (delimited by the
 * standard FFD8...FFD9 start/end-of-image markers) and reports the largest
 * one by pixel area. Most camera RAW formats (NEF, CR2/CR3, ARW, DNG, RAF,
 * ORF, RW2, PEF, SRW) embed at least one sizeable JPEG preview for the
 * camera's own LCD/EVF display, in addition to the small EXIF thumbnail —
 * this is what lets us show something much bigger than a thumbnail without
 * a full native RAW decoder.
 */
object EmbeddedJpegScanner {

    data class JpegSegment(val offset: Int, val length: Int, val width: Int, val height: Int)

    /** Skip segments smaller than this many bytes; too small to be a useful preview. */
    private const val MIN_SEGMENT_BYTES = 8 * 1024

    fun findLargest(bytes: ByteArray): JpegSegment? {
        var best: JpegSegment? = null
        var i = 0
        val limit = bytes.size - 1

        while (i < limit) {
            val isSoi = (bytes[i].toInt() and 0xFF) == 0xFF && (bytes[i + 1].toInt() and 0xFF) == 0xD8
            if (!isSoi) {
                i++
                continue
            }

            val start = i
            var j = i + 2
            var end = -1
            while (j < limit) {
                if ((bytes[j].toInt() and 0xFF) == 0xFF && (bytes[j + 1].toInt() and 0xFF) == 0xD9) {
                    end = j + 1
                    break
                }
                j++
            }

            if (end == -1) break // no matching EOI found; stop scanning
            val length = end - start + 1

            if (length >= MIN_SEGMENT_BYTES) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, start, length, bounds)
                if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                    val area = bounds.outWidth.toLong() * bounds.outHeight.toLong()
                    val bestArea = best?.let { it.width.toLong() * it.height.toLong() } ?: -1L
                    if (area > bestArea) {
                        best = JpegSegment(start, length, bounds.outWidth, bounds.outHeight)
                    }
                }
            }

            i = end + 1
        }

        return best
    }
}
