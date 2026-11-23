package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaybackState
import com.example.model.EqualizerPreset
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.TrashRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    val palette = LocalAmdvPalette.current
    val queue = playbackState.queue
    val currentIndex = playbackState.currentQueueIndex

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
                .testTag("queue_bottom_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COLA DE REPRODUCCIÓN",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = palette.accent
                    )
                    Text(
                        text = "${queue.size} canciones en cola",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (queue.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp)
                ) {
                    Text(
                        text = "No hay canciones en la cola.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(queue, key = { idx, tr -> "${tr.id}_$idx" }) { index, track ->
                        val isCurrent = index == currentIndex || playbackState.currentTrack?.id == track.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) palette.accent.copy(alpha = 0.14f) else palette.surfaceElevated.copy(alpha = 0.55f),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) palette.accent.copy(alpha = 0.65f) else palette.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onPlayIndex(index) }
                                .testTag("queue_item_$index")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                TrackCoverArt(
                                    coverKey = track.coverKey,
                                    title = track.title,
                                    size = 44.dp,
                                    cornerRadius = 10.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) palette.accent else palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isCurrent) "Sonando ahora • ${track.artist}" else track.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveItem(index, index - 1) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowUp,
                                            contentDescription = "Subir en la cola",
                                            tint = palette.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (index < queue.lastIndex) {
                                    IconButton(
                                        onClick = { onMoveItem(index, index + 1) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Bajar en la cola",
                                            tint = palette.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (!isCurrent) {
                                    IconButton(
                                        onClick = { onRemoveItem(index) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Quitar de la cola",
                                            tint = TrashRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsAndTimerSheet(
    playbackState: PlaybackState,
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onToggleEqualizer: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onChangeBandLevel: (Int, Int) -> Unit,
    onChangeBassBoost: (Int) -> Unit,
    onChangeCrossfadeSeconds: (Int) -> Unit = {},
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetSleepAfterTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit
) {
    val palette = LocalAmdvPalette.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val fx = playbackState.audioEffects

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .verticalScroll(rememberScrollState())
                .testTag("audio_fx_timer_sheet")
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = palette.surface,
                contentColor = palette.accent,
                divider = { HorizontalDivider(color = palette.border) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ecualizador") },
                    icon = { Icon(Icons.Filled.Equalizer, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Temporizador") },
                    icon = { Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ecualizador y Graves",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (fx.enabled) {
                                "Perfil activo: ${fx.preset.label}"
                            } else {
                                "Desactivado • Sonido original"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Switch(
                        checked = fx.enabled,
                        onCheckedChange = onToggleEqualizer,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (palette.isDark) Color.Black else Color.White,
                            checkedTrackColor = palette.accent
                        ),
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val presets = remember {
                    listOf(
                        EqualizerPreset.NORMAL,
                        EqualizerPreset.BASS_BOOST,
                        EqualizerPreset.ELECTRONICA,
                        EqualizerPreset.ROCK,
                        EqualizerPreset.POP,
                        EqualizerPreset.VOCAL
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets, key = { it.key }) { preset ->
                        val isSelected = fx.enabled && fx.preset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accent.copy(alpha = 0.22f),
                                selectedLabelColor = palette.accent
                            ),
                            modifier = Modifier.testTag("eq_preset_${preset.key.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceElevated.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        fx.bands.forEach { band ->
                            val dbValue = band.levelMb / 100f
                            val dbText = if (dbValue >= 0f) "+${String.format("%.1f", dbValue)} dB" else "${String.format("%.1f", dbValue)} dB"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = band.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary,
                                    modifier = Modifier.width(56.dp)
                                )
                                Slider(
                                    value = band.levelMb.toFloat(),
                                    onValueChange = { onChangeBandLevel(band.index, it.toInt()) },
                                    valueRange = fx.minLevelMb.toFloat()..fx.maxLevelMb.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (fx.enabled) palette.accent else palette.textSecondary,
                                        activeTrackColor = if (fx.enabled) palette.accent else palette.border,
                                        inactiveTrackColor = palette.border
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = dbText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (fx.enabled) palette.accent else palette.textSecondary,
                                    modifier = Modifier.width(54.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceElevated.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Refuerzo de graves (Bass Boost)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary
                                )
                            }
                            Text(
                                text = "${(fx.bassBoostStrength / 10)}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                        }
                        Slider(
                            value = fx.bassBoostStrength.toFloat(),
                            onValueChange = { onChangeBassBoost(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accent,
                                activeTrackColor = palette.accent,
                                inactiveTrackColor = palette.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bass_boost_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceElevated.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (playbackState.crossfadeSeconds > 0) palette.accent.copy(alpha = 0.6f) else palette.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Crossfade suave entre canciones",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary
                                )
                            }
                            Text(
                                text = if (playbackState.crossfadeSeconds == 0) {
                                    "Desactivado"
                                } else {
                                    "${playbackState.crossfadeSeconds}s"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (playbackState.crossfadeSeconds > 0) palette.accent else palette.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mezcla gradualmente el final de una canción con el inicio de la siguiente (0s a 12s).",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Slider(
                            value = playbackState.crossfadeSeconds.toFloat(),
                            onValueChange = { onChangeCrossfadeSeconds(it.toInt().coerceIn(0, 12)) },
                            valueRange = 0f..12f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accent,
                                activeTrackColor = palette.accent,
                                inactiveTrackColor = palette.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crossfade_slider")
                        )
                    }
                }
            } else {
                // Sleep Timer tab
                val remainingMs = playbackState.sleepTimerRemainingMs
                val afterTrack = playbackState.sleepAfterCurrentTrack
                val hasActiveTimer = remainingMs != null || afterTrack

                Text(
                    text = "Temporizador de apagado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ideal para dormir escuchando música: pausaremos la reproducción automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (hasActiveTimer) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.accent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(palette.accent.copy(alpha = 0.22f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = palette.accent
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (afterTrack) "Al terminar esta canción" else formatSleepCountdown(remainingMs ?: 0L),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent
                                )
                                Text(
                                    text = "Temporizador activo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = onCancelSleepTimer,
                                border = BorderStroke(1.dp, TrashRose),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("sleep_timer_cancel_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TimerOff,
                                    contentDescription = null,
                                    tint = TrashRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apagar", color = TrashRose)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                val durations = listOf(15, 30, 45, 60)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        durations.take(2).forEach { mins ->
                            SleepTimerOptionButton(
                                label = "$mins minutos",
                                onClick = { onSetSleepTimerMinutes(mins) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sleep_timer_${mins}m")
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        durations.drop(2).forEach { mins ->
                            SleepTimerOptionButton(
                                label = "$mins minutos",
                                onClick = { onSetSleepTimerMinutes(mins) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sleep_timer_${mins}m")
                            )
                        }
                    }
                    Button(
                        onClick = onSetSleepAfterTrack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = if (palette.isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sleep_timer_end_of_track")
                    ) {
                        Text("Pausar al terminar esta canción", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerOptionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = palette.textPrimary)
    }
}

fun formatSleepCountdown(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}
