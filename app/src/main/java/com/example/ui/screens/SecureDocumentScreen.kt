package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.DocumentFormat
import com.example.model.SecureDocumentState
import com.example.security.UrlSecurityValidator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecureDocumentScreen(
    documentState: SecureDocumentState,
    isDarkTheme: Boolean,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onFormatChange: (DocumentFormat) -> Unit,
    onTogglePreview: () -> Unit,
    onRequestClear: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissClear: () -> Unit,
    onExportCompleted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val exportFileName = "${documentState.documentTitle.ifBlank { "ephemeral-note" }}.${documentState.format.extension}"

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(documentState.format.mimeType)
    ) { uri ->
        if (uri != null) {
            try {
                val payloadBytes = if (documentState.format == DocumentFormat.SANDBOXED_HTML) {
                    UrlSecurityValidator.buildSandboxedDocumentPreviewHtml(
                        title = documentState.documentTitle,
                        rawBody = documentState.content,
                        isDark = isDarkTheme
                    ).toByteArray(Charsets.UTF_8)
                } else {
                    documentState.content.toByteArray(Charsets.UTF_8)
                }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payloadBytes)
                    out.flush()
                }
                onExportCompleted(exportFileName)
            } catch (_: Exception) {
                onExportCompleted("$exportFileName (export stream closed)")
            }
        }
    }

    if (documentState.showClearConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissClear,
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Unsaved content warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Clear Unsaved Volatile Document?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "This workspace does not autosave or back up drafts. Clearing will permanently wipe ${documentState.byteSize} bytes (${documentState.wordCount} words) from application RAM.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmClear,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_clear_doc_button")
                ) {
                    Text("Wipe Document RAM")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissClear,
                    modifier = Modifier.testTag("cancel_clear_doc_button")
                ) {
                    Text("Keep Editing")
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
        Column(
            modifier = Modifier.widthIn(max = 680.dp)
        ) {
            // Header & Zero-Autosave Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Secure Document Workspace",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Zero Autosave • Zero Cloud Sync • Zero Spell-Check Telemetry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline RAM only",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${documentState.byteSize} B RAM",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("doc_ram_byte_counter")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Format Selector & Sandboxed CSP Preview Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = documentState.documentTitle,
                            onValueChange = onTitleChange,
                            label = { Text("Export Filename Base") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("doc_title_input")
                        )

                        OutlinedButton(
                            onClick = onTogglePreview,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("doc_toggle_preview_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = if (documentState.isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (documentState.isPreviewMode) "Edit Raw" else "CSP Preview")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DocumentFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = documentState.format == fmt,
                                onClick = { onFormatChange(fmt) },
                                label = { Text(fmt.label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.testTag("doc_format_${fmt.extension}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (documentState.isPreviewMode) {
                        SandboxedCspDocumentPreview(
                            title = documentState.documentTitle,
                            rawContent = documentState.content,
                            isDark = isDarkTheme
                        )
                    } else {
                        OutlinedTextField(
                            value = documentState.content,
                            onValueChange = onContentChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp, max = 380.dp)
                                .testTag("doc_editor_input"),
                            placeholder = {
                                Text(
                                    text = "Type confidential notes here. Content lives exclusively in volatile application memory — never written to localStorage, SQLite, SharedPreferences, or external AI/spell-check APIs…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Telemetry & Word/Char Counters + Revoked Temp Stream Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${documentState.wordCount} words • ${documentState.charCount} chars • ${if (documentState.hasUnsavedChanges) "Unsaved in RAM" else "Clean"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Revoked Temp Buffers: ${documentState.revokedTempUriCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons: Manual Export vs Clear Document
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (documentState.content.isBlank()) {
                                    onExportCompleted("$exportFileName (0-byte memory buffer revoked)")
                                } else {
                                    createDocumentLauncher.launch(exportFileName)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("doc_export_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export / Download (.$exportFileName)")
                        }

                        OutlinedButton(
                            onClick = onRequestClear,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("doc_clear_button"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Document")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = documentState.lastExportNotice != null) {
                documentState.lastExportNotice?.let { notice ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("doc_status_banner"),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Honest OS / Memory / Swap / Clipboard Limitation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Document Memory & OS Retention Boundaries",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Strict Content Security Policy: Preview renders with `default-src 'none'; script-src 'none'; connect-src 'none'` and HTML entity encoding to prevent XSS.\n" +
                            "• Zero Background Sync: Drafts are never sent to AI services, cloud spell-checkers, or local databases.\n" +
                            "• Honest OS Limitations: While GhostSearch wipes its own memory references on Clear, operating-system memory pages, zRAM/swap partitions, crash dumps, system clipboard history (if you copy/paste), and soft-keyboard (IME) caches remain controlled by the OS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SandboxedCspDocumentPreview(
    title: String,
    rawContent: String,
    isDark: Boolean
) {
    val htmlPayload = UrlSecurityValidator.buildSandboxedDocumentPreviewHtml(
        title = title,
        rawBody = rawContent.ifEmpty { "(Empty volatile document)" },
        isDark = isDark
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .testTag("doc_csp_preview_box"),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = false
                        domStorageEnabled = false
                        databaseEnabled = false
                        allowFileAccess = false
                        allowContentAccess = false
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        blockNetworkLoads = true
                        blockNetworkImage = true
                    }
                    loadDataWithBaseURL(
                        "about:blank",
                        htmlPayload,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    "about:blank",
                    htmlPayload,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        )
    }
}
