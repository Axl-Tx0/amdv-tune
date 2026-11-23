package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CoverColorExtractor {
    private val cache = LruCache<String, Int>(64)

    suspend fun extractAccentColor(
        context: Context,
        coverKey: String,
        fallback: Color
    ): Color {
        if (coverKey.isBlank()) return fallback
        cache.get(coverKey)?.let { cachedArgb ->
            return Color(cachedArgb)
        }

        val extracted = withContext(Dispatchers.Default) {
            if (coverKey.startsWith("MONO_")) {
                return@withContext monogramAccentForKey(coverKey) ?: fallback
            }
            val bitmap = loadSmallBitmap(context, coverKey) ?: return@withContext fallback
            val color = pickVibrantColor(bitmap) ?: fallback
            if (!bitmap.isRecycled) {
                runCatching { bitmap.recycle() }
            }
            color
        }

        cache.put(coverKey, extracted.toArgb())
        return extracted
    }

    private fun monogramAccentForKey(coverKey: String): Color? {
        return when {
            coverKey.startsWith("MONO_ROSE_") -> Color(0xFFFF2A85)
            coverKey.startsWith("MONO_AMBER_") -> Color(0xFFFF9800)
            coverKey.startsWith("MONO_EMERALD_") -> Color(0xFF00E676)
            coverKey.startsWith("MONO_VIOLET_") -> Color(0xFF9C27B0)
            coverKey.startsWith("MONO_CYAN_") -> Color(0xFF00E5FF)
            else -> null
        }
    }

    private suspend fun loadSmallBitmap(context: Context, coverKey: String): Bitmap? {
        return runCatching {
            when {
                coverKey.startsWith("http://", ignoreCase = true) ||
                    coverKey.startsWith("https://", ignoreCase = true) -> {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(coverKey)
                        .size(48, 48)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    val drawable = (result as? SuccessResult)?.drawable
                    val bmp = (drawable as? BitmapDrawable)?.bitmap ?: return null
                    bmp.copy(Bitmap.Config.ARGB_8888, false)
                }
                coverKey.startsWith("/") && File(coverKey).exists() -> {
                    decodeSampledFile(coverKey)
                }
                coverKey.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(coverKey))?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }
                else -> {
                    val resId = context.resources.getIdentifier(coverKey, "drawable", context.packageName)
                    if (resId != 0) {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeResource(context.resources, resId, opts)
                    } else {
                        null
                    }
                }
            }
        }.getOrNull()
    }

    private fun decodeSampledFile(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        val sample = (maxDim / 48).coerceAtLeast(1)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(path, opts)
    }

    private fun pickVibrantColor(bitmap: Bitmap): Color? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null

        val stepX = (w / 16).coerceAtLeast(1)
        val stepY = (h / 16).coerceAtLeast(1)
        val hsv = FloatArray(3)

        var bestScore = -1f
        var bestR = 0
        var bestG = 0
        var bestB = 0

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = AndroidColor.alpha(pixel)
                if (alpha >= 200) {
                    val r = AndroidColor.red(pixel)
                    val g = AndroidColor.green(pixel)
                    val b = AndroidColor.blue(pixel)
                    sumR += r
                    sumG += g
                    sumB += b
                    count++

                    AndroidColor.RGBToHSV(r, g, b, hsv)
                    val sat = hsv[1]
                    val value = hsv[2]
                    // Prefer rich, vivid tones that aren't near-black or blown-out white
                    if (value in 0.22f..0.95f && sat >= 0.25f) {
                        val valueWeight = 1f - kotlin.math.abs(value - 0.65f)
                        val score = sat * 1.6f + valueWeight * 0.7f
                        if (score > bestScore) {
                            bestScore = score
                            bestR = r
                            bestG = g
                            bestB = b
                        }
                    }
                }
                x += stepX
            }
            y += stepY
        }

        if (bestScore > 0f) {
            AndroidColor.RGBToHSV(bestR, bestG, bestB, hsv)
            hsv[1] = hsv[1].coerceIn(0.45f, 0.92f)
            hsv[2] = hsv[2].coerceIn(0.42f, 0.85f)
            return Color(AndroidColor.HSVToColor(hsv))
        }

        if (count > 0) {
            val avgR = (sumR / count).toInt().coerceIn(0, 255)
            val avgG = (sumG / count).toInt().coerceIn(0, 255)
            val avgB = (sumB / count).toInt().coerceIn(0, 255)
            AndroidColor.RGBToHSV(avgR, avgG, avgB, hsv)
            hsv[1] = hsv[1].coerceAtLeast(0.28f)
            hsv[2] = hsv[2].coerceIn(0.35f, 0.75f)
            return Color(AndroidColor.HSVToColor(hsv))
        }
        return null
    }
}
