package com.example.model

enum class PrivacyTier(val badgeLabel: String) {
    PRIVACY_FOCUSED("No Ad-Profile Policy"),
    INDEPENDENT_INDEX("Independent Index"),
    MAINSTREAM_AD_TECH("Mainstream Ad-Supported")
}

enum class SearchProvider(
    val id: String,
    val displayName: String,
    val host: String,
    val searchUrlTemplate: String,
    val privacyTier: PrivacyTier,
    val disclosureSummary: String,
    val dataReceivedByProvider: String,
    val loggingAndRetentionNotice: String
) {
    DUCKDUCKGO_HTML(
        id = "ddg_html",
        displayName = "DuckDuckGo Lite (HTML)",
        host = "html.duckduckgo.com",
        searchUrlTemplate = "https://html.duckduckgo.com/html/?q=%s",
        privacyTier = PrivacyTier.PRIVACY_FOCUSED,
        disclosureSummary = "Direct HTTPS query to html.duckduckgo.com. Zero client-side JS required.",
        dataReceivedByProvider = "Receives your raw search query, public IP address, TLS handshake metadata, and User-Agent directly.",
        loggingAndRetentionNotice = "DuckDuckGo states it does not store personal IP addresses or user-agent strings with search terms, though aggregate query logs and Microsoft syndication infrastructure apply."
    ),
    BRAVE_SEARCH(
        id = "brave",
        displayName = "Brave Search",
        host = "search.brave.com",
        searchUrlTemplate = "https://search.brave.com/search?q=%s&source=web",
        privacyTier = PrivacyTier.INDEPENDENT_INDEX,
        disclosureSummary = "Direct HTTPS query to search.brave.com using Brave's independent web index.",
        dataReceivedByProvider = "Receives your raw search query, public IP address, TLS handshake metadata, and browser headers.",
        loggingAndRetentionNotice = "Operates an independent search index without user-profile ad targeting; transient IP processing is still required for network delivery and abuse prevention."
    ),
    STARTPAGE(
        id = "startpage",
        displayName = "Startpage",
        host = "www.startpage.com",
        searchUrlTemplate = "https://www.startpage.com/sp/search?query=%s",
        privacyTier = PrivacyTier.PRIVACY_FOCUSED,
        disclosureSummary = "Direct HTTPS query to www.startpage.com (fetches syndicated results without passing your IP to upstream index).",
        dataReceivedByProvider = "Startpage servers receive your raw search query, public IP address, and TLS connection metadata.",
        loggingAndRetentionNotice = "Acts as an intermediary proxy to upstream search indexes; you must trust Startpage's no-PII-logging policy and infrastructure."
    ),
    MOJEEK(
        id = "mojeek",
        displayName = "Mojeek",
        host = "www.mojeek.com",
        searchUrlTemplate = "https://www.mojeek.com/search?q=%s",
        privacyTier = PrivacyTier.INDEPENDENT_INDEX,
        disclosureSummary = "Direct HTTPS query to www.mojeek.com (UK-based independent crawler index).",
        dataReceivedByProvider = "Receives your raw search query, public IP address, and standard HTTP/TLS headers.",
        loggingAndRetentionNotice = "Independent crawler index with a stated no-tracking policy; standard server delivery still exposes connection metadata to Mojeek and its transit providers."
    ),
    GOOGLE(
        id = "google",
        displayName = "Google Web Search",
        host = "www.google.com",
        searchUrlTemplate = "https://www.google.com/search?q=%s",
        privacyTier = PrivacyTier.MAINSTREAM_AD_TECH,
        disclosureSummary = "Direct HTTPS query to www.google.com. NOT anonymous — Google logs queries, IP, and telemetry.",
        dataReceivedByProvider = "Google directly receives your search query, IP address, TLS fingerprint, device headers, and any active session cookies within the current ephemeral view.",
        loggingAndRetentionNotice = "Subject to Google's Privacy Policy. GhostSearch clears local session state on exit, but cannot erase server-side logs recorded by Google."
    ),
    BING(
        id = "bing",
        displayName = "Microsoft Bing",
        host = "www.bing.com",
        searchUrlTemplate = "https://www.bing.com/search?q=%s",
        privacyTier = PrivacyTier.MAINSTREAM_AD_TECH,
        disclosureSummary = "Direct HTTPS query to www.bing.com. NOT anonymous — Microsoft logs queries and IP telemetry.",
        dataReceivedByProvider = "Microsoft directly receives your search query, IP address, TLS metadata, and HTTP request headers.",
        loggingAndRetentionNotice = "Subject to Microsoft's Privacy Statement. Clearing your GhostSearch session does not delete records already transmitted to Microsoft."
    )
}

enum class GhostTab(val route: String, val label: String) {
    SEARCH("search", "Search"),
    WORKSPACE("workspace", "Secure Docs"),
    DOWNLOADS("downloads", "Downloads"),
    PRIVACY_AUDIT("privacy_audit", "Privacy & Audit")
}

data class SecurityHardeningSettings(
    val enforceHttpsOnly: Boolean = true,
    val blockThirdPartyTrackers: Boolean = true,
    val blockPrivateNetworkSsrf: Boolean = true,
    val enableWebViewJavaScript: Boolean = false,
    val enableWebViewDomStorage: Boolean = false,
    val stripTrackingQueryParams: Boolean = true,
    val maxRedirectHops: Int = 5
)

data class EphemeralSessionState(
    val sessionGeneration: Int = 1,
    val sessionStartedEpochMs: Long = System.currentTimeMillis(),
    val queryOrUrlInput: String = "",
    val selectedProvider: SearchProvider = SearchProvider.DUCKDUCKGO_HTML,
    val activeWebUrl: String? = null,
    val activePageTitle: String? = null,
    val isLoading: Boolean = false,
    val loadProgressPercent: Int = 0,
    val errorMessage: String? = null,
    val blockedTrackersInSession: Int = 0,
    val strippedParamsInSession: Int = 0,
    val blockedUnsafeSchemesInSession: Int = 0,
    val lastClearedEpochMs: Long? = null,
    val statusNotice: String? = null
)

enum class DocumentFormat(val label: String, val extension: String, val mimeType: String) {
    PLAIN_TEXT("Plain Text (.txt)", "txt", "text/plain"),
    MARKDOWN("Markdown (.md)", "md", "text/markdown"),
    SANDBOXED_HTML("Sandboxed HTML (.html)", "html", "text/html")
}

data class SecureDocumentState(
    val documentTitle: String = "ephemeral-note",
    val content: String = "",
    val format: DocumentFormat = DocumentFormat.PLAIN_TEXT,
    val isPreviewMode: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val lastExportNotice: String? = null,
    val revokedTempUriCount: Int = 0
) {
    val byteSize: Int get() = content.toByteArray(Charsets.UTF_8).size
    val charCount: Int get() = content.length
    val wordCount: Int
        get() = content.trim().let { trimmed ->
            if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
        }
}

enum class DownloadLifecycleState {
    VALIDATING,
    AWAITING_EXECUTABLE_CONFIRMATION,
    DOWNLOADING,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class DownloadTaskItem(
    val id: String,
    val sourceUrl: String,
    val finalResolvedUrl: String,
    val sanitizedFileName: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val state: DownloadLifecycleState,
    val isExecutableOrDangerous: Boolean,
    val dangerWarningReason: String? = null,
    val redirectChain: List<String> = emptyList(),
    val sha256Hex: String? = null,
    val errorMessage: String? = null,
    val savedMemoryBuffer: ByteArray? = null
) {
    val progressFraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

data class PresetDownloadSample(
    val title: String,
    val description: String,
    val url: String,
    val badge: String,
    val isExecutableDemo: Boolean = false
)

data class AuditAssertionResult(
    val id: String,
    val category: String,
    val title: String,
    val passed: Boolean,
    val technicalDetail: String
)

data class RuntimePrivacyAuditReport(
    val timestampEpochMs: Long,
    val sqliteDatabasesFound: List<String>,
    val sharedPrefsFound: List<String>,
    val internalFilesFound: List<String>,
    val assertions: List<AuditAssertionResult>
) {
    val allPassed: Boolean get() = assertions.all { it.passed }
}
