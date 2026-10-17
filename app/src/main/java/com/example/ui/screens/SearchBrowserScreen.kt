package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.EphemeralSessionState
import com.example.model.PrivacyTier
import com.example.model.SearchProvider
import com.example.model.SecurityHardeningSettings
import com.example.security.UrlSecurityValidator
import com.example.security.UrlValidationResult
import java.io.ByteArrayInputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchBrowserScreen(
    sessionState: EphemeralSessionState,
    securitySettings: SecurityHardeningSettings,
    onQueryOrUrlChange: (String) -> Unit,
    onSelectProvider: (SearchProvider) -> Unit,
    onSubmitSearch: () -> Unit,
    onSubmitDirectUrl: () -> Unit,
    onClearSession: () -> Unit,
    onReturnHome: () -> Unit,
    onWebViewStarted: (String) -> Unit,
    onWebViewProgress: (Int) -> Unit,
    onWebViewFinished: (String, String?) -> Unit,
    onWebViewError: (String) -> Unit,
    onTrackerBlocked: () -> Unit,
    onUnsafeNavigationBlocked: (String) -> Unit,
    onSendUrlToSafeDownloader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    if (sessionState.activeWebUrl != null) {
        BackHandler {
            onReturnHome()
        }
        EphemeralWebViewContainer(
            sessionState = sessionState,
            securitySettings = securitySettings,
            onReturnHome = onReturnHome,
            onClearSession = onClearSession,
            onWebViewStarted = onWebViewStarted,
            onWebViewProgress = onWebViewProgress,
            onWebViewFinished = onWebViewFinished,
            onWebViewError = onWebViewError,
            onTrackerBlocked = onTrackerBlocked,
            onUnsafeNavigationBlocked = onUnsafeNavigationBlocked,
            onSendUrlToSafeDownloader = onSendUrlToSafeDownloader,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Private Session Status Pill + Ephemeral Counter
            EphemeralSessionBadgeRow(
                sessionState = sessionState,
                onClearSession = onClearSession
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Minimalist Search Hero Emblem & Title
            GhostHeroHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // Centered Search & Direct URL Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_card_container"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = sessionState.queryOrUrlInput,
                        onValueChange = onQueryOrUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_query_input"),
                        placeholder = {
                            Text(
                                text = "Search via ${sessionState.selectedProvider.displayName} or enter https:// URL…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search input icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (sessionState.queryOrUrlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryOrUrlChange("") },
                                    modifier = Modifier.testTag("clear_input_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search box"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onSubmitSearch()
                            }
                        ),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Row: Search Provider vs Direct HTTPS URL Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onSubmitSearch()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("search_submit_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onSubmitDirectUrl()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("direct_url_button"),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Direct URL",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Error / Security Rejection Alert Banner
            AnimatedVisibility(visible = sessionState.errorMessage != null) {
                sessionState.errorMessage?.let { errorMsg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("security_error_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Security error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Transient Session Status Notice
            AnimatedVisibility(visible = sessionState.statusNotice != null && sessionState.errorMessage == null) {
                sessionState.statusNotice?.let { notice ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("session_status_notice"),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Configurable Search Provider Selector + Honest Provider Disclosure Card
            SearchProviderSelectorSection(
                selectedProvider = sessionState.selectedProvider,
                onSelectProvider = onSelectProvider
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Visible Privacy Status Panel (Real Implemented Controls + Honest Limitations)
            VisiblePrivacyStatusPanel(
                sessionState = sessionState,
                securitySettings = securitySettings,
                onClearSession = onClearSession
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EphemeralSessionBadgeRow(
    sessionState: EphemeralSessionState,
    onClearSession: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier.testTag("private_session_indicator")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Private ephemeral session active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RAM Session #${sessionState.sessionGeneration} • Zero History DB",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        OutlinedButton(
            onClick = onClearSession,
            modifier = Modifier
                .height(48.dp)
                .testTag("clear_session_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Clear Session",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Clear Session",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun GhostHeroHeader() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    radius = radius
                )
                drawCircle(
                    color = surfaceVariantColor,
                    radius = radius * 0.72f
                )
                drawCircle(
                    color = primaryColor,
                    radius = radius * 0.72f,
                    style = Stroke(width = 3f)
                )
                // Minimalist radar/lens crosshair lines
                drawLine(
                    color = secondaryColor.copy(alpha = 0.65f),
                    start = Offset(center.x, center.y - radius * 0.52f),
                    end = Offset(center.x, center.y - radius * 0.28f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondaryColor.copy(alpha = 0.65f),
                    start = Offset(center.x, center.y + radius * 0.28f),
                    end = Offset(center.x, center.y + radius * 0.52f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "GhostSearch Emblem",
                tint = primaryColor,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "GhostSearch",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ephemeral Search • Zero Local History • Honest Threat Disclosure",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchProviderSelectorSection(
    selectedProvider: SearchProvider,
    onSelectProvider: (SearchProvider) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_provider_section"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configurable Search Provider",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = when (selectedProvider.privacyTier) {
                        PrivacyTier.PRIVACY_FOCUSED -> MaterialTheme.colorScheme.secondaryContainer
                        PrivacyTier.INDEPENDENT_INDEX -> MaterialTheme.colorScheme.primaryContainer
                        PrivacyTier.MAINSTREAM_AD_TECH -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = selectedProvider.privacyTier.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (selectedProvider.privacyTier) {
                            PrivacyTier.PRIVACY_FOCUSED -> MaterialTheme.colorScheme.onSecondaryContainer
                            PrivacyTier.INDEPENDENT_INDEX -> MaterialTheme.colorScheme.onPrimaryContainer
                            PrivacyTier.MAINSTREAM_AD_TECH -> MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SearchProvider.entries.forEach { provider ->
                    val isSelected = provider == selectedProvider
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectProvider(provider) },
                        label = {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("provider_chip_${provider.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explicit Provider Disclosure Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("provider_disclosure_box"),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedProvider.privacyTier == PrivacyTier.MAINSTREAM_AD_TECH) {
                                Icons.Default.WarningAmber
                            } else {
                                Icons.Default.Info
                            },
                            contentDescription = "Provider disclosure",
                            tint = if (selectedProvider.privacyTier == PrivacyTier.MAINSTREAM_AD_TECH) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recipient Endpoint: https://${selectedProvider.host}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedProvider.disclosureSummary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "What ${selectedProvider.displayName} sees: ${selectedProvider.dataReceivedByProvider}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Logging & Retention: ${selectedProvider.loggingAndRetentionNotice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VisiblePrivacyStatusPanel(
    sessionState: EphemeralSessionState,
    securitySettings: SecurityHardeningSettings,
    onClearSession: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visible_privacy_status_panel"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Privacy & Telemetry Status",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Real Controls",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Verified Active Controls Grid
            PrivacyControlStatusRow(
                label = "Local Search History / URL Database",
                status = "DISABLED (0 SQLite / 0 Prefs)",
                isPositive = true
            )
            PrivacyControlStatusRow(
                label = "Transport Encryption Policy",
                status = if (securitySettings.enforceHttpsOnly) "HTTPS-Only Enforced" else "HTTP Allowed (Warning)",
                isPositive = securitySettings.enforceHttpsOnly
            )
            PrivacyControlStatusRow(
                label = "Tracker & Analytics Subresource Blocker",
                status = if (securitySettings.blockThirdPartyTrackers) {
                    "ACTIVE (${sessionState.blockedTrackersInSession} blocked)"
                } else {
                    "DISABLED"
                },
                isPositive = securitySettings.blockThirdPartyTrackers
            )
            PrivacyControlStatusRow(
                label = "Tracking Parameter Sanitizer (utm_*, fbclid)",
                status = if (securitySettings.stripTrackingQueryParams) {
                    "ACTIVE (${sessionState.strippedParamsInSession} stripped)"
                } else {
                    "DISABLED"
                },
                isPositive = securitySettings.stripTrackingQueryParams
            )
            PrivacyControlStatusRow(
                label = "WebView JavaScript Execution",
                status = if (securitySettings.enableWebViewJavaScript) {
                    "ENABLED (Higher Fingerprinting Risk)"
                } else {
                    "BLOCKED BY DEFAULT (Hardened)"
                },
                isPositive = !securitySettings.enableWebViewJavaScript
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Honest Non-Anonymity & External Trace Disclosure
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Honest privacy boundaries",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Honest Privacy Boundaries (Not a VPN or Tor)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "GhostSearch does not guarantee 100% anonymity. While application-managed search history, URLs, and drafts live only in volatile RAM, external observers — including your ISP, DNS resolver, selected search provider, visited websites, OS-level network/DNS caches, and downloaded files — can still observe or retain traces outside this application's control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyControlStatusRow(
    label: String,
    status: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = if (isPositive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EphemeralWebViewContainer(
    sessionState: EphemeralSessionState,
    securitySettings: SecurityHardeningSettings,
    onReturnHome: () -> Unit,
    onClearSession: () -> Unit,
    onWebViewStarted: (String) -> Unit,
    onWebViewProgress: (Int) -> Unit,
    onWebViewFinished: (String, String?) -> Unit,
    onWebViewError: (String) -> Unit,
    onTrackerBlocked: () -> Unit,
    onUnsafeNavigationBlocked: (String) -> Unit,
    onSendUrlToSafeDownloader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember(sessionState.sessionGeneration) { mutableStateOf<WebView?>(null) }

    Column(modifier = modifier) {
        // Ephemeral Browser Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onReturnHome,
                        modifier = Modifier.testTag("webview_back_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to GhostSearch Home"
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "HTTPS Active",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sessionState.activeWebUrl ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.testTag("webview_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload page"
                        )
                    }

                    IconButton(
                        onClick = onClearSession,
                        modifier = Modifier.testTag("webview_clear_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Session and Close",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (sessionState.isLoading) {
                    LinearProgressIndicator(
                        progress = { (sessionState.loadProgressPercent / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .testTag("webview_loading_indicator"),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Error Banner inside WebView if a navigation or subresource was blocked
        AnimatedVisibility(visible = sessionState.errorMessage != null) {
            sessionState.errorMessage?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Hardened Ephemeral WebView keyed by sessionGeneration so Clear Session destroys & recreates it
        key(sessionState.sessionGeneration) {
            DisposableEffect(sessionState.sessionGeneration) {
                onDispose {
                    webViewRef?.apply {
                        stopLoading()
                        clearHistory()
                        clearCache(true)
                        clearFormData()
                        loadUrl("about:blank")
                        removeAllViews()
                        destroy()
                    }
                    webViewRef = null
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("ephemeral_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        // Hardened WebSettings
                        settings.apply {
                            javaScriptEnabled = securitySettings.enableWebViewJavaScript
                            domStorageEnabled = securitySettings.enableWebViewDomStorage
                            databaseEnabled = false
                            saveFormData = false
                            allowFileAccess = false
                            allowContentAccess = false
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            setSupportMultipleWindows(false)
                            javaScriptCanOpenWindowsAutomatically = false
                            mediaPlaybackRequiresUserGesture = true
                            setGeolocationEnabled(false)
                        }

                        CookieManager.getInstance().setAcceptCookie(false)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

                        // Intercept downloads and route to GhostSearch Safe Download Manager
                        setDownloadListener { url, _, _, _, _ ->
                            if (!url.isNullOrBlank()) {
                                onSendUrlToSafeDownloader(url)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                onWebViewProgress(newProgress)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUri = request?.url ?: return null
                                val scheme = reqUri.scheme?.lowercase() ?: ""
                                val host = reqUri.host?.lowercase() ?: ""

                                // Block non-HTTPS subresources when strict HTTPS is enabled
                                if (securitySettings.enforceHttpsOnly && scheme != "https") {
                                    return emptyBlockedResponse()
                                }

                                // Block SSRF / private network subresource fetches
                                if (securitySettings.blockPrivateNetworkSsrf &&
                                    UrlSecurityValidator.isPrivateOrLoopbackHost(host)
                                ) {
                                    return emptyBlockedResponse()
                                }

                                // Block known third-party analytics & ad trackers
                                if (securitySettings.blockThirdPartyTrackers &&
                                    UrlSecurityValidator.isKnownTrackerHost(host)
                                ) {
                                    onTrackerBlocked()
                                    return emptyBlockedResponse()
                                }

                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val targetUrl = request?.url?.toString() ?: return true
                                val validation = UrlSecurityValidator.validateDirectUrl(
                                    rawUrl = targetUrl,
                                    enforceHttpsOnly = securitySettings.enforceHttpsOnly,
                                    blockPrivateNetworkSsrf = securitySettings.blockPrivateNetworkSsrf,
                                    stripTrackingParams = securitySettings.stripTrackingQueryParams
                                )
                                return when (validation) {
                                    is UrlValidationResult.ValidDirectUrl -> {
                                        if (validation.normalizedUrl != targetUrl) {
                                            view?.loadUrl(validation.normalizedUrl)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    is UrlValidationResult.Rejected -> {
                                        onUnsafeNavigationBlocked("${validation.reasonTitle}: ${validation.technicalDetail}")
                                        true
                                    }
                                    else -> true
                                }
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                if (url != null) onWebViewStarted(url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                // Explicitly clear back/forward history in WebView engine on every load
                                view?.clearHistory()
                                if (url != null) onWebViewFinished(url, view?.title)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    onWebViewError(
                                        "Navigation error: ${error?.description ?: "Connection failed or blocked"}"
                                    )
                                }
                            }
                        }

                        sessionState.activeWebUrl?.let { loadUrl(it) }
                    }
                },
                update = { webView ->
                    webView.settings.javaScriptEnabled = securitySettings.enableWebViewJavaScript
                    webView.settings.domStorageEnabled = securitySettings.enableWebViewDomStorage
                    val target = sessionState.activeWebUrl
                    if (target != null && webView.url != target && !sessionState.isLoading) {
                        webView.loadUrl(target)
                    }
                }
            )
        }
    }
}

private fun emptyBlockedResponse(): WebResourceResponse {
    return WebResourceResponse(
        "text/plain",
        "UTF-8",
        403,
        "Blocked by GhostSearch Privacy Policy",
        mapOf(
            "Content-Security-Policy" to "default-src 'none'",
            "X-Content-Type-Options" to "nosniff"
        ),
        ByteArrayInputStream(ByteArray(0))
    )
}
