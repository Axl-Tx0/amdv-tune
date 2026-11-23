package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.LrcParser
import com.example.model.TrackUiModel
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private data class LyricCardThemeStyle(
    val id: String,
    val label: String,
    val startColor: Color,
    val endColor: Color,
    val textColor: Color,
    val subtitleColor: Color,
    val badgeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricCardStudioSheet(
    track: TrackUiModel,
    accentColor: Color,
    initialPositionMs: Long = 0L,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAmdvPalette.current
    val scope = rememberCoroutineScope()

    val allLines = remember(track.id, track.lyrics) {
        val synced = LrcParser.parseSyncedLyrics(track.lyrics)
        if (synced.isNotEmpty()) {
            synced.map { it.text }.filter { it.isNotBlank() }
        } else {
            LrcParser.stripTimestamps(track.lyrics)
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
    }

    val initialSelected = remember(track.id, allLines) {
        if (allLines.isEmpty()) {
            emptySet()
        } else {
            val synced = LrcParser.parseSyncedLyrics(track.lyrics)
            val activeIdx = if (synced.isNotEmpty()) {
                LrcParser.findActiveLineIndex(synced, initialPositionMs).coerceAtLeast(0)
            } else {
                0
            }
            val start = activeIdx.coerceIn(0, (allLines.size - 1).coerceAtLeast(0))
            val end = (start + 2).coerceAtMost(allLines.lastIndex)
            (start..end).toSet()
        }
    }

    var selectedIndices by remember(track.id) { mutableStateOf(initialSelected) }
    val cardThemes = remember(accentColor) {
        listOf(
            LyricCardThemeStyle(
                id = "dynamic",
                label = "Portada",
                startColor = accentColor,
                endColor = Color(0xFF0A0D16),
                textColor = Color.White,
                subtitleColor = Color.White.copy(alpha = 0.78f),
                badgeColor = Color.White
            ),
            LyricCardThemeStyle(
                id = "obsidian",
                label = "Neón",
                startColor = Color(0xFF0B1120),
                endColor = Color(0xFF1E1B4B),
                textColor = Color(0xFFF8FAFC),
                subtitleColor = Color(0xFF38BDF8),
                badgeColor = Color(0xFF22D3EE)
            ),
            LyricCardThemeStyle(
                id = "sunset",
                label = "Atardecer",
                startColor = Color(0xFF9A3412),
                endColor = Color(0xFF4C0519),
                textColor = Color(0xFFFFF7ED),
                subtitleColor = Color(0xFFFDBA74),
                badgeColor = Color(0xFFFDE68A)
            ),
            LyricCardThemeStyle(
                id = "emerald",
                label = "Esmeralda",
                startColor = Color(0xFF065F46),
                endColor = Color(0xFF022C22),
                textColor = Color(0xFFECFDF5),
                subtitleColor = Color(0xFF6EE7B7),
                badgeColor = Color(0xFFA7F3D0)
            ),
            LyricCardThemeStyle(
                id = "editorial",
                label = "Editorial",
                startColor = Color(0xFFF8FAFC),
                endColor = Color(0xFFE2E8F0),
                textColor = Color(0xFF0F172A),
                subtitleColor = Color(0xFF475569),
                badgeColor = Color(0xFF0F172A)
            )
        )
    }
    var selectedTheme by remember(cardThemes) { mutableStateOf(cardThemes.first()) }
    var isExporting by remember { mutableStateOf(false) }

    val selectedLines = remember(selectedIndices, allLines) {
        selectedIndices.sorted().mapNotNull { allLines.getOrNull(it) }
    }

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
                .testTag("lyric_card_studio_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TARJETA DE LETRA",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = palette.accent
                    )
                    Text(
                        text = "Elige de 1 a 4 versos para compartir en Stories o WhatsApp",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Card Preview
            Surface(
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, selectedTheme.badgeColor.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lyric_card_preview")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(selectedTheme.startColor, selectedTheme.endColor)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TrackCoverArt(
                                coverKey = track.coverKey,
                                title = track.title,
                                size = 46.dp,
                                cornerRadius = 10.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = selectedTheme.textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = selectedTheme.subtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selectedLines.isEmpty()) {
                                Text(
                                    text = "Toca las líneas de abajo para armar tu tarjeta.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = selectedTheme.subtitleColor
                                )
                            } else {
                                selectedLines.forEach { verse ->
                                    Text(
                                        text = verse,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 19.sp,
                                            lineHeight = 25.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = selectedTheme.textColor
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "AMDV-TUNE",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp,
                                color = selectedTheme.badgeColor
                            )
                            Text(
                                text = "${selectedLines.size}/4 versos",
                                style = MaterialTheme.typography.labelSmall,
                                color = selectedTheme.subtitleColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Theme Swatch Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cardThemes, key = { it.id }) { style ->
                    val chosen = style.id == selectedTheme.id
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (chosen) palette.accent.copy(alpha = 0.18f) else palette.surfaceElevated,
                        border = BorderStroke(1.dp, if (chosen) palette.accent else palette.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedTheme = style }
                            .testTag("lyric_card_theme_${style.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(style.startColor, style.endColor)))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium,
                                color = if (chosen) palette.accent else palette.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Line Picker (max 4 lines)
            if (allLines.isEmpty()) {
                Text(
                    text = "Esta canción aún no tiene letra cargada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 210.dp)
                        .testTag("lyric_card_lines_picker")
                ) {
                    itemsIndexed(allLines) { index, line ->
                        val isPicked = selectedIndices.contains(index)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPicked) palette.accent.copy(alpha = 0.16f) else palette.surfaceElevated.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isPicked) palette.accent else palette.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedIndices = if (isPicked) {
                                        selectedIndices - index
                                    } else if (selectedIndices.size < 4) {
                                        selectedIndices + index
                                    } else {
                                        Toast.makeText(context, "Máximo 4 versos por tarjeta", Toast.LENGTH_SHORT).show()
                                        selectedIndices
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPicked) palette.accent else palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isPicked) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Seleccionada",
                                        tint = palette.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                enabled = selectedLines.isNotEmpty() && !isExporting,
                onClick = {
                    isExporting = true
                    scope.launch {
                        val uri = withContext(Dispatchers.IO) {
                            renderAndSaveLyricCardPng(
                                context = context,
                                track = track,
                                lines = selectedLines,
                                theme = selectedTheme
                            )
                        }
                        isExporting = false
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "\"${selectedLines.joinToString(" / ")}\" — ${track.title} (${track.artist}) en AMDV-TUNE"
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir tarjeta de letra"))
                            }.onFailure {
                                Toast.makeText(context, "No se pudo abrir el menú para compartir", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No se pudo generar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("lyric_card_share_btn")
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        color = if (palette.isDark) Color.Black else Color.White,
                        strokeWidth = 2.4.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir imagen PNG", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun renderAndSaveLyricCardPng(
    context: Context,
    track: TrackUiModel,
    lines: List<String>,
    theme: LyricCardThemeStyle
): Uri? {
    return try {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                theme.startColor.toArgb(),
                theme.endColor.toArgb(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle inner frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = theme.badgeColor.copy(alpha = 0.22f).toArgb()
        }
        canvas.drawRoundRect(RectF(44f, 44f, width - 44f, height - 44f), 46f, 46f, framePaint)

        // Header cover art
        val coverBmp = decodeTrackCoverForCard(context, track.coverKey)
        val coverRect = RectF(96f, 110f, 256f, 270f)
        if (coverBmp != null) {
            val scaled = Bitmap.createScaledBitmap(coverBmp, 160, 160, true)
            val clipPath = android.graphics.Path().apply {
                addRoundRect(coverRect, 28f, 28f, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(scaled, null, coverRect, Paint(Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.badgeColor.copy(alpha = 0.22f).toArgb()
            }
            canvas.drawRoundRect(coverRect, 28f, 28f, fallbackPaint)
        }

        // Title & artist next to cover
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor.toArgb()
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.subtitleColor.toArgb()
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val maxHeaderWidth = width - 380
        val titleLayout = StaticLayout.Builder.obtain(
            track.title,
            0,
            track.title.length,
            titlePaint,
            maxHeaderWidth
        ).setMaxLines(2).build()
        canvas.save()
        canvas.translate(292f, 134f)
        titleLayout.draw(canvas)
        canvas.restore()

        val artistLayout = StaticLayout.Builder.obtain(
            track.artist,
            0,
            track.artist.length,
            artistPaint,
            maxHeaderWidth
        ).setMaxLines(1).build()
        canvas.save()
        canvas.translate(292f, 134f + titleLayout.height + 10f)
        artistLayout.draw(canvas)
        canvas.restore()

        // Lyrics body centered vertically
        val joinedLyrics = lines.joinToString("\n\n")
        val lyricSize = when {
            joinedLyrics.length <= 70 -> 68f
            joinedLyrics.length <= 130 -> 58f
            else -> 48f
        }
        val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor.toArgb()
            textSize = lyricSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val lyricLayout = StaticLayout.Builder.obtain(
            joinedLyrics,
            0,
            joinedLyrics.length,
            lyricPaint,
            width - 192
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(14f, 1.0f)
            .build()

        val bodyTop = (340f + ((height - 540f - lyricLayout.height) / 2f)).coerceAtLeast(330f)
        canvas.save()
        canvas.translate(96f, bodyTop)
        lyricLayout.draw(canvas)
        canvas.restore()

        // Footer watermark
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.badgeColor.toArgb()
            textSize = 34f
            letterSpacing = 0.16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("AMDV-TUNE", 96f, height - 108f, brandPaint)

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.subtitleColor.toArgb()
            textSize = 26f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Escúchala ahora", width - 96f, height - 108f, taglinePaint)

        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, "amdv_lyric_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }
}

private fun decodeTrackCoverForCard(context: Context, coverKey: String): Bitmap? {
    return try {
        when {
            coverKey == "COVER_CRIMSON" ->
                BitmapFactory.decodeResource(context.resources, R.drawable.img_cover_crimson_1794973232429)
            coverKey == "COVER_SUNSET" ->
                BitmapFactory.decodeResource(context.resources, R.drawable.img_cover_sunset_1794973233484)
            coverKey == "COVER_CYAN_DRIP" ->
                BitmapFactory.decodeResource(context.resources, R.drawable.img_cover_cyan_drip_1794973233181)
            coverKey == "COVER_EMBER" ->
                BitmapFactory.decodeResource(context.resources, R.drawable.img_cover_ember_1794973233687)
            coverKey.startsWith("/") -> {
                val f = File(coverKey)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            }
            coverKey.startsWith("content://") -> {
                context.contentResolver.openInputStream(Uri.parse(coverKey))?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
            coverKey.startsWith("http://") || coverKey.startsWith("https://") -> {
                val conn = (URL(coverKey).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    doInput = true
                }
                try {
                    if (conn.responseCode in 200..299) {
                        conn.inputStream.use { BitmapFactory.decodeStream(it) }
                    } else null
                } finally {
                    conn.disconnect()
                }
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
