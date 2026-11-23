package com.example.data

import android.content.Context
import com.example.model.ListeningStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ListeningStatsRepository(context: Context) {
    private val statsFile = File(context.applicationContext.filesDir, "amdv_listening_stats.json")

    suspend fun load(): ListeningStats = withContext(Dispatchers.IO) {
        if (!statsFile.exists()) return@withContext ListeningStats()
        runCatching {
            val raw = statsFile.readText(Charsets.UTF_8)
            val root = JSONObject(raw)
            val recentArr = root.optJSONArray("recent") ?: JSONArray()
            val recent = buildList {
                for (i in 0 until recentArr.length()) {
                    val id = recentArr.optString(i).trim()
                    if (id.isNotEmpty() && !contains(id)) add(id)
                }
            }.take(30)

            val countsObj = root.optJSONObject("counts") ?: JSONObject()
            val counts = mutableMapOf<String, Int>()
            val keys = countsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = countsObj.optInt(key, 0)
                if (key.isNotBlank() && value > 0) {
                    counts[key] = value
                }
            }
            val totalListeningSec = root.optLong("totalListeningSec", 0L).coerceAtLeast(0L)

            val daysArr = root.optJSONArray("activeDays") ?: JSONArray()
            val activeDays = buildList {
                for (i in 0 until daysArr.length()) {
                    val day = daysArr.optString(i).trim()
                    if (day.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) && !contains(day)) {
                        add(day)
                    }
                }
            }.sorted().takeLast(365)

            val offsetsObj = root.optJSONObject("lyricsOffsetsMs") ?: JSONObject()
            val lyricsOffsets = mutableMapOf<String, Long>()
            val offsetKeys = offsetsObj.keys()
            while (offsetKeys.hasNext()) {
                val key = offsetKeys.next()
                val value = offsetsObj.optLong(key, 0L).coerceIn(-15_000L, 15_000L)
                if (key.isNotBlank() && value != 0L) {
                    lyricsOffsets[key] = value
                }
            }

            ListeningStats(
                recentTrackIds = recent,
                playCounts = counts,
                totalListeningSec = totalListeningSec,
                activeDays = activeDays,
                lyricsOffsetsMs = lyricsOffsets
            )
        }.getOrDefault(ListeningStats())
    }

    suspend fun save(stats: ListeningStats) = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            val recentArr = JSONArray()
            stats.recentTrackIds.take(30).forEach { recentArr.put(it) }
            root.put("recent", recentArr)

            val countsObj = JSONObject()
            stats.playCounts.forEach { (trackId, count) ->
                if (trackId.isNotBlank() && count > 0) {
                    countsObj.put(trackId, count)
                }
            }
            root.put("counts", countsObj)
            root.put("totalListeningSec", stats.totalListeningSec.coerceAtLeast(0L))

            val daysArr = JSONArray()
            stats.activeDays.distinct().sorted().takeLast(365).forEach { daysArr.put(it) }
            root.put("activeDays", daysArr)

            val offsetsObj = JSONObject()
            stats.lyricsOffsetsMs.forEach { (trackId, offsetMs) ->
                if (trackId.isNotBlank() && offsetMs != 0L) {
                    offsetsObj.put(trackId, offsetMs.coerceIn(-15_000L, 15_000L))
                }
            }
            root.put("lyricsOffsetsMs", offsetsObj)

            val tmp = File(statsFile.parentFile, "${statsFile.name}.tmp")
            tmp.writeText(root.toString(), Charsets.UTF_8)
            if (!tmp.renameTo(statsFile)) {
                statsFile.writeText(root.toString(), Charsets.UTF_8)
                tmp.delete()
            }
        }
    }
}
