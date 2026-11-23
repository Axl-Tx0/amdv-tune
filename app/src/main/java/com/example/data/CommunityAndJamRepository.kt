package com.example.data

import com.example.model.ArtistStats
import com.example.model.CommunityTrackStats
import com.example.model.JamSession
import com.example.model.SharedPlaylist
import com.example.model.TrackComment
import com.example.model.UserArtistFollow
import com.example.model.UserPlaylist
import com.example.model.UserTrackReaction
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
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
import java.security.SecureRandom

class CommunityAndJamRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val random = SecureRandom()
    private val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private fun generateSixCharCode(): String {
        val sb = StringBuilder(6)
        repeat(6) {
            sb.append(codeChars[random.nextInt(codeChars.length)])
        }
        return sb.toString()
    }

    private fun sanitizeTrackId(trackId: String): String {
        return trackId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
    }

    fun observeCommunityStats(): Flow<Map<String, CommunityTrackStats>> {
        val colRef = db.collection("community_stats")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CommunityTrackStats::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.trackId.ifBlank { it.id } }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    fun observeUserReactions(userId: String): Flow<Map<String, UserTrackReaction>> {
        val colRef = db.collection("users").document(userId).collection("reactions")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserTrackReaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.trackId.ifBlank { it.id } }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun recordCommunityPlay(trackId: String): Result<Unit> {
        val safeId = sanitizeTrackId(trackId)
        if (safeId.isBlank()) return Result.success(Unit)
        val docPath = "community_stats/$safeId"
        return try {
            if (auth.currentUser == null) return Result.success(Unit)
            val docRef = db.collection("community_stats").document(safeId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (snap.exists()) {
                    val current = (snap.getLong("playCount") ?: 0L).coerceIn(0L, 9_999_999L)
                    tx.update(
                        docRef,
                        mapOf(
                            "playCount" to (current + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        docRef,
                        mapOf(
                            "trackId" to safeId,
                            "playCount" to 1L,
                            "fireCount" to 0L,
                            "heartCount" to 0L,
                            "headphonesCount" to 0L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(
        userId: String,
        trackId: String,
        reactionType: String, // "fire", "heart", "headphones"
        currentReaction: UserTrackReaction?
    ): Result<Boolean> {
        val safeId = sanitizeTrackId(trackId)
        val userDocPath = "users/$userId/reactions/$safeId"
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Inicia sesión para reaccionar.")
            check(uid == userId) { "Sesión inválida." }
            require(safeId.isNotBlank()) { "Track inválido." }

            val prevFire = currentReaction?.fire == true
            val prevHeart = currentReaction?.heart == true
            val prevHeadphones = currentReaction?.headphones == true

            val nextFire = if (reactionType == "fire") !prevFire else prevFire
            val nextHeart = if (reactionType == "heart") !prevHeart else prevHeart
            val nextHeadphones = if (reactionType == "headphones") !prevHeadphones else prevHeadphones

            val nowActive = when (reactionType) {
                "fire" -> nextFire
                "heart" -> nextHeart
                "headphones" -> nextHeadphones
                else -> false
            }
            val delta = if (nowActive) 1L else -1L
            val countField = when (reactionType) {
                "fire" -> "fireCount"
                "heart" -> "heartCount"
                "headphones" -> "headphonesCount"
                else -> throw IllegalArgumentException("Reacción inválida.")
            }

            val userReactionRef = db.collection("users").document(uid)
                .collection("reactions").document(safeId)
            val statsRef = db.collection("community_stats").document(safeId)

            db.runTransaction { tx ->
                val userSnap = tx.get(userReactionRef)
                val statsSnap = tx.get(statsRef)

                if (userSnap.exists()) {
                    tx.update(
                        userReactionRef,
                        mapOf(
                            "fire" to nextFire,
                            "heart" to nextHeart,
                            "headphones" to nextHeadphones,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        userReactionRef,
                        mapOf(
                            "userId" to uid,
                            "trackId" to safeId,
                            "fire" to nextFire,
                            "heart" to nextHeart,
                            "headphones" to nextHeadphones,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (statsSnap.exists()) {
                    val prevCount = (statsSnap.getLong(countField) ?: 0L).coerceIn(0L, 9_999_999L)
                    val nextCount = (prevCount + delta).coerceAtLeast(0L)
                    tx.update(
                        statsRef,
                        mapOf(
                            countField to nextCount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        statsRef,
                        mapOf(
                            "trackId" to safeId,
                            "playCount" to 0L,
                            "fireCount" to if (reactionType == "fire" && nowActive) 1L else 0L,
                            "heartCount" to if (reactionType == "heart" && nowActive) 1L else 0L,
                            "headphonesCount" to if (reactionType == "headphones" && nowActive) 1L else 0L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                null
            }.await()

            Result.success(nowActive)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }

    suspend fun createJamSession(
        hostUserId: String,
        hostName: String,
        currentTrackId: String,
        playing: Boolean,
        positionMs: Long,
        queueTrackIds: List<String>
    ): Result<JamSession> {
        val code = generateSixCharCode()
        val docPath = "jam_sessions/$code"
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Inicia sesión con Google para crear una sala Jam.")
            check(uid == hostUserId) { "Sesión inválida." }
            val safeHostName = hostName.trim().ifBlank { "Anfitrión AMDV" }.take(100)
            val safeQueue = queueTrackIds.distinct().take(100)
            val safePos = positionMs.coerceIn(0L, 86_400_000L)

            val payload = mapOf(
                "roomCode" to code,
                "hostUserId" to uid,
                "hostName" to safeHostName,
                "currentTrackId" to currentTrackId.take(128),
                "playing" to playing,
                "positionMs" to safePos,
                "queueTrackIds" to safeQueue,
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("jam_sessions").document(code).set(payload).await()
            Result.success(
                JamSession(
                    id = code,
                    roomCode = code,
                    hostUserId = uid,
                    hostName = safeHostName,
                    currentTrackId = currentTrackId.take(128),
                    playing = playing,
                    positionMs = safePos,
                    queueTrackIds = safeQueue,
                    active = true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun fetchJamSession(roomCode: String): Result<JamSession?> {
        val normalized = roomCode.trim().uppercase()
        val docPath = "jam_sessions/$normalized"
        return try {
            require(normalized.matches(Regex("^[A-Z0-9]{6}$"))) {
                "El código de sala debe tener 6 letras o números."
            }
            val snap = db.collection("jam_sessions").document(normalized).get().await()
            if (!snap.exists()) return Result.success(null)
            val session = snap.toObject(
                JamSession::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    fun observeJamSession(roomCode: String): Flow<JamSession?> {
        val normalized = roomCode.trim().uppercase()
        val docRef = db.collection("jam_sessions").document(normalized)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        JamSession::class.java,
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
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun updateJamPlayback(
        roomCode: String,
        currentTrackId: String,
        playing: Boolean,
        positionMs: Long,
        queueTrackIds: List<String>? = null
    ): Result<Unit> {
        val normalized = roomCode.trim().uppercase()
        val docPath = "jam_sessions/$normalized"
        return try {
            val updates = mutableMapOf<String, Any>(
                "currentTrackId" to currentTrackId.take(128),
                "playing" to playing,
                "positionMs" to positionMs.coerceIn(0L, 86_400_000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (queueTrackIds != null) {
                updates["queueTrackIds"] = queueTrackIds.distinct().take(100)
            }
            db.collection("jam_sessions").document(normalized).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addTrackToJamQueue(roomCode: String, trackId: String): Result<Unit> {
        val normalized = roomCode.trim().uppercase()
        val docPath = "jam_sessions/$normalized"
        return try {
            val docRef = db.collection("jam_sessions").document(normalized)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("La sala Jam ya no está activa.")
                val existing = (snap.get("queueTrackIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                val updated = (existing + trackId).distinct().take(100)
                tx.update(
                    docRef,
                    mapOf(
                        "queueTrackIds" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun closeJamSession(roomCode: String): Result<Unit> {
        val normalized = roomCode.trim().uppercase()
        val docPath = "jam_sessions/$normalized"
        return try {
            db.collection("jam_sessions").document(normalized)
                .update(
                    mapOf(
                        "active" to false,
                        "playing" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishSharedPlaylist(
        ownerUserId: String,
        ownerName: String,
        playlist: UserPlaylist,
        collaborative: Boolean
    ): Result<SharedPlaylist> {
        val code = generateSixCharCode()
        val docPath = "shared_playlists/$code"
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Inicia sesión con Google para compartir tu playlist.")
            check(uid == ownerUserId) { "Sesión inválida." }
            val safeOwnerName = ownerName.trim().ifBlank { "Usuario AMDV" }.take(100)
            val safeName = playlist.name.trim().ifBlank { "Playlist AMDV" }.take(80)
            val safeDesc = playlist.description.trim().take(200)
            val safeTracks = playlist.trackIds.distinct().take(500)

            val payload = mapOf(
                "shareCode" to code,
                "ownerUserId" to uid,
                "ownerName" to safeOwnerName,
                "name" to safeName,
                "description" to safeDesc,
                "trackIds" to safeTracks,
                "collaborative" to collaborative,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("shared_playlists").document(code).set(payload).await()
            Result.success(
                SharedPlaylist(
                    id = code,
                    shareCode = code,
                    ownerUserId = uid,
                    ownerName = safeOwnerName,
                    name = safeName,
                    description = safeDesc,
                    trackIds = safeTracks,
                    collaborative = collaborative
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun fetchSharedPlaylist(shareCode: String): Result<SharedPlaylist?> {
        val normalized = shareCode.trim().uppercase()
        val docPath = "shared_playlists/$normalized"
        return try {
            require(normalized.matches(Regex("^[A-Z0-9]{6}$"))) {
                "El código de playlist debe tener 6 caracteres."
            }
            val snap = db.collection("shared_playlists").document(normalized).get().await()
            if (!snap.exists()) return Result.success(null)
            val shared = snap.toObject(
                SharedPlaylist::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(shared)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun appendTrackToCollaborativePlaylist(
        shareCode: String,
        trackId: String
    ): Result<Unit> {
        val normalized = shareCode.trim().uppercase()
        val docPath = "shared_playlists/$normalized"
        return try {
            val docRef = db.collection("shared_playlists").document(normalized)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("Playlist compartida no encontrada.")
                val isCollab = snap.getBoolean("collaborative") == true
                if (!isCollab) throw IllegalStateException("Esta playlist es solo de lectura.")
                val existing = (snap.get("trackIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                val updated = (existing + trackId).distinct().take(500)
                tx.update(
                    docRef,
                    mapOf(
                        "trackIds" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Artist Follows & Live Follower Counts ---
    fun observeArtistStats(): Flow<Map<String, ArtistStats>> {
        val colRef = db.collection("artist_stats")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ArtistStats::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.artistId.ifBlank { it.id } }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    fun observeFollowedArtistIds(userId: String): Flow<Set<String>> {
        val colRef = db.collection("users").document(userId).collection("followed_artists")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserArtistFollow::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.artistId.ifBlank { it.id } }.toSet()
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun toggleFollowArtist(
        userId: String,
        artistId: String,
        artistName: String,
        currentlyFollowing: Boolean
    ): Result<Boolean> {
        val safeArtistId = sanitizeTrackId(artistId)
        val followPath = "users/$userId/followed_artists/$safeArtistId"
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Inicia sesión con Google para seguir artistas.")
            check(uid == userId) { "Sesión inválida." }
            require(safeArtistId.isNotBlank()) { "Artista inválido." }
            val safeName = artistName.trim().ifBlank { "Artista" }.take(100)
            val nowFollowing = !currentlyFollowing
            val delta = if (nowFollowing) 1L else -1L

            val followRef = db.collection("users").document(uid)
                .collection("followed_artists").document(safeArtistId)
            val statsRef = db.collection("artist_stats").document(safeArtistId)

            db.runTransaction { tx ->
                val statsSnap = tx.get(statsRef)
                if (nowFollowing) {
                    tx.set(
                        followRef,
                        mapOf(
                            "userId" to uid,
                            "artistId" to safeArtistId,
                            "artistName" to safeName,
                            "followedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.delete(followRef)
                }

                if (statsSnap.exists()) {
                    val prev = (statsSnap.getLong("followerCount") ?: 0L).coerceIn(0L, 9_999_999L)
                    val next = (prev + delta).coerceAtLeast(0L)
                    tx.update(
                        statsRef,
                        mapOf(
                            "followerCount" to next,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (nowFollowing) {
                    tx.set(
                        statsRef,
                        mapOf(
                            "artistId" to safeArtistId,
                            "followerCount" to 1L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                null
            }.await()
            Result.success(nowFollowing)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followPath)
            Result.failure(e)
        }
    }

    // --- Timestamped Track Comments & Admin Moderation ---
    fun observeCommentsForTrack(trackId: String): Flow<List<TrackComment>> {
        val safeTrackId = sanitizeTrackId(trackId)
        val colRef = db.collection("track_comments")
        return colRef.whereEqualTo("trackId", safeTrackId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TrackComment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<TrackComment> { it.timestampMs }.thenBy { it.createdAt?.toDate()?.time ?: 0L })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    fun observeAllCommentsForModeration(): Flow<List<TrackComment>> {
        val colRef = db.collection("track_comments")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TrackComment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .take(150)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun addTrackComment(
        userId: String,
        authorName: String,
        trackId: String,
        trackTitle: String,
        timestampMs: Long,
        text: String
    ): Result<Unit> {
        val safeTrackId = sanitizeTrackId(trackId)
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docPath = "track_comments/$commentId"
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Inicia sesión con Google para comentar.")
            check(uid == userId) { "Sesión inválida." }
            require(safeTrackId.isNotBlank()) { "Canción inválida." }
            val cleanedText = text.trim().take(220)
            require(cleanedText.isNotBlank()) { "Escribe un comentario antes de enviar." }

            val payload = mapOf(
                "trackId" to safeTrackId,
                "trackTitle" to trackTitle.trim().ifBlank { "Canción" }.take(120),
                "userId" to uid,
                "authorName" to authorName.trim().ifBlank { "Usuario AMDV" }.take(100),
                "timestampMs" to timestampMs.coerceIn(0L, 86_400_000L),
                "text" to cleanedText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("track_comments").document(commentId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTrackComment(commentId: String): Result<Unit> {
        val safeId = sanitizeTrackId(commentId)
        val docPath = "track_comments/$safeId"
        return try {
            require(auth.currentUser != null) { "Inicia sesión primero." }
            require(safeId.isNotBlank()) { "Comentario inválido." }
            db.collection("track_comments").document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
