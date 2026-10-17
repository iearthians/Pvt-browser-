package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DownloadLifecycleState
import com.example.model.DownloadTaskItem
import com.example.model.PresetDownloadSample

@Composable
fun SafeDownloadsScreen(
    downloadInputUrl: String,
    downloads: List<DownloadTaskItem>,
    presets: List<PresetDownloadSample>,
    onUrlChange: (String) -> Unit,
    onStartDownload: (String) -> Unit,
    onConfirmExecutable: (String) -> Unit,
    onDismissExecutable: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onClearAllDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var pendingSaveTask by remember { mutableStateOf<DownloadTaskItem?>(null) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val task = pendingSaveTask
        if (uri != null && task?.savedMemoryBuffer != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(task.savedMemoryBuffer)
                    out.flush()
                }
            } catch (_: Exception) {
            }
        }
        pendingSaveTask = null
    }

    // Explicit User Confirmation Modal for any Executable / Potentially Dangerous File
    val awaitingExecutableTask = downloads.firstOrNull {
        it.state == DownloadLifecycleState.AWAITING_EXECUTABLE_CONFIRMATION
    }

    if (awaitingExecutableTask != null) {
        AlertDialog(
            onDismissRequest = { onDismissExecutable(awaitingExecutableTask.id) },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Executable Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Executable / Dangerous File Blocked",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = awaitingExecutableTask.dangerWarningReason
                            ?: "This file has an executable or script signature.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sanitized Filename: ${awaitingExecutableTask.sanitizedFileName}\n" +
                            "Source URL: ${awaitingExecutableTask.finalResolvedUrl}\n\n" +
                            "GhostSearch never auto-executes downloaded files. Only proceed if you explicitly trust the publisher and verify the SHA-256 hash.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmExecutable(awaitingExecutableTask.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_executable_download_button")
                ) {
                    Text("I Understand — Download Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onDismissExecutable(awaitingExecutableTask.id) },
                    modifier = Modifier.testTag("abort_executable_download_button")
                ) {
                    Text("Cancel Download (Recommended)")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 680.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Safe One-Click HTTPS Downloads",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "HTTPS-Only • Hop-by-Hop SSRF Guard • Filename Sanitizer • SHA-256 Digest",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (downloads.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearAllDownloads,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clear_all_downloads_button")
                    ) {
                        Text("Wipe List")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Direct HTTPS Download Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Direct Authorized HTTPS File URL",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = downloadInputUrl,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_url_input"),
                        placeholder = { Text("https://example.org/public-document.txt") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "HTTPS required",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onStartDownload(downloadInputUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("one_click_download_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("One-Click Safe HTTPS Download")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active & Completed Downloads List
            if (downloads.isNotEmpty()) {
                Text(
                    text = "Download Queue & Integrity Log (${downloads.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                downloads.forEach { item ->
                    DownloadTaskCard(
                        item = item,
                        onCancel = { onCancelDownload(item.id) },
                        onRemove = { onRemoveDownload(item.id) },
                        onSaveToDisk = {
                            pendingSaveTask = item
                            saveFileLauncher.launch(item.sanitizedFileName)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // One-Click Verified Public Specs & Guardrail Test Cases
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "One-Click Public Standards & Security Guardrail Presets",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Only download publicly accessible files or files you are authorized to access. GhostSearch never bypasses paywalls, DRM, or authentication controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    presets.forEachIndexed { idx, preset ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(
                                1.dp,
                                if (preset.isExecutableDemo) {
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        onUrlChange(preset.url)
                                        onStartDownload(preset.url)
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("preset_download_button_$idx"),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (preset.isExecutableDemo) Icons.Default.WarningAmber else Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (preset.isExecutableDemo) "Test Guard" else "Download",
                                        style = MaterialTheme.typography.labelLarge
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

@Composable
private fun DownloadTaskCard(
    item: DownloadTaskItem,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onSaveToDisk: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_task_card_${item.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when (item.state) {
                DownloadLifecycleState.COMPLETED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                DownloadLifecycleState.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                DownloadLifecycleState.AWAITING_EXECUTABLE_CONFIRMATION -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (item.state) {
                            DownloadLifecycleState.COMPLETED -> Icons.Default.CheckCircle
                            DownloadLifecycleState.FAILED -> Icons.Default.ErrorOutline
                            DownloadLifecycleState.AWAITING_EXECUTABLE_CONFIRMATION -> Icons.Default.WarningAmber
                            else -> Icons.Default.FilePresent
                        },
                        contentDescription = null,
                        tint = when (item.state) {
                            DownloadLifecycleState.COMPLETED -> MaterialTheme.colorScheme.secondary
                            DownloadLifecycleState.FAILED -> MaterialTheme.colorScheme.error
                            DownloadLifecycleState.AWAITING_EXECUTABLE_CONFIRMATION -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.sanitizedFileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("remove_download_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove task"
                    )
                }
            }

            Text(
                text = "MIME: ${item.mimeType} • Size: ${formatBytes(item.downloadedBytes, item.totalBytes)} • Status: ${item.state.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.redirectChain.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verified HTTPS Redirect Hops (${item.redirectChain.size - 1}): ${item.redirectChain.joinToString(" → ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (item.state == DownloadLifecycleState.DOWNLOADING || item.state == DownloadLifecycleState.VALIDATING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.totalBytes > 0L) {
                        LinearProgressIndicator(
                            progress = { item.progressFraction },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("cancel_download_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel download",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
            }

            item.sha256Hex?.let { sha ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SHA-256: $sha",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            item.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (item.state == DownloadLifecycleState.COMPLETED && item.savedMemoryBuffer != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No auto-execution. Verified in volatile RAM buffer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onSaveToDisk,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("save_completed_download_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Device")
                    }
                }
            }
        }
    }
}

private fun formatBytes(downloaded: Long, total: Long): String {
    val dKb = downloaded / 1024.0
    return if (total > 0) {
        val tKb = total / 1024.0
        String.format("%.1f KB / %.1f KB", dKb, tKb)
    } else {
        String.format("%.1f KB", dKb)
    }
}
