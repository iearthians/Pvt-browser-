package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GhostTab
import com.example.ui.screens.PrivacySecurityPanelScreen
import com.example.ui.screens.SafeDownloadsScreen
import com.example.ui.screens.SearchBrowserScreen
import com.example.ui.screens.SecureDocumentScreen
import com.example.ui.theme.GhostSearchTheme
import com.example.viewmodel.GhostSearchViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GhostSearchViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            GhostSearchTheme(darkTheme = isDarkTheme) {
                GhostSearchApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhostSearchApp(
    viewModel: GhostSearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val securitySettings by viewModel.securitySettings.collectAsStateWithLifecycle()
    val documentState by viewModel.documentState.collectAsStateWithLifecycle()
    val downloadInputUrl by viewModel.downloadInputUrl.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val auditReport by viewModel.auditReport.collectAsStateWithLifecycle()

    if (currentTab != GhostTab.SEARCH) {
        BackHandler {
            viewModel.selectTab(GhostTab.SEARCH)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "GhostSearch",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Ephemeral Session #${sessionState.sessionGeneration} • RAM-Only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.clearSession(includeWorkspaceAndDownloads = false) },
                            modifier = Modifier.testTag("top_bar_clear_session_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Ephemeral Session",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        GhostTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        hasUnsavedDoc = documentState.hasUnsavedChanges,
                                        activeDownloadsCount = downloads.size
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        GhostTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        hasUnsavedDoc = documentState.hasUnsavedChanges,
                                        activeDownloadsCount = downloads.size
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentTab) {
                        GhostTab.SEARCH -> SearchBrowserScreen(
                            sessionState = sessionState,
                            securitySettings = securitySettings,
                            onQueryOrUrlChange = viewModel::updateSearchOrUrlInput,
                            onSelectProvider = viewModel::selectSearchProvider,
                            onSubmitSearch = { viewModel.submitSearchOrUrl(forceDirectUrl = false) },
                            onSubmitDirectUrl = { viewModel.submitSearchOrUrl(forceDirectUrl = true) },
                            onClearSession = { viewModel.clearSession(includeWorkspaceAndDownloads = false) },
                            onReturnHome = viewModel::returnToSearchHome,
                            onWebViewStarted = viewModel::onWebViewPageStarted,
                            onWebViewProgress = viewModel::onWebViewProgress,
                            onWebViewFinished = viewModel::onWebViewPageFinished,
                            onWebViewError = viewModel::onWebViewNavigationError,
                            onTrackerBlocked = viewModel::recordBlockedTracker,
                            onUnsafeNavigationBlocked = viewModel::recordBlockedUnsafeNavigation,
                            onSendUrlToSafeDownloader = { url ->
                                viewModel.updateDownloadInputUrl(url)
                                viewModel.selectTab(GhostTab.DOWNLOADS)
                                viewModel.startOneClickDownload(url)
                            }
                        )

                        GhostTab.WORKSPACE -> SecureDocumentScreen(
                            documentState = documentState,
                            isDarkTheme = isDarkTheme,
                            onTitleChange = viewModel::updateDocumentTitle,
                            onContentChange = viewModel::updateDocumentContent,
                            onFormatChange = viewModel::selectDocumentFormat,
                            onTogglePreview = viewModel::toggleDocumentPreviewMode,
                            onRequestClear = viewModel::requestClearDocument,
                            onConfirmClear = viewModel::confirmClearDocument,
                            onDismissClear = viewModel::dismissClearDocumentConfirmation,
                            onExportCompleted = viewModel::onDocumentExportCompleted
                        )

                        GhostTab.DOWNLOADS -> SafeDownloadsScreen(
                            downloadInputUrl = downloadInputUrl,
                            downloads = downloads,
                            presets = viewModel.presetDownloads,
                            onUrlChange = viewModel::updateDownloadInputUrl,
                            onStartDownload = { url -> viewModel.startOneClickDownload(url) },
                            onConfirmExecutable = viewModel::confirmExecutableDownload,
                            onDismissExecutable = viewModel::dismissExecutableDownload,
                            onCancelDownload = viewModel::cancelDownload,
                            onRemoveDownload = viewModel::removeDownloadTask,
                            onClearAllDownloads = viewModel::clearAllDownloadsFromMemory
                        )

                        GhostTab.PRIVACY_AUDIT -> PrivacySecurityPanelScreen(
                            securitySettings = securitySettings,
                            auditReport = auditReport,
                            onUpdateSettings = viewModel::updateSecuritySettings,
                            onRunLiveAudit = { viewModel.runRuntimePrivacyAudit(context) },
                            onFullWipeEverything = {
                                viewModel.clearSession(includeWorkspaceAndDownloads = true)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: GhostTab,
    selected: Boolean,
    hasUnsavedDoc: Boolean,
    activeDownloadsCount: Int
) {
    val icon: ImageVector = when (tab) {
        GhostTab.SEARCH -> if (selected) Icons.Filled.Search else Icons.Outlined.Search
        GhostTab.WORKSPACE -> if (selected) Icons.Filled.Description else Icons.Outlined.Description
        GhostTab.DOWNLOADS -> if (selected) Icons.Filled.Download else Icons.Outlined.Download
        GhostTab.PRIVACY_AUDIT -> if (selected) Icons.Filled.Shield else Icons.Outlined.Shield
    }

    BadgedBox(
        badge = {
            if (tab == GhostTab.WORKSPACE && hasUnsavedDoc) {
                Badge()
            } else if (tab == GhostTab.DOWNLOADS && activeDownloadsCount > 0) {
                Badge { Text(activeDownloadsCount.toString()) }
            }
        }
    ) {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}
