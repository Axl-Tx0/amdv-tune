package com.example.data

import com.example.model.AppThemeMode
import com.example.model.CloudTrack
import com.example.model.CloudUserProfile
import com.example.model.TrackUiModel
import com.example.model.UserPlaylist
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para sincronizar.")
        check(uid == expectedUserId) { "La sesión activa cambió; operación cancelada." }
        return uid
    }

    fun observeUserProfile(userId: String): Flow<CloudUserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        CloudUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(toLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    fun observeUserTracks(userId: String): Flow<List<CloudTrack>> {
        val colRef = db.collection("users").document(userId).collection("tracks")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudTrack::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        themeMode: AppThemeMode
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUid(userId)
            val safeName = displayName.trim().ifBlank { "Usuario AMDV" }.take(100)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "themeMode" to themeMode.key,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateThemeMode(userId: String, themeMode: AppThemeMode): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUid(userId)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "themeMode" to themeMode.key,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val safeName = auth.currentUser?.displayName?.trim()?.ifBlank { "Usuario AMDV" }?.take(100)
                    ?: "Usuario AMDV"
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "themeMode" to themeMode.key,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertTrack(userId: String, track: TrackUiModel): Result<Unit> {
        val sanitizedId = track.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val docPath = "users/$userId/tracks/$sanitizedId"
        return try {
            val uid = requireUid(userId)
            require(sanitizedId.isNotBlank()) { "Track ID inválido" }
            val safeTitle = track.title.trim().ifBlank { "Sin título" }.take(120)
            val safeArtist = track.artist.trim().ifBlank { "Desconocido" }.take(100)
            val safeDuration = track.durationSec.coerceIn(0L, 86400L)
            val safeSource = when (track.sourceType) {
                "LOCAL" -> "LOCAL"
                "CLOUD" -> "CLOUD"
                else -> "BUNDLED"
            }

            val docRef = db.collection("users").document(uid)
                .collection("tracks").document(sanitizedId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "title" to safeTitle,
                        "artist" to safeArtist,
                        "durationSec" to safeDuration,
                        "favorite" to track.favorite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to safeTitle,
                        "artist" to safeArtist,
                        "durationSec" to safeDuration,
                        "favorite" to track.favorite,
                        "sourceType" to safeSource,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setTrackFavorite(userId: String, track: TrackUiModel, favorite: Boolean): Result<Unit> {
        return upsertTrack(userId, track.copy(favorite = favorite))
    }

    suspend fun deleteTrack(userId: String, trackId: String): Result<Unit> {
        val sanitizedId = trackId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val docPath = "users/$userId/tracks/$sanitizedId"
        return try {
            val uid = requireUid(userId)
            db.collection("users").document(uid)
                .collection("tracks").document(sanitizedId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserPlaylists(userId: String): Flow<List<UserPlaylist>> {
        val colRef = db.collection("users").document(userId).collection("playlists")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserPlaylist::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun createPlaylist(
        userId: String,
        name: String,
        description: String = "",
        initialTrackIds: List<String> = emptyList()
    ): Result<UserPlaylist> {
        val playlistId = "pl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUid(userId)
            val safeName = name.trim().take(80)
            require(safeName.isNotEmpty()) { "Ingresa un nombre para la playlist." }
            val safeDesc = description.trim().take(200)
            val safeTracks = initialTrackIds.distinct().take(500)

            val payload = mapOf(
                "userId" to uid,
                "name" to safeName,
                "description" to safeDesc,
                "trackIds" to safeTracks,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("playlists").document(playlistId)
                .set(payload)
                .await()
            Result.success(
                UserPlaylist(
                    id = playlistId,
                    userId = uid,
                    name = safeName,
                    description = safeDesc,
                    trackIds = safeTracks
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePlaylist(
        userId: String,
        playlistId: String,
        name: String? = null,
        description: String? = null,
        trackIds: List<String>? = null
    ): Result<Unit> {
        val sanitizedId = playlistId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val docPath = "users/$userId/playlists/$sanitizedId"
        return try {
            val uid = requireUid(userId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (name != null) {
                val safeName = name.trim().take(80)
                require(safeName.isNotEmpty()) { "El nombre de la playlist no puede estar vacío." }
                updates["name"] = safeName
            }
            if (description != null) {
                updates["description"] = description.trim().take(200)
            }
            if (trackIds != null) {
                updates["trackIds"] = trackIds.distinct().take(500)
            }
            db.collection("users").document(uid)
                .collection("playlists").document(sanitizedId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(userId: String, playlistId: String): Result<Unit> {
        val sanitizedId = playlistId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val docPath = "users/$userId/playlists/$sanitizedId"
        return try {
            val uid = requireUid(userId)
            db.collection("users").document(uid)
                .collection("playlists").document(sanitizedId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
