package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrackUiModel
import com.example.ui.theme.OrbitronFontFamily

@Composable
fun AmdvReplayOverlay(
    visible: Boolean,
    userName: String,
    totalPlaysCount: Int,
    totalListeningMinutes: Int,
    listeningStreakDays: Int,
    favoriteCount: Int,
    offlineCount: Int,
    playlistCount: Int,
    topTracks: List<Pair<TrackUiModel, Int>>,
    topArtists: List<Pair<String, Int>>,
    onPlayTrack: (TrackUiModel) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(200))
    ) {
        val context = LocalContext.current
        var slideIndex by remember { mutableIntStateOf(0) }
        val totalSlides = 4

        val gradients = remember {
            listOf(
                listOf(Color(0xFF0A192F), Color(0xFF1D4ED8), Color(0xFF06B6D4)),
                listOf(Color(0xFF1E1038), Color(0xFF7C3AED), Color(0xFFEC4899)),
                listOf(Color(0xFF09201E), Color(0xFF059669), Color(0xFF10B981)),
                listOf(Color(0xFF181124), Color(0xFFDB2777), Color(0xFFF59E0B))
            )
        }

        val currentGradient = gradients[slideIndex.coerceIn(0, gradients.lastIndex)]
        val topTrack = topTracks.firstOrNull()
        val topArtist = topArtists.firstOrNull()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(currentGradient))
                .pointerInput(slideIndex) {
                    detectTapGestures { offset ->
                        if (offset.x < size.width * 0.35f) {
                            slideIndex = (slideIndex - 1).coerceAtLeast(0)
                        } else {
                            if (slideIndex < totalSlides - 1) {
                                slideIndex++
                            } else {
                                onDismiss()
                            }
                        }
                    }
                }
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .testTag("amdv_replay_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Top Stories Progress Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(totalSlides) { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (idx <= slideIndex) Color.White else Color.White.copy(alpha = 0.28f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Top Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "AMDV REPLAY",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = userName.ifBlank { "Tu resumen musical" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("replay_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar AMDV Replay",
                            tint = Color.White
                        )
                    }
                }

                // Slide Content
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = slideIndex,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                        label = "replay_slide_anim"
                    ) { page ->
                        when (page) {
                            0 -> {
                                // Slide 1: Plays, Minutes & Streak
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "TU RITMO EN AMDV-TUNE",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.8.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = "$totalPlaysCount",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "reproducciones totales",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ReplayStatPill(
                                            value = "${totalListeningMinutes.coerceAtLeast(if (totalPlaysCount > 0) totalPlaysCount * 3 else 0)} min",
                                            label = "Escuchados",
                                            modifier = Modifier.weight(1f)
                                        )
                                        ReplayStatPill(
                                            value = "$listeningStreakDays días",
                                            label = "Racha activa",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // Slide 2: #1 Track
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "TU CANCIÓN #1",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 14.sp,
                                        letterSpacing = 2.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    if (topTrack != null) {
                                        val (track, plays) = topTrack
                                        Surface(
                                            shape = RoundedCornerShape(26.dp),
                                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.65f)),
                                            shadowElevation = 18.dp
                                        ) {
                                            TrackCoverArt(
                                                coverKey = track.coverKey,
                                                title = track.title,
                                                size = 210.dp,
                                                cornerRadius = 26.dp
                                            )
                                        }
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artist} • $plays reproducciones",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White.copy(alpha = 0.85f),
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onPlayTrack(track)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Escuchar ahora", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                            text = "Reproduce tus primeros temas para desbloquear tu canción #1.",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            2 -> {
                                // Slide 3: #1 Artist & Top Artists
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "TU ARTISTA TOP",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 14.sp,
                                        letterSpacing = 2.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    if (topArtist != null) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(92.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                        Text(
                                            text = topArtist.first,
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${topArtist.second} reproducciones en tu historial",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        if (topArtists.size > 1) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color.Black.copy(alpha = 0.28f),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(16.dp)
                                                ) {
                                                    topArtists.take(4).forEachIndexed { idx, (name, plays) ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = "#${idx + 1}",
                                                                fontFamily = OrbitronFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                modifier = Modifier.width(34.dp)
                                                            )
                                                            Text(
                                                                text = name,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                color = Color.White,
                                                                modifier = Modifier.weight(1f),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "$plays×",
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = Color.White.copy(alpha = 0.85f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Aún estamos descubriendo tu artista favorito.",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            else -> {
                                // Slide 4: Summary Card + Share Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(26.dp),
                                        color = Color.Black.copy(alpha = 0.34f),
                                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.padding(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = Color(0xFFFDE047),
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Text(
                                                text = "MI AMDV REPLAY",
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 18.sp,
                                                letterSpacing = 2.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = buildString {
                                                    append("🎵 $totalPlaysCount reproducciones")
                                                    if (topTrack != null) append("\n🏆 Tema #1: ${topTrack.first.title}")
                                                    if (topArtist != null) append("\n⭐ Artista #1: ${topArtist.first}")
                                                    append("\n❤️ $favoriteCount favoritos • 📥 $offlineCount offline • 📀 $playlistCount playlists")
                                                    append("\n🔥 Racha actual: $listeningStreakDays días")
                                                },
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color.White.copy(alpha = 0.94f),
                                                textAlign = TextAlign.Center,
                                                lineHeight = 26.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val shareSummary = buildString {
                                                append("🎧 Mi AMDV Replay en AMDV-TUNE 🔥\n")
                                                append("• Reproducciones: $totalPlaysCount\n")
                                                if (topTrack != null) {
                                                    append("• Canción #1: ${topTrack.first.title} - ${topTrack.first.artist}\n")
                                                }
                                                if (topArtist != null) {
                                                    append("• Artista #1: ${topArtist.first}\n")
                                                }
                                                append("• Racha activa: $listeningStreakDays días\n")
                                                append("• Favoritos: $favoriteCount | Offline: $offlineCount")
                                            }
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareSummary)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Compartir mi AMDV Replay"))
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("replay_share_btn")
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Compartir mi Replay", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom hint
                Text(
                    text = if (slideIndex < totalSlides - 1) "Toca a la derecha para continuar →" else "Toca para cerrar",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ReplayStatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp)
        ) {
            Text(
                text = value,
                fontFamily = OrbitronFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
