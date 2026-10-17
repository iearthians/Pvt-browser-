package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.RuntimePrivacyAuditReport
import com.example.model.SecurityHardeningSettings

@Composable
fun PrivacySecurityPanelScreen(
    securitySettings: SecurityHardeningSettings,
    auditReport: RuntimePrivacyAuditReport?,
    onUpdateSettings: ((SecurityHardeningSettings) -> SecurityHardeningSettings) -> Unit,
    onRunLiveAudit: () -> Unit,
    onFullWipeEverything: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (auditReport == null) {
            onRunLiveAudit()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 680.dp)) {
            Text(
                text = "Privacy & Security Architecture",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Real Implemented Controls • Runtime Zero-Storage Verification • Honest Threat Model",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Live Zero-Storage & Security Self-Audit Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_audit_card"),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GppGood,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Runtime Zero-Storage & Defense Audit",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = onRunLiveAudit,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("run_live_audit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-Verify Now")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    auditReport?.let { report ->
                        Surface(
                            color = if (report.allPassed) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (report.allPassed) {
                                    "ALL ${report.assertions.size}/${report.assertions.size} RUNTIME PRIVACY & SECURITY ASSERTIONS PASSED"
                                } else {
                                    "WARNING: ONE OR MORE ASSERTIONS FAILED"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (report.allPassed) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        report.assertions.forEach { assertion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (assertion.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (assertion.passed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${assertion.title} [${assertion.category}]",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = assertion.technicalDetail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Real Security & Privacy Settings Toggles
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_settings_card"),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Hardening & Isolation Controls",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SecurityToggleItem(
                        title = "Enforce Strict HTTPS-Only Navigation",
                        description = "Rejects unencrypted http:// URLs and mixed-content subresources.",
                        checked = securitySettings.enforceHttpsOnly,
                        testTag = "toggle_https_only",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(enforceHttpsOnly = enabled) }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SecurityToggleItem(
                        title = "Block Analytics & Ad Tracker Subresources",
                        description = "Intercepts and blocks known third-party tracking domains inside WebView.",
                        checked = securitySettings.blockThirdPartyTrackers,
                        testTag = "toggle_block_trackers",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(blockThirdPartyTrackers = enabled) }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SecurityToggleItem(
                        title = "Block Private Network / SSRF Targets",
                        description = "Blocks requests and redirects to 127.0.0.1, localhost, RFC 1918 LANs, and 169.254.169.254.",
                        checked = securitySettings.blockPrivateNetworkSsrf,
                        testTag = "toggle_block_ssrf",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(blockPrivateNetworkSsrf = enabled) }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SecurityToggleItem(
                        title = "Strip Tracking Query Parameters (utm_*, fbclid, gclid)",
                        description = "Automatically removes marketing attribution parameters from URLs.",
                        checked = securitySettings.stripTrackingQueryParams,
                        testTag = "toggle_strip_params",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(stripTrackingQueryParams = enabled) }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SecurityToggleItem(
                        title = "Enable WebView JavaScript (Off by Default)",
                        description = "Keep disabled for maximum XSS & fingerprinting defense. Enable only if a trusted site requires JS.",
                        checked = securitySettings.enableWebViewJavaScript,
                        testTag = "toggle_webview_js",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(enableWebViewJavaScript = enabled) }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onFullWipeEverything,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("panic_wipe_all_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe Session, Document Workspace & Downloads Now")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Honest Threat Model, Limitations & Caching vs. Privacy Trade-Offs
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
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Honest Privacy Limitations & Threat Boundaries",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Network & ISP Visibility: GhostSearch is NOT a VPN, proxy, or Tor client. Your ISP, mobile carrier, Wi-Fi operator, and DNS resolver can observe destination domains and IP addresses.\n" +
                            "2. Search Provider Logs: Sending a search query transmits your raw query, public IP address, and TLS handshake directly to the selected search provider.\n" +
                            "3. Browser Engine & OS Artifacts: While GhostSearch disables WebView disk cache (`LOAD_NO_CACHE`), disables form/database storage, and flushes cookies on session clear, the underlying OS kernel, DNS resolver cache, zRAM/swap pages, and any files you explicitly save to device storage persist outside application control.\n" +
                            "4. Caching vs. Privacy Trade-off: Persistent HTTP caching improves repeat page-load speed but leaves forensic traces of visited assets on flash storage. GhostSearch intentionally sacrifices persistent disk caching in favor of `LOAD_NO_CACHE` memory-only operation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Network Audit Procedure, Native Browser Engine Integration & Security Policy
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Network Audit Procedure, Engine Isolation & Security Policy",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Network-Request Audit Procedure: Verify zero background telemetry by running `gradle :app:testDebugUnitTest` or inspecting traffic in an intercepting proxy: GhostSearch emits zero network packets on startup, typing, or document editing. Network packets occur strictly on explicit user Search, Direct URL navigation, or One-Click Download.\n" +
                            "• Stronger Browser-Engine Integration Roadmap: An embedded system WebView shares process-level Chromium primitives provided by the OS. For stronger desktop/mobile engine isolation, integrate a dedicated GeckoView (`org.mozilla.geckoview`) or Chromium Embedded Framework profile configured with per-session ephemeral memory partitions, strict ServiceWorker prohibition, DNS-over-HTTPS (DoH), and OS-level `seccomp-bpf` process sandboxing.\n" +
                            "• Dependency & Security Policy: Dependencies are pinned in `gradle/libs.versions.toml` with Firebase, Room, and analytics SDKs disabled. Audit updates via `gradle dependencyUpdates` and verify SHA-256 checksums before upgrading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
