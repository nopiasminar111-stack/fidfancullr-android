package com.fidfanstudios.fidfancullr.util

import android.graphics.Bitmap
import android.util.LruCache

/**
 * In-memory LRU cache for decoded preview bitmaps, sized relative to the
 * app's available heap (the standard Android-recommended pattern) so large
 * RAW previews can't accumulate and cause an OutOfMemoryError. Bitmaps are
 * measured by their actual byte size, not by count, since preview sizes
 * vary a lot between a JPEG thumbnail fallback and a full RAW preview.
 */
object BitmapMemoryCache {
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = maxMemoryKb / 8

    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun contains(key: String): Boolean = cache.get(key) != null

    fun clear() = cache.evictAll()
}
