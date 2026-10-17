package com.example.viewmodel

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DocumentFormat
import com.example.model.DownloadLifecycleState
import com.example.model.DownloadTaskItem
import com.example.model.EphemeralSessionState
import com.example.model.GhostTab
import com.example.model.PresetDownloadSample
import com.example.model.RuntimePrivacyAuditReport
import com.example.model.SearchProvider
import com.example.model.SecureDocumentState
import com.example.model.SecurityHardeningSettings
import com.example.security.PrivacyStorageAuditor
import com.example.security.SafeDownloadManager
import com.example.security.UrlSecurityValidator
import com.example.security.UrlValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class GhostSearchViewModel(
    private val safeDownloadManager: SafeDownloadManager = SafeDownloadManager()
) : ViewModel() {

    private val downloadIdSequence = AtomicInteger(1)

    private val _currentTab = MutableStateFlow(GhostTab.SEARCH)
    val currentTab: StateFlow<GhostTab> = _currentTab.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _securitySettings = MutableStateFlow(SecurityHardeningSettings())
    val securitySettings: StateFlow<SecurityHardeningSettings> = _securitySettings.asStateFlow()

    private val _sessionState = MutableStateFlow(EphemeralSessionState())
    val sessionState: StateFlow<EphemeralSessionState> = _sessionState.asStateFlow()

    private val _documentState = MutableStateFlow(SecureDocumentState())
    val documentState: StateFlow<SecureDocumentState> = _documentState.asStateFlow()

    private val _downloadInputUrl = MutableStateFlow("https://www.rfc-editor.org/rfc/rfc9116.txt")
    val downloadInputUrl: StateFlow<String> = _downloadInputUrl.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadTaskItem>>(emptyList())
    val downloads: StateFlow<List<DownloadTaskItem>> = _downloads.asStateFlow()

    private val _auditReport = MutableStateFlow<RuntimePrivacyAuditReport?>(null)
    val auditReport: StateFlow<RuntimePrivacyAuditReport?> = _auditReport.asStateFlow()

    val presetDownloads: List<PresetDownloadSample> = listOf(
        PresetDownloadSample(
            title = "RFC 9116 — security.txt Standard",
            description = "Public IETF specification (plain text, ~24 KB) over HTTPS.",
            url = "https://www.rfc-editor.org/rfc/rfc9116.txt",
            badge = "Verified TXT"
        ),
        PresetDownloadSample(
            title = "RFC 7626 — DNS Privacy Considerations",
            description = "Public IETF DNS privacy architecture analysis (plain text, ~49 KB).",
            url = "https://www.rfc-editor.org/rfc/rfc7626.txt",
            badge = "Verified TXT"
        ),
        PresetDownloadSample(
            title = "W3C Security & Privacy Questionnaire",
            description = "Public W3C technical specification page over HTTPS.",
            url = "https://www.w3.org/TR/security-privacy-questionnaire/",
            badge = "Verified HTML"
        ),
        PresetDownloadSample(
            title = "Guardrail Test: Executable Package (.apk)",
            description = "Tests pre-flight executable detection & explicit user confirmation modal.",
            url = "https://example.com/releases/ghost-installer-v1.0.apk",
            badge = "Executable Guardrail",
            isExecutableDemo = true
        ),
        PresetDownloadSample(
            title = "Guardrail Test: Deceptive Double Extension (.pdf.exe)",
            description = "Tests detection of disguised binary filenames before network transfer.",
            url = "https://example.com/files/quarterly_statement.pdf.exe",
            badge = "Deceptive Filename",
            isExecutableDemo = true
        )
    )

    fun selectTab(tab: GhostTab) {
        _currentTab.value = tab
    }

    fun toggleTheme() {
        _isDarkTheme.update { !it }
    }

    fun updateSearchOrUrlInput(raw: String) {
        _sessionState.update {
            it.copy(
                queryOrUrlInput = raw,
                errorMessage = null,
                statusNotice = null
            )
        }
    }

    fun selectSearchProvider(provider: SearchProvider) {
        _sessionState.update {
            it.copy(
                selectedProvider = provider,
                errorMessage = null,
                statusNotice = "Search provider set to ${provider.displayName} (${provider.host})."
            )
        }
    }

    fun submitSearchOrUrl(forceDirectUrl: Boolean = false) {
        val currentSession = _sessionState.value
        val settings = _securitySettings.value

        val validation = UrlSecurityValidator.validateSearchOrNavigation(
            rawInput = currentSession.queryOrUrlInput,
            provider = currentSession.selectedProvider,
            enforceHttpsOnly = settings.enforceHttpsOnly,
            blockPrivateNetworkSsrf = settings.blockPrivateNetworkSsrf,
            stripTrackingParams = settings.stripTrackingQueryParams,
            forceDirectUrl = forceDirectUrl
        )

        when (validation) {
            is UrlValidationResult.ValidDirectUrl -> {
                _sessionState.update {
                    it.copy(
                        activeWebUrl = validation.normalizedUrl,
                        activePageTitle = validation.host,
                        isLoading = true,
                        loadProgressPercent = 10,
                        errorMessage = null,
                        strippedParamsInSession = it.strippedParamsInSession + validation.strippedTrackingParamsCount,
                        statusNotice = if (validation.strippedTrackingParamsCount > 0) {
                            "Stripped ${validation.strippedTrackingParamsCount} tracking parameter(s) before HTTPS navigation."
                        } else {
                            "Opening direct HTTPS connection to ${validation.host}."
                        }
                    )
                }
            }
            is UrlValidationResult.ValidSearchQuery -> {
                _sessionState.update {
                    it.copy(
                        activeWebUrl = validation.targetSearchUrl,
                        activePageTitle = "${validation.provider.displayName} Search",
                        isLoading = true,
                        loadProgressPercent = 10,
                        errorMessage = null,
                        statusNotice = "Dispatched query over HTTPS to ${validation.provider.host}."
                    )
                }
            }
            is UrlValidationResult.Rejected -> {
                _sessionState.update {
                    it.copy(
                        isLoading = false,
                        loadProgressPercent = 0,
                        blockedUnsafeSchemesInSession = it.blockedUnsafeSchemesInSession + 1,
                        errorMessage = "${validation.reasonTitle}: ${validation.technicalDetail}"
                    )
                }
            }
        }
    }

    fun returnToSearchHome() {
        _sessionState.update {
            it.copy(
                activeWebUrl = null,
                activePageTitle = null,
                isLoading = false,
                loadProgressPercent = 0,
                errorMessage = null
            )
        }
    }

    fun onWebViewPageStarted(url: String) {
        _sessionState.update {
            it.copy(
                activeWebUrl = url,
                isLoading = true,
                errorMessage = null
            )
        }
    }

    fun onWebViewProgress(progress: Int) {
        _sessionState.update {
            it.copy(
                loadProgressPercent = progress.coerceIn(0, 100),
                isLoading = progress < 100
            )
        }
    }

    fun onWebViewPageFinished(url: String, title: String?) {
        _sessionState.update {
            it.copy(
                activeWebUrl = url,
                activePageTitle = title?.takeIf { t -> t.isNotBlank() } ?: it.activePageTitle,
                isLoading = false,
                loadProgressPercent = 100
            )
        }
    }

    fun onWebViewNavigationError(message: String) {
        _sessionState.update {
            it.copy(
                isLoading = false,
                loadProgressPercent = 0,
                errorMessage = message
            )
        }
    }

    fun recordBlockedTracker() {
        _sessionState.update {
            it.copy(blockedTrackersInSession = it.blockedTrackersInSession + 1)
        }
    }

    fun recordBlockedUnsafeNavigation(reason: String) {
        _sessionState.update {
            it.copy(
                blockedUnsafeSchemesInSession = it.blockedUnsafeSchemesInSession + 1,
                errorMessage = reason
            )
        }
    }

    /**
     * Immediately wipes all temporary in-memory session state, cancels any active downloads
     * if requested, and flushes Android WebView RAM cookies & storage without writing logs.
     */
    fun clearSession(includeWorkspaceAndDownloads: Boolean = false) {
        val nextGeneration = _sessionState.value.sessionGeneration + 1
        val currentProvider = _sessionState.value.selectedProvider
        val now = System.currentTimeMillis()

        // Best-effort flush of WebView engine cookies & web storage if initialized in process
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        } catch (_: Throwable) {
            // Safe fallback when running in JVM unit test without full Chromium engine
        }

        _sessionState.value = EphemeralSessionState(
            sessionGeneration = nextGeneration,
            sessionStartedEpochMs = now,
            queryOrUrlInput = "",
            selectedProvider = currentProvider,
            activeWebUrl = null,
            activePageTitle = null,
            isLoading = false,
            loadProgressPercent = 0,
            errorMessage = null,
            blockedTrackersInSession = 0,
            strippedParamsInSession = 0,
            blockedUnsafeSchemesInSession = 0,
            lastClearedEpochMs = now,
            statusNotice = "Ephemeral Session #$nextGeneration started. Previous RAM state, WebView cache, and session cookies wiped."
        )

        if (includeWorkspaceAndDownloads) {
            safeDownloadManager.cancelAllDownloads()
            _downloads.value = emptyList()
            _documentState.update { prev ->
                SecureDocumentState(
                    revokedTempUriCount = prev.revokedTempUriCount + 1,
                    lastExportNotice = "Document workspace wiped during full session reset."
                )
            }
        }
    }

    // --- Secure Document Workspace ---

    fun updateDocumentTitle(rawTitle: String) {
        val sanitized = rawTitle.replace(Regex("[\\r\\n/\\\\]"), "").take(80)
        _documentState.update {
            it.copy(
                documentTitle = sanitized,
                hasUnsavedChanges = true,
                lastExportNotice = null
            )
        }
    }

    fun updateDocumentContent(newContent: String) {
        _documentState.update {
            it.copy(
                content = newContent,
                hasUnsavedChanges = newContent.isNotEmpty(),
                lastExportNotice = null
            )
        }
    }

    fun selectDocumentFormat(format: DocumentFormat) {
        _documentState.update {
            it.copy(format = format, lastExportNotice = null)
        }
    }

    fun toggleDocumentPreviewMode() {
        _documentState.update {
            it.copy(isPreviewMode = !it.isPreviewMode)
        }
    }

    fun requestClearDocument() {
        val current = _documentState.value
        if (current.content.isNotEmpty() || current.hasUnsavedChanges) {
            _documentState.update { it.copy(showClearConfirmation = true) }
        } else {
            confirmClearDocument()
        }
    }

    fun dismissClearDocumentConfirmation() {
        _documentState.update { it.copy(showClearConfirmation = false) }
    }

    fun confirmClearDocument() {
        _documentState.update { prev ->
            SecureDocumentState(
                documentTitle = "ephemeral-note",
                content = "",
                format = prev.format,
                isPreviewMode = false,
                hasUnsavedChanges = false,
                showClearConfirmation = false,
                lastExportNotice = "Volatile document buffer cleared from application memory.",
                revokedTempUriCount = prev.revokedTempUriCount + 1
            )
        }
    }

    fun onDocumentExportCompleted(exportedFileName: String) {
        _documentState.update { prev ->
            prev.copy(
                hasUnsavedChanges = false,
                revokedTempUriCount = prev.revokedTempUriCount + 1,
                lastExportNotice = "Exported '$exportedFileName' and immediately revoked temporary export stream."
            )
        }
    }

    // --- Safe One-Click Downloads ---

    fun updateDownloadInputUrl(url: String) {
        _downloadInputUrl.value = url
    }

    fun startOneClickDownload(
        rawUrl: String = _downloadInputUrl.value,
        userConfirmedExecutable: Boolean = false
    ) {
        val taskId = "dl-${downloadIdSequence.getAndIncrement()}"
        val placeholder = DownloadTaskItem(
            id = taskId,
            sourceUrl = rawUrl.trim(),
            finalResolvedUrl = rawUrl.trim(),
            sanitizedFileName = UrlSecurityValidator.classifyAndSanitizeDownload(rawUrl, null).sanitizedFileName,
            mimeType = "inspecting…",
            totalBytes = -1L,
            downloadedBytes = 0L,
            state = DownloadLifecycleState.VALIDATING,
            isExecutableOrDangerous = false
        )
        _downloads.update { listOf(placeholder) + it }

        val maxHops = _securitySettings.value.maxRedirectHops
        viewModelScope.launch {
            safeDownloadManager.executeSafeDownload(
                taskId = taskId,
                initialUrl = rawUrl,
                maxRedirects = maxHops,
                userConfirmedExecutable = userConfirmedExecutable,
                onUpdate = { updatedItem ->
                    _downloads.update { list ->
                        list.map { existing -> if (existing.id == taskId) updatedItem else existing }
                    }
                }
            )
        }
    }

    fun confirmExecutableDownload(taskId: String) {
        val target = _downloads.value.find { it.id == taskId } ?: return
        // Remove the awaiting entry and re-run with explicit confirmation
        _downloads.update { list -> list.filterNot { it.id == taskId } }
        startOneClickDownload(
            rawUrl = target.sourceUrl,
            userConfirmedExecutable = true
        )
    }

    fun dismissExecutableDownload(taskId: String) {
        _downloads.update { list ->
            list.map { item ->
                if (item.id == taskId) {
                    item.copy(
                        state = DownloadLifecycleState.CANCELLED,
                        errorMessage = "Executable download cancelled by user at safety prompt."
                    )
                } else {
                    item
                }
            }
        }
    }

    fun cancelDownload(taskId: String) {
        safeDownloadManager.cancelDownload(taskId)
        _downloads.update { list ->
            list.map { item ->
                if (item.id == taskId && item.state == DownloadLifecycleState.DOWNLOADING) {
                    item.copy(
                        state = DownloadLifecycleState.CANCELLED,
                        savedMemoryBuffer = null,
                        errorMessage = "Download cancelled; volatile buffer zeroed."
                    )
                } else {
                    item
                }
            }
        }
    }

    fun removeDownloadTask(taskId: String) {
        safeDownloadManager.cancelDownload(taskId)
        _downloads.update { list -> list.filterNot { it.id == taskId } }
    }

    fun clearAllDownloadsFromMemory() {
        safeDownloadManager.cancelAllDownloads()
        _downloads.value = emptyList()
    }

    // --- Security & Privacy Settings & Live Audit ---

    fun updateSecuritySettings(transform: (SecurityHardeningSettings) -> SecurityHardeningSettings) {
        _securitySettings.update(transform)
    }

    fun runRuntimePrivacyAudit(context: Context) {
        _auditReport.value = PrivacyStorageAuditor.runLiveAudit(context)
    }
}
