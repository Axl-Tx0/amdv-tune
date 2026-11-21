package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.model.AppReleaseConfig
import com.example.ui.theme.LocalAmdvPalette

fun openExternalUpdateUrl(context: Context, url: String) {
    val cleanUrl = url.trim()
    if (!cleanUrl.startsWith("https://", ignoreCase = true)) {
        Toast.makeText(context, "Enlace de descarga inválido.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No se encontró un navegador para descargar la actualización.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun AppUpdateAvailableDialog(
    release: AppReleaseConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAmdvPalette.current

    AlertDialog(
        onDismissRequest = {
            if (!release.mandatory) onDismiss()
        },
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
        icon = {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = "Nueva actualización disponible",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}  →  v${release.latestVersionName}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = palette.accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (release.releaseNotes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Novedades",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.accent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = release.releaseNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "Se abrirá la descarga del APK oficial desde GitHub. Al terminar, toca el archivo descargado para actualizar sin perder tus datos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openExternalUpdateUrl(context, release.downloadUrl)
                    if (!release.mandatory) onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                ),
                modifier = Modifier.testTag("update_download_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Descargar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!release.mandatory) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("update_later_btn")
                ) {
                    Text("Más tarde", color = palette.textSecondary)
                }
            }
        }
    )
}

@Composable
fun AdminAppUpdateTabContent(
    currentCloudRelease: AppReleaseConfig?,
    isUploading: Boolean,
    onPublishAppUpdate: (
        versionCode: Long,
        versionName: String,
        downloadUrl: String,
        releaseNotes: String,
        mandatory: Boolean,
        onDone: (Boolean) -> Unit
    ) -> Unit
) {
    val palette = LocalAmdvPalette.current
    val suggestedCode = remember(currentCloudRelease) {
        val base = maxOf(BuildConfig.VERSION_CODE.toLong(), currentCloudRelease?.latestVersionCode ?: 1L)
        (base + 1L).toString()
    }
    var versionCodeText by remember(currentCloudRelease) {
        mutableStateOf(
            currentCloudRelease?.latestVersionCode?.takeIf { it > 0 }?.toString() ?: suggestedCode
        )
    }
    var versionNameText by remember(currentCloudRelease) {
        mutableStateOf(
            currentCloudRelease?.latestVersionName?.ifBlank { null } ?: "1.1"
        )
    }
    var downloadUrlText by remember(currentCloudRelease) {
        mutableStateOf(currentCloudRelease?.downloadUrl.orEmpty())
    }
    var releaseNotesText by remember(currentCloudRelease) {
        mutableStateOf(currentCloudRelease?.releaseNotes.orEmpty())
    }
    var mandatory by remember(currentCloudRelease) {
        mutableStateOf(currentCloudRelease?.mandatory ?: false)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceElevated.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Versión instalada en este celular: v${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (currentCloudRelease != null && currentCloudRelease.latestVersionCode > 0) {
                            "Publicada en la nube: v${currentCloudRelease.latestVersionName} (código ${currentCloudRelease.latestVersionCode})"
                        } else {
                            "Aún no has publicado ninguna actualización en la nube."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "Sube tu nuevo APK a GitHub Releases (repo público), copia el enlace directo del .apk y publícalo aquí.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = versionCodeText,
                onValueChange = { versionCodeText = it.filter { ch -> ch.isDigit() }.take(6) },
                label = { Text("Código (ej. 2)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = versionNameText,
                onValueChange = { versionNameText = it },
                label = { Text("Versión (ej. 1.1)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = downloadUrlText,
            onValueChange = { downloadUrlText = it },
            label = { Text("Enlace del APK en GitHub Releases") },
            placeholder = { Text("https://github.com/usuario/repo/releases/download/v1.1/app.apk") },
            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = releaseNotesText,
            onValueChange = { releaseNotesText = it },
            label = { Text("Novedades de esta versión") },
            placeholder = { Text("• Mejoras en reproductor\n• Corrección de errores") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Actualización obligatoria",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = "Oculta el botón \"Más tarde\" en el aviso",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
            Switch(
                checked = mandatory,
                onCheckedChange = { mandatory = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (palette.isDark) Color.Black else Color.White,
                    checkedTrackColor = palette.accent
                )
            )
        }

        val parsedCode = versionCodeText.toLongOrNull() ?: 0L
        val urlValid = downloadUrlText.trim().startsWith("https://", ignoreCase = true)
        Button(
            enabled = !isUploading && parsedCode >= 1L && versionNameText.isNotBlank() && urlValid,
            onClick = {
                onPublishAppUpdate(
                    parsedCode,
                    versionNameText.trim(),
                    downloadUrlText.trim(),
                    releaseNotesText.trim(),
                    mandatory
                ) { }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = if (palette.isDark) Color.Black else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("admin_publish_app_update_btn")
        ) {
            Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publicar aviso de actualización", fontWeight = FontWeight.Bold)
        }
    }
}
