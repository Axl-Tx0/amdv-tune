package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiLyricsResult(
    val found: Boolean,
    val lyrics: String,
    val note: String
)

/**
 * Servicio de búsqueda automática de letras usando Firebase AI Logic (Gemini).
 * Usa salida estructurada JSON con Schema.obj y timeout realista de 45s.
 */
object AiLyricsService {
    private const val TAG = "AiLyricsService"
    private const val TIMEOUT_MS = 45_000L
    private const val MAX_LYRICS_LENGTH = 4800

    private val lyricsSchema = Schema.obj(
        properties = mapOf(
            "found" to Schema.boolean(
                description = "true si reconoces la canción y proporcionas su letra; false si es instrumental o desconocida"
            ),
            "lyrics" to Schema.string(
                description = "Letra completa y bien formateada con saltos de línea (máximo 4800 caracteres), o cadena vacía si found es false"
            ),
            "note" to Schema.string(
                description = "Resumen breve en español del resultado (ej: 'Letra encontrada' o 'Canción instrumental / no encontrada')"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = lyricsSchema
            }
        )
    }

    suspend fun findLyrics(
        artistName: String,
        songTitle: String,
        albumTitle: String = ""
    ): Result<AiLyricsResult> {
        val cleanArtist = cleanQueryField(artistName)
        val cleanTitle = cleanQueryField(songTitle)
        val cleanAlbum = cleanQueryField(albumTitle)

        if (cleanTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Se necesita el nombre de la canción para buscar la letra."))
        }

        val prompt = buildString {
            appendLine("Eres un asistente experto en letras de canciones para un reproductor musical.")
            appendLine("Busca y entrega la letra de la siguiente canción usando exactamente su idioma original:")
            appendLine("- Artista: ${cleanArtist.ifBlank { "Desconocido" }}")
            appendLine("- Canción: $cleanTitle")
            if (cleanAlbum.isNotBlank()) {
                appendLine("- Álbum: $cleanAlbum")
            }
            appendLine()
            appendLine("Instrucciones:")
            appendLine("1. Si conoces la canción, devuelve found=true y la letra bien organizada por estrofas con saltos de línea (\\n).")
            appendLine("2. No incluyas explicaciones ni comentarios dentro del campo 'lyrics', solo la letra (y opcionalmente etiquetas breves como [Estribillo] o [Verso]).")
            appendLine("3. Si la canción es instrumental o no estás seguro de la letra real, NO la inventes: devuelve found=false, lyrics=\"\" y explícalo brevemente en 'note'.")
            appendLine("4. Mantén 'lyrics' por debajo de $MAX_LYRICS_LENGTH caracteres.")
        }

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val rawJson = response.text?.trim()
                ?: return Result.failure(IllegalStateException("La IA devolvió una respuesta vacía."))

            val json = JSONObject(rawJson)
            val found = json.optBoolean("found", false)
            val lyrics = json.optString("lyrics", "").trim().take(MAX_LYRICS_LENGTH)
            val note = json.optString("note", "").trim()
                .ifBlank { if (found && lyrics.isNotBlank()) "Letra encontrada con IA" else "Letra no disponible" }

            Result.success(
                AiLyricsResult(
                    found = found && lyrics.isNotBlank(),
                    lyrics = if (found) lyrics else "",
                    note = note
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI lyrics lookup timed out for $cleanArtist - $cleanTitle", e)
            Result.failure(Exception("La búsqueda de letra con IA tardó demasiado (45s). Intenta de nuevo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI lyrics lookup failed for $cleanArtist - $cleanTitle", e)
            Result.failure(Exception(e.localizedMessage ?: "No se pudo consultar la IA para buscar la letra."))
        }
    }

    private fun cleanQueryField(raw: String): String {
        return raw
            .replace(Regex("\\.(mp3|m4a|wav|flac|ogg|aac|opus)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\d{1,3}[.\\-_\\s]+"), "")
            .replace(Regex("\\((official\\s*(video|audio|lyric\\s*video)|hd|hq|remastered)\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[(official\\s*(video|audio|lyric\\s*video)|hd|hq)\\]", RegexOption.IGNORE_CASE), "")
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
