package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.model.AppReleaseConfig
import com.example.model.AppThemeMode
import com.example.model.TrackUiModel
import com.example.ui.components.TrackCoverArt
import com.example.ui.theme.HeartPink
import com.example.ui.theme.LocalAmdvPalette
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileScreen(
    currentUser: FirebaseUser?,
    isAdmin: Boolean,
    isAuthLoading: Boolean,
    authErrorMessage: String?,
    selectedTheme: AppThemeMode,
    shuffleEnabled: Boolean,
    repeatOneEnabled: Boolean,
    autoContinueEnabled: Boolean,
    muteOnNotificationsEnabled: Boolean,
    isEqualizerEnabled: Boolean = false,
    crossfadeSeconds: Int = 0,
    hasActiveTrack: Boolean = false,
    activeSleepTimerLabel: String? = null,
    latestRelease: AppReleaseConfig?,
    favoriteCount: Int,
    offlineCount: Int,
    totalSongsCount: Int,
    topTracks: List<Pair<TrackUiModel, Int>> = emptyList(),
    topArtists: List<Pair<String, Int>> = emptyList(),
    totalPlaysCount: Int = 0,
    totalListeningMinutes: Int = 0,
    listeningStreakDays: Int = 0,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleAutoContinue: () -> Unit,
    onToggleMuteOnNotifications: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenDrivingMode: () -> Unit = {},
    onOpenReplay: () -> Unit = {},
    onTrackClick: (TrackUiModel) -> Unit = {},
    onOpenUpdateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val photoUrl = currentUser?.photoUrl
                if (photoUrl != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = stringResource(R.string.cd_app_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.amdv_tune_logo),
                        contentDescription = stringResource(R.string.cd_app_logo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(96.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentUser?.displayName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.brand_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentUser?.email?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.brand_subtitle_player),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.accent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.75f)),
                        modifier = Modifier.testTag("profile_admin_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = stringResource(R.string.role_admin_badge),
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.role_admin_badge),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(18.dp)
                ) {
                    if (currentUser == null) {
                        Text(
                            text = stringResource(R.string.profile_login_prompt),
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(palette.metallicStart, palette.metallicEnd)
                                    )
                                )
                                .clickable(enabled = !isAuthLoading, onClick = onSignInClick)
                                .testTag("google_signin_button")
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.profile_login_button),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.profile_login_caption),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAdmin) {
                                    stringResource(R.string.role_admin_status)
                                } else {
                                    stringResource(R.string.profile_synced_status)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onSignOutClick,
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_logout_button),
                                color = palette.textPrimary
                            )
                        }
                    }

                    if (!authErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = authErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryAccent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.profile_app_color),
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemeOptionTile(
                            mode = AppThemeMode.NEON,
                            selected = selectedTheme == AppThemeMode.NEON,
                            swatchBrush = Brush.linearGradient(
                                listOf(Color(0xFF00F0FF), Color(0xFF7C3AED))
                            ),
                            onClick = { onSelectTheme(AppThemeMode.NEON) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_card_neon")
                        )
                        ThemeOptionTile(
                            mode = AppThemeMode.NEGRO,
                            selected = selectedTheme == AppThemeMode.NEGRO,
                            swatchBrush = SolidColor(Color(0xFF050507)),
                            onClick = { onSelectTheme(AppThemeMode.NEGRO) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_card_negro")
                        )
                        ThemeOptionTile(
                            mode = AppThemeMode.BLANCO,
                            selected = selectedTheme == AppThemeMode.BLANCO,
                            swatchBrush = SolidColor(Color(0xFFFFFFFF)),
                            onClick = { onSelectTheme(AppThemeMode.BLANCO) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_card_blanco")
                        )
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = HeartPink,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    count = favoriteCount,
                    label = stringResource(R.string.stat_favorites),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.DownloadDone,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    count = offlineCount,
                    label = "Offline",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    count = totalSongsCount,
                    label = stringResource(R.string.stat_my_songs),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenReplay)
                    .testTag("profile_amdv_replay_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF7C3AED).copy(alpha = if (palette.isDark) 0.45f else 0.18f),
                                    palette.accent.copy(alpha = if (palette.isDark) 0.30f else 0.14f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.22f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AMDV REPLAY",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (totalPlaysCount > 0) {
                                    "🔥 Racha: $listeningStreakDays días • ${totalListeningMinutes.coerceAtLeast(totalPlaysCount * 3)} min escuchados"
                                } else {
                                    "Descubre tu resumen animado estilo Stories y compártelo"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.accent
                        ) {
                            Text(
                                text = "Ver",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (palette.isDark) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            PersonalTopCard(
                totalPlaysCount = totalPlaysCount,
                topTracks = topTracks,
                topArtists = topArtists,
                onTrackClick = onTrackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_top_personal_card")
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_playback_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PlaybackSettingRow(
                        icon = Icons.Filled.Shuffle,
                        title = stringResource(R.string.settings_shuffle_title),
                        subtitle = stringResource(R.string.settings_shuffle_subtitle),
                        checked = shuffleEnabled,
                        onCheckedChange = { onToggleShuffle() },
                        testTag = "setting_shuffle_switch"
                    )

                    HorizontalDivider(
                        color = palette.border.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    PlaybackSettingRow(
                        icon = Icons.Filled.RepeatOne,
                        title = stringResource(R.string.settings_repeat_title),
                        subtitle = stringResource(R.string.settings_repeat_subtitle),
                        checked = repeatOneEnabled,
                        onCheckedChange = { onToggleRepeat() },
                        testTag = "setting_repeat_switch"
                    )

                    HorizontalDivider(
                        color = palette.border.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    PlaybackSettingRow(
                        icon = Icons.Filled.SkipNext,
                        title = "Reproducción continua",
                        subtitle = "Al terminar una canción, continúa automáticamente con la siguiente",
                        checked = autoContinueEnabled,
                        onCheckedChange = { onToggleAutoContinue() },
                        testTag = "setting_auto_continue_switch"
                    )

                    HorizontalDivider(
                        color = palette.border.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    PlaybackSettingRow(
                        icon = Icons.Filled.NotificationsOff,
                        title = "Silenciar con notificaciones",
                        subtitle = "Opcional: desactivado por defecto para que la música no se corte con notificaciones ni al ir a Inicio",
                        checked = muteOnNotificationsEnabled,
                        onCheckedChange = { onToggleMuteOnNotifications() },
                        testTag = "setting_mute_notif_switch"
                    )

                    HorizontalDivider(
                        color = palette.border.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    val eqAndCrossfadeBadge = when {
                        isEqualizerEnabled && crossfadeSeconds > 0 -> "EQ • ${crossfadeSeconds}s"
                        crossfadeSeconds > 0 -> "Crossfade ${crossfadeSeconds}s"
                        isEqualizerEnabled -> "Activo"
                        else -> "Apagado"
                    }
                    PlaybackActionShortcutRow(
                        icon = Icons.Filled.Equalizer,
                        title = "Ecualizador, Bass Boost y Crossfade",
                        subtitle = "5 bandas, refuerzo de graves y transición suave (0s–12s)",
                        badgeText = eqAndCrossfadeBadge,
                        isActive = isEqualizerEnabled || crossfadeSeconds > 0,
                        onClick = onOpenEqualizer,
                        testTag = "profile_open_eq_btn"
                    )

                    HorizontalDivider(
                        color = palette.border.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    PlaybackActionShortcutRow(
                        icon = Icons.Filled.Timer,
                        title = "Temporizador de apagado",
                        subtitle = "Pausa automática al dormir o al terminar la pista",
                        badgeText = activeSleepTimerLabel ?: "Apagado",
                        isActive = activeSleepTimerLabel != null,
                        onClick = onOpenSleepTimer,
                        testTag = "profile_open_timer_btn"
                    )

                    HorizontalDivider(
                        color = palette.border.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    PlaybackActionShortcutRow(
                        icon = Icons.Filled.DirectionsCar,
                        title = "Modo Conducción",
                        subtitle = "Pantalla OLED de alto contraste con botones grandes para el auto",
                        badgeText = if (hasActiveTrack) "Abrir" else "Sin pista",
                        isActive = hasActiveTrack,
                        onClick = onOpenDrivingMode,
                        testTag = "profile_driving_mode_btn"
                    )
                }
            }
        }

        item {
            CreditsAndAboutCard(
                latestRelease = latestRelease,
                onOpenUpdateDialog = onOpenUpdateDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("credits_card")
            )
        }
    }
}

@Composable
private fun ThemeOptionTile(
    mode: AppThemeMode,
    selected: Boolean,
    swatchBrush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated.copy(alpha = 0.55f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) palette.accent else palette.border
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp)
        ) {
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0EA5E9))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Seleccionado",
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(swatchBrush)
                        .border(1.dp, palette.border, CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: @Composable () -> Unit,
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp)
        ) {
            icon()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun PlaybackSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalAmdvPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) palette.accent else palette.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (palette.isDark) Color.Black else Color.White,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.surfaceElevated,
                uncheckedBorderColor = palette.border
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PlaybackActionShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalAmdvPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) palette.accent else palette.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isActive) palette.accent.copy(alpha = 0.16f) else palette.surfaceElevated,
            border = BorderStroke(1.dp, if (isActive) palette.accent else palette.border)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) palette.accent else palette.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PersonalTopCard(
    totalPlaysCount: Int,
    topTracks: List<Pair<TrackUiModel, Int>>,
    topArtists: List<Pair<String, Int>>,
    onTrackClick: (TrackUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Leaderboard,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TU TOP PERSONAL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
                if (totalPlaysCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.accent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "$totalPlaysCount reproducciones",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topTracks.isEmpty()) {
                Text(
                    text = "Reproduce tus canciones favoritas y aquí aparecerán tus temas y artistas más escuchados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            } else {
                topTracks.take(5).forEachIndexed { idx, (track, count) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTrackClick(track) }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (idx == 0) palette.accent else palette.textSecondary,
                            modifier = Modifier.width(28.dp)
                        )
                        TrackCoverArt(
                            coverKey = track.coverKey,
                            title = track.title,
                            size = 42.dp,
                            cornerRadius = 9.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Text(
                                text = "$count×",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (topArtists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Artistas más escuchados",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(topArtists.take(6), key = { it.first }) { (artistName, plays) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Text(
                                    text = "$artistName • $plays",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditsAndAboutCard(
    latestRelease: AppReleaseConfig?,
    onOpenUpdateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.credits_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceElevated.copy(alpha = 0.55f))
                    .padding(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.amdv_tune_logo),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.brand_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${stringResource(R.string.credits_version_label)} ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accent
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (latestRelease != null && latestRelease.hasNewerVersion(BuildConfig.VERSION_CODE)) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenUpdateDialog,
                    border = BorderStroke(1.dp, palette.accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("profile_update_available_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nueva versión v${latestRelease.latestVersionName} disponible",
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CreditInfoRow(
                icon = Icons.Filled.Person,
                label = stringResource(R.string.credits_creator_label),
                value = stringResource(R.string.credits_creator_value)
            )
            CreditInfoRow(
                icon = Icons.Filled.Public,
                label = stringResource(R.string.credits_origin_label),
                value = stringResource(R.string.credits_origin_value)
            )
            CreditInfoRow(
                icon = Icons.Filled.Email,
                label = stringResource(R.string.credits_contact_label),
                value = stringResource(R.string.credits_contact_value)
            )
            CreditInfoRow(
                icon = Icons.Filled.GraphicEq,
                label = stringResource(R.string.credits_audio_label),
                value = stringResource(R.string.credits_audio_value)
            )
            CreditInfoRow(
                icon = Icons.Filled.CloudSync,
                label = stringResource(R.string.credits_cloud_label),
                value = stringResource(R.string.credits_cloud_value)
            )
            CreditInfoRow(
                icon = Icons.Filled.Palette,
                label = stringResource(R.string.credits_design_label),
                value = stringResource(R.string.credits_design_value)
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = palette.border.copy(alpha = 0.6f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.credits_footer),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CreditInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    val palette = LocalAmdvPalette.current

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary
            )
        }
    }
}
