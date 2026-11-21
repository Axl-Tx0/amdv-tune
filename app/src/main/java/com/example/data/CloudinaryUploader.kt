package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Cliente de subida a Cloudinary para el Panel Admin de AMDV-TUNE.
 *
 * Soporta dos modos automáticos (configurados vía panel Secrets de AI Studio / BuildConfig):
 * 1. Modo Backend Propio Firmado (Recomendado): usa UPLOAD_SIGNER_URL (Render), valida la sesión
 *    de Firebase Auth (amdvtune@gmail.com) en el servidor y obtiene una firma temporal SHA-1.
 * 2. Modo Directo Unsigned (Alternativo): usa CLOUDINARY_CLOUD_NAME y CLOUDINARY_UPLOAD_PRESET.
 */
object CloudinaryUploader {

    private const val PLACEHOLDER_SIGNER_URL = "UPLOAD_SIGNER_URL_DEFAULT_VALUE"
    private const val PLACEHOLDER_CLOUD_NAME = "CLOUDINARY_CLOUD_NAME_DEFAULT_VALUE"
    private const val PLACEHOLDER_UPLOAD_PRESET = "CLOUDINARY_UPLOAD_PRESET_DEFAULT_VALUE"

    // Render Free puede tardar ~30-50s en despertar tras inactividad
    private const val SIGNER_CONNECT_TIMEOUT_MS = 60_000
    private const val SIGNER_READ_TIMEOUT_MS = 75_000
    private const val UPLOAD_CONNECT_TIMEOUT_MS = 20_000
    private const val UPLOAD_READ_TIMEOUT_MS = 180_000

    private val rawSignerUrl: String
        get() = BuildConfig.UPLOAD_SIGNER_URL.trim()

    private val fallbackCloudName: String
        get() = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()

    private val fallbackUploadPreset: String
        get() = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()

    fun isSignedBackendConfigured(): Boolean {
        val url = rawSignerUrl
        return url.isNotBlank() &&
            url != PLACEHOLDER_SIGNER_URL &&
            !url.startsWith("YOUR_", ignoreCase = true) &&
            (url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true))
    }

    fun isUnsignedConfigured(): Boolean {
        val cn = fallbackCloudName
        val preset = fallbackUploadPreset
        return cn.isNotBlank() &&
            preset.isNotBlank() &&
            cn != PLACEHOLDER_CLOUD_NAME &&
            preset != PLACEHOLDER_UPLOAD_PRESET &&
            !cn.startsWith("YOUR_", ignoreCase = true) &&
            !preset.startsWith("YOUR_", ignoreCase = true)
    }

    fun isConfigured(): Boolean = isSignedBackendConfigured() || isUnsignedConfigured()

    private data class SignedCredentials(
        val cloudName: String,
        val apiKey: String,
        val timestamp: Long,
        val signature: String
    )

    suspend fun uploadUri(
        context: Context,
        uri: Uri,
        folder: String,
        publicId: String,
        fileName: String,
        contentType: String,
        isAudio: Boolean,
        onProgress: (Float) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val totalSize = queryUriSize(context, uri)
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir el archivo seleccionado del dispositivo.")
        inputStream.use { stream ->
            uploadStream(
                inputStream = stream,
                totalBytes = totalSize,
                folder = folder,
                publicId = publicId,
                fileName = fileName,
                contentType = contentType,
                isAudio = isAudio,
                onProgress = onProgress
            )
        }
    }

    suspend fun uploadBytes(
        bytes: ByteArray,
        folder: String,
        publicId: String,
        fileName: String,
        contentType: String,
        isAudio: Boolean,
        onProgress: (Float) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        ByteArrayInputStream(bytes).use { stream ->
            uploadStream(
                inputStream = stream,
                totalBytes = bytes.size.toLong(),
                folder = folder,
                publicId = publicId,
                fileName = fileName,
                contentType = contentType,
                isAudio = isAudio,
                onProgress = onProgress
            )
        }
    }

    private suspend fun requestUploadSignature(
        folder: String,
        publicId: String
    ): SignedCredentials = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Debes iniciar sesión con tu cuenta Admin para subir archivos.")

        val base = rawSignerUrl.trim().trimEnd('/')
        val signEndpoint = if (base.endsWith("/sign-upload", ignoreCase = true)) {
            base
        } else {
            "$base/sign-upload"
        }

        // Si el primer token fallara con 401 (expirado), reintentamos una vez forzando renovación
        var lastError: Exception? = null
        for (attempt in 0..1) {
            coroutineContext.ensureActive()
            val forceRefresh = attempt == 1
            val idToken = user.getIdToken(forceRefresh).await().token
                ?: throw IllegalStateException("No se pudo obtener el token de sesión de Firebase.")

            val connection = (URL(signEndpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                useCaches = false
                connectTimeout = SIGNER_CONNECT_TIMEOUT_MS
                readTimeout = SIGNER_READ_TIMEOUT_MS
                setRequestProperty("Authorization", "Bearer $idToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            try {
                val payload = JSONObject().apply {
                    if (folder.isNotBlank()) put("folder", folder)
                    if (publicId.isNotBlank()) put("public_id", publicId)
                }.toString()

                connection.outputStream.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                    out.flush()
                }

                val statusCode = connection.responseCode
                val responseStream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val body = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

                if (statusCode == 401 && attempt == 0) {
                    lastError = IllegalStateException(parseBackendError(statusCode, body))
                    continue
                }
                if (statusCode !in 200..299) {
                    throw IllegalStateException(parseBackendError(statusCode, body))
                }

                val json = JSONObject(body)
                val cloudName = json.optString("cloudName").trim()
                val apiKey = json.optString("apiKey").trim()
                val timestamp = json.optLong("timestamp", 0L)
                val signature = json.optString("signature").trim()
                if (cloudName.isBlank() || apiKey.isBlank() || timestamp <= 0L || signature.isBlank()) {
                    throw IllegalStateException("Tu backend devolvió una respuesta incompleta al firmar la subida.")
                }
                return@withContext SignedCredentials(
                    cloudName = cloudName,
                    apiKey = apiKey,
                    timestamp = timestamp,
                    signature = signature
                )
            } finally {
                connection.disconnect()
            }
        }

        throw lastError ?: IllegalStateException("No se pudo autenticar con tu backend de Render.")
    }

    private suspend fun uploadStream(
        inputStream: InputStream,
        totalBytes: Long,
        folder: String,
        publicId: String,
        fileName: String,
        contentType: String,
        isAudio: Boolean,
        onProgress: (Float) -> Unit
    ): String = withContext(Dispatchers.IO) {
        check(isConfigured()) {
            "Configura UPLOAD_SIGNER_URL (la URL de tu backend en Render) en el panel Secrets de AI Studio."
        }

        val safeFolder = folder.trim()
        val safePublicId = publicId
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(80)
            .ifBlank { "file_${System.currentTimeMillis()}" }
        val safeFileName = fileName
            .replace("\"", "")
            .replace("\r", "")
            .replace("\n", "")
            .ifBlank { if (isAudio) "$safePublicId.mp3" else "$safePublicId.jpg" }
        val safeContentType = contentType.ifBlank {
            if (isAudio) "audio/mpeg" else "image/jpeg"
        }

        val signed: SignedCredentials?
        val targetCloudName: String
        if (isSignedBackendConfigured()) {
            onProgress(0.02f)
            signed = requestUploadSignature(folder = safeFolder, publicId = safePublicId)
            targetCloudName = signed.cloudName
        } else {
            signed = null
            targetCloudName = fallbackCloudName
        }

        val resourceType = if (isAudio) "video" else "image"
        val encodedCloud = URLEncoder.encode(targetCloudName, "UTF-8")
        val endpoint = URL("https://api.cloudinary.com/v1_1/$encodedCloud/$resourceType/upload")
        val boundary = "----AmdvTuneBoundary${UUID.randomUUID().toString().replace("-", "")}"
        val crlf = "\r\n"

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            useCaches = false
            connectTimeout = UPLOAD_CONNECT_TIMEOUT_MS
            readTimeout = UPLOAD_READ_TIMEOUT_MS
            setChunkedStreamingMode(64 * 1024)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
        }

        try {
            BufferedOutputStream(connection.outputStream, 64 * 1024).use { out ->
                fun writeFormField(name: String, value: String) {
                    val part = buildString {
                        append("--").append(boundary).append(crlf)
                        append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(crlf)
                        append(crlf)
                        append(value).append(crlf)
                    }
                    out.write(part.toByteArray(Charsets.UTF_8))
                }

                if (signed != null) {
                    writeFormField("api_key", signed.apiKey)
                    writeFormField("timestamp", signed.timestamp.toString())
                    writeFormField("signature", signed.signature)
                } else {
                    writeFormField("upload_preset", fallbackUploadPreset)
                }

                if (safeFolder.isNotBlank()) {
                    writeFormField("folder", safeFolder)
                }
                writeFormField("public_id", safePublicId)

                val fileHeader = buildString {
                    append("--").append(boundary).append(crlf)
                    append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(safeFileName).append("\"").append(crlf)
                    append("Content-Type: ").append(safeContentType).append(crlf)
                    append(crlf)
                }
                out.write(fileHeader.toByteArray(Charsets.UTF_8))

                val buffer = ByteArray(16 * 1024)
                var uploaded = 0L
                var lastReported = -1f
                while (true) {
                    coroutineContext.ensureActive()
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    uploaded += read
                    val pct = if (totalBytes > 0L) {
                        (uploaded.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.95f)
                    } else {
                        0.5f
                    }
                    if (pct - lastReported >= 0.03f) {
                        lastReported = pct
                        onProgress(pct)
                    }
                }

                out.write(crlf.toByteArray(Charsets.UTF_8))
                out.write("--$boundary--$crlf".toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val statusCode = connection.responseCode
            val responseStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val body = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (statusCode !in 200..299) {
                throw IllegalStateException(parseCloudinaryError(statusCode, body))
            }

            val json = JSONObject(body)
            val secureUrl = json.optString("secure_url").trim()
            if (secureUrl.isBlank()) {
                throw IllegalStateException("Cloudinary no devolvió la URL del archivo subido.")
            }
            onProgress(1f)
            secureUrl
        } finally {
            connection.disconnect()
        }
    }

    private fun queryUriSize(context: Context, uri: Uri): Long {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !cursor.isNull(idx)) {
                        val size = cursor.getLong(idx)
                        if (size > 0L) return size
                    }
                }
            }
        } catch (_: Exception) {
        }
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    private fun parseBackendError(statusCode: Int, body: String): String {
        val rawMsg = try {
            JSONObject(body).optString("error").trim()
        } catch (_: Exception) {
            ""
        }
        return when {
            rawMsg.isNotBlank() -> "Backend Render ($statusCode): $rawMsg"
            statusCode == 404 -> "No se encontró /sign-upload en tu backend de Render. Verifica UPLOAD_SIGNER_URL en Secrets."
            statusCode == 403 -> "Solo la cuenta Admin (amdvtune@gmail.com) tiene permiso para firmar subidas."
            statusCode == 502 || statusCode == 503 -> "Tu servidor en Render está despertando. Intenta nuevamente en unos segundos."
            else -> "Error al conectar con tu backend de Render (código HTTP $statusCode)."
        }
    }

    private fun parseCloudinaryError(statusCode: Int, body: String): String {
        val rawMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (_: Exception) {
            ""
        }
        val lower = rawMsg.lowercase()
        return when {
            lower.contains("invalid signature") ->
                "Firma inválida de Cloudinary. Verifica CLOUDINARY_API_SECRET en Render."
            lower.contains("upload preset") || lower.contains("whitelisted") ->
                "El Upload Preset de Cloudinary no existe o no está en modo 'Unsigned'."
            lower.contains("cloud_name") || statusCode == 401 ->
                "Credenciales de Cloudinary inválidas. Revisa CLOUDINARY_CLOUD_NAME, API_KEY y API_SECRET en Render."
            lower.contains("file size too large") || statusCode == 413 ->
                "El archivo supera el límite gratuito de Cloudinary (10 MB imágenes / 100 MB audio)."
            rawMsg.isNotBlank() ->
                "Error de Cloudinary ($statusCode): $rawMsg"
            else ->
                "Error al subir a Cloudinary (código HTTP $statusCode). Verifica tu conexión."
        }
    }
}
