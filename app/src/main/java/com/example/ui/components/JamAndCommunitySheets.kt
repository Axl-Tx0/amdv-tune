package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JamSession
import com.example.model.TrackUiModel
import com.example.model.UserPlaylist
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.TrashRose

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareTextIntent(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir con amigos"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamSessionSheet(
    activeJam: JamSession?,
    isHost: Boolean,
    isSignedIn: Boolean,
    allTracks: List<TrackUiModel>,
    currentTrack: TrackUiModel?,
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onLeaveOrCloseRoom: () -> Unit,
    onAddTrackToJam: (TrackUiModel) -> Unit,
    onPlayJamTrack: (TrackUiModel) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAmdvPalette.current
    var codeInput by remember { mutableStateOf("") }
    var showAddTrackPicker by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }
    val tracksById = remember(allTracks) { allTracks.associateBy { it.id } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("jam_session_sheet")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Podcasts,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "JAM EN VIVO",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                    Text(
                        text = if (activeJam != null) {
                            if (isHost) "Eres el anfitrión de la sala" else "Conectado con ${activeJam.hostName}"
                        } else {
                            "Escucha música sincronizada con amigos"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isSignedIn) {
                Text(
                    text = "Inicia sesión con Google en la pestaña Perfil para crear una sala Jam o unirte con un código.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            } else if (activeJam == null) {
                Button(
                    onClick = onCreateRoom,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = if (palette.isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("jam_create_room_btn")
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear sala Jam ahora", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "O únete con el código de un amigo",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { raw ->
                            codeInput = raw.uppercase().filter { it.isLetterOrDigit() }.take(6)
                        },
                        label = { Text("Código (6 dígitos)") },
                        placeholder = { Text("Ej: AMDV99") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("jam_join_code_input")
                    )
                    Button(
                        enabled = codeInput.length == 6,
                        onClick = { onJoinRoom(codeInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.secondaryAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("jam_join_room_btn")
                    ) {
                        Text("Unirse", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Active Jam Room Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceElevated.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CÓDIGO DE SALA",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Text(
                                text = activeJam.roomCode,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 24.sp,
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.accent
                            )
                        }
                        IconButton(
                            onClick = {
                                copyToClipboard(context, "Código Jam AMDV-TUNE", activeJam.roomCode)
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copiar código",
                                tint = palette.textPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                shareTextIntent(
                                    context,
                                    "¡Únete a mi sala Jam en vivo en AMDV-TUNE! 🎧🔥\nCódigo de sala: ${activeJam.roomCode}"
                                )
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Compartir código Jam",
                                tint = palette.accent
                            )
                        }
                    }
                }

                val playingTrack = tracksById[activeJam.currentTrackId] ?: currentTrack
                if (playingTrack != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (activeJam.playing) "SONANDO EN SINCRONÍA" else "EN PAUSA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceElevated.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            TrackCoverArt(
                                coverKey = playingTrack.coverKey,
                                title = playingTrack.title,
                                size = 44.dp,
                                cornerRadius = 10.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playingTrack.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = playingTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cola compartida (${activeJam.queueTrackIds.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    TextButton(onClick = { showAddTrackPicker = !showAddTrackPicker }) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showAddTrackPicker) "Ver cola" else "Sumar canción",
                            color = palette.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showAddTrackPicker) {
                    OutlinedTextField(
                        value = pickerQuery,
                        onValueChange = { pickerQuery = it },
                        label = { Text("Buscar canción del catálogo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val catalogCloud = remember(allTracks, pickerQuery) {
                        val q = pickerQuery.trim()
                        allTracks.filter { it.isCloudCatalog }.filter {
                            q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
                        }.take(25)
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(catalogCloud, key = { "pick_${it.id}" }) { tr ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onAddTrackToJam(tr)
                                        showAddTrackPicker = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                TrackCoverArt(coverKey = tr.coverKey, title = tr.title, size = 36.dp, cornerRadius = 8.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tr.title, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(tr.artist, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = palette.accent)
                            }
                        }
                    }
                } else {
                    val jamQueueTracks = remember(activeJam.queueTrackIds, tracksById) {
                        activeJam.queueTrackIds.mapNotNull { tracksById[it] }
                    }
                    if (jamQueueTracks.isEmpty()) {
                        Text(
                            text = "Cualquier participante puede sumar canciones a la cola compartida.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                        ) {
                            items(jamQueueTracks, key = { "jamq_${it.id}" }) { tr ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = isHost) { onPlayJamTrack(tr) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    TrackCoverArt(coverKey = tr.coverKey, title = tr.title, size = 36.dp, cornerRadius = 8.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tr.title, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(tr.artist, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (isHost) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir", tint = palette.accent)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onLeaveOrCloseRoom,
                    border = BorderStroke(1.dp, TrashRose),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("jam_leave_btn")
                ) {
                    Text(
                        text = if (isHost) "Finalizar sala Jam" else "Salir de la sala",
                        color = TrashRose,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SharePlaylistCodeDialog(
    playlist: UserPlaylist,
    generatedCode: String?,
    isPublishing: Boolean,
    onPublish: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAmdvPalette.current
    var collaborative by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = palette.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir playlist")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Comparte \"${playlist.name}\" (${playlist.trackIds.size} canciones) mediante un código de 6 caracteres.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )

                if (generatedCode == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo colaborativo",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Permite que amigos con el código agreguen canciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = collaborative,
                            onCheckedChange = { collaborative = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = palette.accent)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "CÓDIGO LISTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = generatedCode,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 26.sp,
                                letterSpacing = 4.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.accent
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        copyToClipboard(context, "Código Playlist AMDV-TUNE", generatedCode)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copiar")
                                }
                                Button(
                                    onClick = {
                                        shareTextIntent(
                                            context,
                                            "¡Escucha mi playlist \"${playlist.name}\" en AMDV-TUNE! 🎵\nUsa el código: $generatedCode en Biblioteca → Playlists → Unirse por código."
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accent,
                                        contentColor = if (palette.isDark) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enviar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (generatedCode == null) {
                Button(
                    enabled = !isPublishing,
                    onClick = { onPublish(collaborative) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = if (palette.isDark) Color.Black else Color.White
                    )
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Generar código", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Listo", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (generatedCode == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        }
    )
}

@Composable
fun JoinPlaylistByCodeDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    val palette = LocalAmdvPalette.current
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = palette.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unirse a playlist por código")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ingresa el código de 6 caracteres que compartió un amigo para importar su playlist a tu biblioteca.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { raw ->
                        code = raw.uppercase().filter { it.isLetterOrDigit() }.take(6)
                    },
                    label = { Text("Código de playlist") },
                    placeholder = { Text("Ej: MIX777") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_playlist_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                enabled = code.length == 6,
                onClick = { onJoin(code) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                ),
                modifier = Modifier.testTag("join_playlist_confirm_btn")
            ) {
                Text("Importar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = palette.textSecondary)
            }
        }
    )
}
