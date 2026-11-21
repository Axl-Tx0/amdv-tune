package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class LrcLine(
    val timeMs: Long,
    val text: String
)

data class LrcLibFetchResult(
    val lyrics: String,
    val isSynced: Boolean,
    val isInstrumental: Boolean = false
)

object LrcParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val METADATA_TAG_REGEX = Regex("""^\[[a-zA-Z]+:.*]$""")

    fun parseSyncedLyrics(raw: String): List<LrcLine> {
        if (raw.isBlank()) return emptyList()
        val parsed = mutableListOf<LrcLine>()
        raw.lineSequence().forEach { originalLine ->
            val trimmed = originalLine.trim()
            if (trimmed.isEmpty() || METADATA_TAG_REGEX.matches(trimmed)) return@forEach
            val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isEmpty()) return@forEach
            val textPart = TIMESTAMP_REGEX.replace(trimmed, "").trim()
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = match.groupValues[3]
                val millis = when (fracRaw.length) {
                    0 -> 0L
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    else -> fracRaw.take(3).toLongOrNull() ?: 0L
                }
                val timeMs = (minutes * 60_000L) + (seconds * 1_000L) + millis
                parsed.add(LrcLine(timeMs = timeMs, text = textPart))
            }
        }
        val sorted = parsed
            .filter { it.text.isNotBlank() }
            .sortedBy { it.timeMs }
        return if (sorted.size >= 2) sorted else emptyList()
    }

    fun hasSyncedTimestamps(raw: String): Boolean = parseSyncedLyrics(raw).size >= 2

    fun findActiveLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        val adjustedPos = positionMs + 150L
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= adjustedPos) {
                active = i
            } else {
                break
            }
        }
        return active
    }

    fun stripTimestamps(raw: String): String {
        if (raw.isBlank()) return ""
        val lines = raw.lineSequence()
            .map { it.trim() }
            .filterNot { METADATA_TAG_REGEX.matches(it) }
            .map { TIMESTAMP_REGEX.replace(it, "").trim() }
            .toList()
        return lines.joinToString("\n").trim()
    }

    fun trimToMaxChars(raw: String, maxChars: Int = 5000): String {
        val trimmed = raw.trim()
        if (trimmed.length <= maxChars) return trimmed
        val cut = trimmed.take(maxChars)
        val lastNewline = cut.lastIndexOf('\n')
        return if (lastNewline > maxChars / 2) cut.substring(0, lastNewline).trim() else cut.trim()
    }
}

object LrcLibService {
    private const val BASE_URL = "https://lrclib.net/api"
    private const val USER_AGENT = "AMDV-TUNE/1.4 (https://github.com/Axl-Tx0/amdv-tune)"
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 7_000

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String = "",
        durationSec: Long = 0L
    ): Result<LrcLibFetchResult?> = withContext(Dispatchers.IO) {
        try {
            val rawTitle = title.trim()
            val rawArtist = artist.trim()
            if (rawTitle.isBlank()) {
                return@withContext Result.success(null)
            }

            val cleanTitle = cleanQueryText(rawTitle)
            val cleanArtist = cleanArtistText(rawArtist)
            val cleanAlbum = album.trim()

            // 1. Exact lookup via /api/get when artist is known
            if (cleanArtist.isNotBlank()) {
                val exact = requestExactGet(
                    trackName = cleanTitle.ifBlank { rawTitle },
                    artistName = cleanArtist,
                    albumName = cleanAlbum,
                    durationSec = durationSec
                )
                if (exact != null && (exact.lyrics.isNotBlank() || exact.isInstrumental)) {
                    return@withContext Result.success(exact)
                }
            }

            // 2. Structured search via /api/search?track_name=...&artist_name=...
            if (cleanArtist.isNotBlank()) {
                val structuredUrl = buildString {
                    append("$BASE_URL/search?track_name=${encode(cleanTitle.ifBlank { rawTitle })}")
                    append("&artist_name=${encode(cleanArtist)}")
                }
                val structuredMatch = searchAndPickBest(
                    urlStr = structuredUrl,
                    targetTitle = cleanTitle.ifBlank { rawTitle },
                    targetArtist = cleanArtist,
                    durationSec = durationSec
                )
                if (structuredMatch != null) {
                    return@withContext Result.success(structuredMatch)
                }
            }

            // 3. Broad fallback search via /api/search?q=...
            val broadQuery = listOf(cleanTitle.ifBlank { rawTitle }, cleanArtist)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            if (broadQuery.isNotBlank()) {
                val broadUrl = "$BASE_URL/search?q=${encode(broadQuery)}"
                val broadMatch = searchAndPickBest(
                    urlStr = broadUrl,
                    targetTitle = cleanTitle.ifBlank { rawTitle },
                    targetArtist = cleanArtist,
                    durationSec = durationSec
                )
                if (broadMatch != null) {
                    return@withContext Result.success(broadMatch)
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con LRCLIB: ${e.localizedMessage ?: "sin red"}"))
        }
    }

    private fun requestExactGet(
        trackName: String,
        artistName: String,
        albumName: String,
        durationSec: Long
    ): LrcLibFetchResult? {
        val urlStr = buildString {
            append("$BASE_URL/get?track_name=${encode(trackName)}")
            append("&artist_name=${encode(artistName)}")
            if (albumName.isNotBlank()) {
                append("&album_name=${encode(albumName)}")
            }
            if (durationSec in 10L..3600L) {
                append("&duration=$durationSec")
            }
        }
        val body = httpGetOrNull(urlStr) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        return extractFromJson(json)
    }

    private fun searchAndPickBest(
        urlStr: String,
        targetTitle: String,
        targetArtist: String,
        durationSec: Long
    ): LrcLibFetchResult? {
        val body = httpGetOrNull(urlStr) ?: return null
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return null
        if (array.length() == 0) return null

        val normTitle = normalizeForCompare(targetTitle)
        val normArtist = normalizeForCompare(targetArtist)

        var bestResult: LrcLibFetchResult? = null
        var bestScore = Int.MIN_VALUE

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val extracted = extractFromJson(item) ?: continue
            if (extracted.lyrics.isBlank() && !extracted.isInstrumental) continue

            var score = 0
            if (extracted.isSynced) score += 120
            if (extracted.lyrics.isNotBlank()) score += 40
            if (extracted.isInstrumental) score -= 50

            val candTitle = normalizeForCompare(item.optNullableString("trackName"))
            val candArtist = normalizeForCompare(item.optNullableString("artistName"))

            if (normTitle.isNotBlank() && candTitle == normTitle) {
                score += 60
            } else if (normTitle.isNotBlank() && (candTitle.contains(normTitle) || normTitle.contains(candTitle))) {
                score += 30
            }

            if (normArtist.isNotBlank() && candArtist == normArtist) {
                score += 50
            } else if (normArtist.isNotBlank() && (candArtist.contains(normArtist) || normArtist.contains(candArtist))) {
                score += 25
            }

            val candDuration = item.optDouble("duration", -1.0)
            if (durationSec > 0L && candDuration > 0.0) {
                val diff = abs(candDuration - durationSec.toDouble())
                when {
                    diff <= 3.0 -> score += 50
                    diff <= 8.0 -> score += 25
                    diff <= 20.0 -> score += 5
                    else -> score -= 20
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestResult = extracted
            }
        }
        return bestResult
    }

    private fun extractFromJson(obj: JSONObject): LrcLibFetchResult? {
        val synced = obj.optNullableString("syncedLyrics").trim()
        val plain = obj.optNullableString("plainLyrics").trim()
        val instrumental = obj.optBoolean("instrumental", false)

        return when {
            synced.isNotBlank() && LrcParser.hasSyncedTimestamps(synced) -> {
                LrcLibFetchResult(
                    lyrics = LrcParser.trimToMaxChars(synced, 5000),
                    isSynced = true,
                    isInstrumental = false
                )
            }
            plain.isNotBlank() -> {
                LrcLibFetchResult(
                    lyrics = LrcParser.trimToMaxChars(plain, 5000),
                    isSynced = false,
                    isInstrumental = false
                )
            }
            instrumental -> {
                LrcLibFetchResult(
                    lyrics = "",
                    isSynced = false,
                    isInstrumental = true
                )
            }
            else -> null
        }
    }

    private fun httpGetOrNull(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                null
            } else {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun JSONObject.optNullableString(key: String): String {
        if (isNull(key)) return ""
        return optString(key, "")
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun cleanQueryText(raw: String): String {
        return raw
            .replace(Regex("""\.(mp3|m4a|wav|flac|ogg|aac)$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^\d{1,3}\s*[-._)]\s*"""), "")
            .replace(
                Regex(
                    """[\(\[]\s*(official\s*(music\s*)?video|official\s*audio|audio\s*oficial|video\s*oficial|lyric\s*video|lyrics|letra|hd|hq|4k|remastered(\s*\d{4})?|visualizer)\s*[\)\]]""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(Regex("""\s+(feat\.?|ft\.?)\s+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun cleanArtistText(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.equals("Artista desconocido", ignoreCase = true) ||
            trimmed.equals("Unknown Artist", ignoreCase = true) ||
            trimmed.equals("<unknown>", ignoreCase = true) ||
            trimmed.equals("Artista", ignoreCase = true)
        ) {
            return ""
        }
        return trimmed
            .replace(Regex("""\s*(feat\.?|ft\.?|x|&|,)\s+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun normalizeForCompare(raw: String): String {
        return raw.lowercase()
            .replace(Regex("""[^\p{L}\p{Nd}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}

class LrcLyricsCache(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAll(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        prefs.all.forEach { (k, v) ->
            if (v is String && v.isNotBlank()) {
                result[k] = v
            }
        }
        return result
    }

    fun save(trackId: String, lyrics: String) {
        if (trackId.isBlank()) return
        val editor = prefs.edit()
        if (lyrics.isBlank()) {
            editor.remove(trackId)
        } else {
            editor.putString(trackId, lyrics)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "amdv_lrclib_lyrics_cache"
    }
}
