package com.example.security

import com.example.model.SearchProvider
import java.net.URI
import java.net.URLEncoder

sealed class UrlValidationResult {
    data class ValidDirectUrl(
        val normalizedUrl: String,
        val host: String,
        val strippedTrackingParamsCount: Int
    ) : UrlValidationResult()

    data class ValidSearchQuery(
        val targetSearchUrl: String,
        val provider: SearchProvider,
        val strippedCharacters: Boolean = false
    ) : UrlValidationResult()

    data class Rejected(
        val reasonTitle: String,
        val technicalDetail: String
    ) : UrlValidationResult()
}

data class FileSafetyClassification(
    val sanitizedFileName: String,
    val isExecutableOrDangerous: Boolean,
    val warningReason: String?
)

object UrlSecurityValidator {

    private val DANGEROUS_SCHEMES = setOf(
        "javascript",
        "vbscript",
        "data",
        "file",
        "content",
        "intent",
        "jar",
        "about",
        "blob",
        "filesystem",
        "chrome",
        "edge",
        "view-source"
    )

    private val TRACKING_QUERY_PARAMS = setOf(
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "utm_id",
        "gclid",
        "gclsrc",
        "dclid",
        "fbclid",
        "msclkid",
        "twclid",
        "mc_cid",
        "mc_eid",
        "yclid",
        "igshid",
        "_hsenc",
        "_hsmi",
        "vero_id",
        "rb_clickid",
        "s_kwcid"
    )

    private val KNOWN_TRACKER_HOSTS = setOf(
        "google-analytics.com",
        "www.google-analytics.com",
        "ssl.google-analytics.com",
        "googletagmanager.com",
        "www.googletagmanager.com",
        "doubleclick.net",
        "stats.g.doubleclick.net",
        "connect.facebook.net",
        "pixel.facebook.com",
        "analytics.tiktok.com",
        "bat.bing.com",
        "clarity.ms",
        "www.clarity.ms",
        "hotjar.com",
        "static.hotjar.com",
        "mixpanel.com",
        "api.mixpanel.com",
        "segment.io",
        "api.segment.io",
        "scorecardresearch.com",
        "sb.scorecardresearch.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com"
    )

    private val EXECUTABLE_EXTENSIONS = setOf(
        "exe", "apk", "aab", "msi", "bat", "cmd", "com", "scr", "ps1", "psm1",
        "vbs", "vbe", "js", "jse", "wsf", "wsh", "sh", "bash", "zsh", "run",
        "bin", "dmg", "pkg", "deb", "rpm", "appimage", "jar", "jnlp", "hta",
        "cpl", "reg", "dll", "so", "dylib", "elf", "py", "pl", "rb"
    )

    private val EXECUTABLE_MIME_PREFIXES = setOf(
        "application/x-msdownload",
        "application/x-msdos-program",
        "application/vnd.android.package-archive",
        "application/x-executable",
        "application/x-dosexec",
        "application/x-sh",
        "application/x-shellscript",
        "application/java-archive",
        "application/x-apple-diskimage",
        "application/x-debian-package",
        "application/x-rpm",
        "application/x-msi",
        "text/javascript",
        "application/javascript"
    )

    const val STRICT_DOCUMENT_PREVIEW_CSP =
        "default-src 'none'; style-src 'unsafe-inline'; img-src data:; font-src 'none'; " +
            "script-src 'none'; connect-src 'none'; form-action 'none'; base-uri 'none'; frame-ancestors 'none';"

    fun validateSearchOrNavigation(
        rawInput: String,
        provider: SearchProvider,
        enforceHttpsOnly: Boolean = true,
        blockPrivateNetworkSsrf: Boolean = true,
        stripTrackingParams: Boolean = true,
        forceDirectUrl: Boolean = false
    ): UrlValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return UrlValidationResult.Rejected(
                reasonTitle = "Empty Input",
                technicalDetail = "Enter a search query or an HTTPS URL to proceed."
            )
        }

        if (trimmed.length > 2048) {
            return UrlValidationResult.Rejected(
                reasonTitle = "Input Length Exceeded",
                technicalDetail = "Input exceeds the 2,048-character security ceiling."
            )
        }

        // Check for dangerous scheme prefix regardless of whitespace/control obfuscation
        val compactLower = trimmed.replace(Regex("[\\u0000-\\u0020]+"), "").lowercase()
        val colonIdx = compactLower.indexOf(':')
        if (colonIdx > 0) {
            val candidateScheme = compactLower.substring(0, colonIdx)
            if (candidateScheme in DANGEROUS_SCHEMES) {
                return UrlValidationResult.Rejected(
                    reasonTitle = "Blocked Unsafe Scheme ($candidateScheme:)",
                    technicalDetail = "GhostSearch rejects '$candidateScheme:' URIs to prevent XSS, local file disclosure, and intent hijacking."
                )
            }
        }

        val looksLikeUrl = forceDirectUrl || isLikelyDirectUrl(trimmed)
        if (!looksLikeUrl) {
            val encodedQuery = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
            val searchUrl = String.format(provider.searchUrlTemplate, encodedQuery)
            return UrlValidationResult.ValidSearchQuery(
                targetSearchUrl = searchUrl,
                provider = provider
            )
        }

        return validateDirectUrl(
            rawUrl = trimmed,
            enforceHttpsOnly = enforceHttpsOnly,
            blockPrivateNetworkSsrf = blockPrivateNetworkSsrf,
            stripTrackingParams = stripTrackingParams
        )
    }

    fun validateDirectUrl(
        rawUrl: String,
        enforceHttpsOnly: Boolean = true,
        blockPrivateNetworkSsrf: Boolean = true,
        stripTrackingParams: Boolean = true
    ): UrlValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return UrlValidationResult.Rejected(
                reasonTitle = "Empty URL",
                technicalDetail = "Provide a complete HTTPS URL."
            )
        }

        val compactLower = trimmed.replace(Regex("[\\u0000-\\u0020]+"), "").lowercase()
        val colonIdx = compactLower.indexOf(':')
        if (colonIdx > 0) {
            val scheme = compactLower.substring(0, colonIdx)
            if (scheme in DANGEROUS_SCHEMES) {
                return UrlValidationResult.Rejected(
                    reasonTitle = "Blocked Unsafe Scheme ($scheme:)",
                    technicalDetail = "Scheme '$scheme:' is forbidden by GhostSearch security policy."
                )
            }
        }

        val normalizedCandidate = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> {
                if (enforceHttpsOnly) {
                    return UrlValidationResult.Rejected(
                        reasonTitle = "Cleartext HTTP Blocked",
                        technicalDetail = "Unencrypted http:// connections expose traffic to network interception. Use https:// or disable strict HTTPS-only mode."
                    )
                }
                trimmed
            }
            trimmed.contains("://") -> {
                val scheme = trimmed.substringBefore("://").lowercase()
                return UrlValidationResult.Rejected(
                    reasonTitle = "Unsupported Protocol ($scheme://)",
                    technicalDetail = "Only HTTPS URLs are permitted for navigation and downloads."
                )
            }
            else -> "https://$trimmed"
        }

        val parsedUri = try {
            URI(normalizedCandidate)
        } catch (e: Exception) {
            return UrlValidationResult.Rejected(
                reasonTitle = "Malformed URL Syntax",
                technicalDetail = "URL could not be parsed safely as an RFC 3986 URI."
            )
        }

        val scheme = parsedUri.scheme?.lowercase() ?: ""
        if (scheme != "https" && (enforceHttpsOnly || scheme != "http")) {
            return UrlValidationResult.Rejected(
                reasonTitle = "HTTPS Required",
                technicalDetail = "Only https:// URLs are allowed (received '$scheme')."
            )
        }

        if (!parsedUri.userInfo.isNullOrEmpty()) {
            return UrlValidationResult.Rejected(
                reasonTitle = "Embedded Credentials Rejected",
                technicalDetail = "URLs containing user:password@host syntax are blocked to prevent credential phishing and host spoofing."
            )
        }

        val host = parsedUri.host?.lowercase()?.trimEnd('.')
        if (host.isNullOrBlank()) {
            return UrlValidationResult.Rejected(
                reasonTitle = "Missing Hostname",
                technicalDetail = "URL must contain a valid public domain name."
            )
        }

        if (blockPrivateNetworkSsrf && isPrivateOrLoopbackHost(host)) {
            return UrlValidationResult.Rejected(
                reasonTitle = "SSRF / Private Network Blocked",
                technicalDetail = "Access to loopback, link-local, cloud metadata, or RFC 1918 internal network host '$host' is blocked."
            )
        }

        if (!host.contains('.') && host != "localhost") {
            return UrlValidationResult.Rejected(
                reasonTitle = "Unqualified Hostname Blocked",
                technicalDetail = "Hostname '$host' is not a fully qualified public domain."
            )
        }

        val (cleanedUrl, strippedCount) = if (stripTrackingParams) {
            stripKnownTrackingParameters(parsedUri)
        } else {
            parsedUri.toASCIIString() to 0
        }

        return UrlValidationResult.ValidDirectUrl(
            normalizedUrl = cleanedUrl,
            host = host,
            strippedTrackingParamsCount = strippedCount
        )
    }

    fun isLikelyDirectUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.contains(' ') || trimmed.contains('\n') || trimmed.contains('\t')) {
            return false
        }
        if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return true
        }
        // Match domain-like patterns such as example.com or sub.example.org/path
        val hostPart = trimmed.substringBefore('/').substringBefore('?').substringBefore('#')
        val dotIndex = hostPart.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex == hostPart.length - 1) return false
        val tld = hostPart.substring(dotIndex + 1)
        return tld.length in 2..12 && tld.all { it.isLetter() }
    }

    fun isPrivateOrLoopbackHost(rawHost: String): Boolean {
        val host = rawHost.trim().lowercase().removeSurrounding("[", "]").trimEnd('.')
        if (host.isEmpty()) return true

        if (host == "localhost" ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host.endsWith(".home.arpa") ||
            host.endsWith(".lan") ||
            host == "0.0.0.0" ||
            host == "::1" ||
            host == "0:0:0:0:0:0:0:1" ||
            host == "::"
        ) {
            return true
        }

        // IPv6 ULA (fc00::/7 -> fc.. or fd..) and Link-Local (fe80::/10)
        if (host.contains(':')) {
            if (host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80")) {
                return true
            }
        }

        // Check IPv4 dotted quad or numeric IP representations
        val ipv4Parts = host.split('.')
        if (ipv4Parts.size == 4 && ipv4Parts.all { it.toIntOrNull() in 0..255 }) {
            val o1 = ipv4Parts[0].toInt()
            val o2 = ipv4Parts[1].toInt()
            if (o1 == 10) return true // 10.0.0.0/8
            if (o1 == 127) return true // 127.0.0.0/8 loopback
            if (o1 == 0) return true // 0.0.0.0/8
            if (o1 == 169 && o2 == 254) return true // 169.254.0.0/16 AWS/GCP metadata & link-local
            if (o1 == 172 && o2 in 16..31) return true // 172.16.0.0/12
            if (o1 == 192 && o2 == 168) return true // 192.168.0.0/16
            if (o1 == 100 && o2 in 64..127) return true // 100.64.0.0/10 CGNAT
        }

        return false
    }

    fun isKnownTrackerHost(rawHost: String?): Boolean {
        if (rawHost.isNullOrBlank()) return false
        val host = rawHost.lowercase().trimEnd('.')
        return KNOWN_TRACKER_HOSTS.any { tracker ->
            host == tracker || host.endsWith(".$tracker")
        }
    }

    fun stripKnownTrackingParameters(uri: URI): Pair<String, Int> {
        val rawQuery = uri.rawQuery ?: return uri.toASCIIString() to 0
        val pairs = rawQuery.split('&')
        var removedCount = 0
        val keptPairs = mutableListOf<String>()
        for (pair in pairs) {
            val key = pair.substringBefore('=').lowercase()
            if (key in TRACKING_QUERY_PARAMS || key.startsWith("utm_")) {
                removedCount++
            } else if (pair.isNotBlank()) {
                keptPairs.add(pair)
            }
        }
        if (removedCount == 0) return uri.toASCIIString() to 0

        val rebuilt = URI(
            uri.scheme,
            uri.RawAuthorityOrAuthority(),
            uri.path,
            if (keptPairs.isEmpty()) null else keptPairs.joinToString("&"),
            uri.fragment
        )
        return rebuilt.toASCIIString() to removedCount
    }

    private fun URI.RawAuthorityOrAuthority(): String? = this.authority

    fun classifyAndSanitizeDownload(
        rawFileNameOrUrl: String,
        contentTypeHeader: String?
    ): FileSafetyClassification {
        // Extract candidate name from URL path if a full URL was passed
        val candidateRaw = rawFileNameOrUrl
            .substringBefore('?')
            .substringBefore('#')
            .replace('\\', '/')
            .substringAfterLast('/')
            .trim()

        // Strip bidirectional override characters (RLO \u202E etc.), control chars, and path traversal tokens
        val withoutControlAndBidi = candidateRaw
            .replace(Regex("[\\u0000-\\u001F\\u007F\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069]"), "")
            .replace("..", "_")
            .replace("/", "_")
            .replace("\\", "_")
            .replace(":", "_")

        // Only allow safe characters [A-Za-z0-9._-]
        val safeChars = withoutControlAndBidi
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trimStart('.', '_')
            .trimEnd('.', '_')

        val baseSanitized = if (safeChars.isBlank()) "ghostsearch_download.txt" else safeChars.take(120)

        // Inspect all extensions (including deceptive double extensions like report.pdf.exe)
        val segments = baseSanitized.split('.').drop(1).map { it.lowercase() }
        val primaryExtension = segments.lastOrNull() ?: ""
        val hasDeceptiveDoubleExtension = segments.size >= 2 &&
            segments.dropLast(1).any { it in setOf("pdf", "txt", "doc", "docx", "jpg", "png", "csv") } &&
            primaryExtension in EXECUTABLE_EXTENSIONS

        val normalizedMime = contentTypeHeader?.substringBefore(';')?.trim()?.lowercase() ?: ""
        val isExeExtension = segments.any { it in EXECUTABLE_EXTENSIONS }
        val isExeMime = EXECUTABLE_MIME_PREFIXES.any { normalizedMime.startsWith(it) }

        val isDangerous = isExeExtension || isExeMime || hasDeceptiveDoubleExtension
        val warningReason = when {
            hasDeceptiveDoubleExtension ->
                "Deceptive double extension detected (.$primaryExtension disguised as a document). Explicit confirmation required."
            isExeExtension ->
                "Executable or script file extension (.$primaryExtension) can execute code on your device."
            isExeMime ->
                "Server Content-Type ($normalizedMime) indicates an executable binary or script."
            else -> null
        }

        return FileSafetyClassification(
            sanitizedFileName = baseSanitized,
            isExecutableOrDangerous = isDangerous,
            warningReason = warningReason
        )
    }

    fun escapeHtml(rawText: String): String {
        val sb = StringBuilder(rawText.length + 32)
        for (ch in rawText) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#x27;")
                '/' -> sb.append("&#x2F;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun buildSandboxedDocumentPreviewHtml(title: String, rawBody: String, isDark: Boolean): String {
        val safeTitle = escapeHtml(title)
        val safeBody = escapeHtml(rawBody)
        val bgHex = if (isDark) "#0B0F19" else "#F8FAFC"
        val fgHex = if (isDark) "#F8FAFC" else "#0F172A"
        val borderHex = if (isDark) "#1E293B" else "#CBD5E1"
        val accentHex = if (isDark) "#00E5FF" else "#0284C7"

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta http-equiv="Content-Security-Policy" content="$STRICT_DOCUMENT_PREVIEW_CSP">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>$safeTitle</title>
              <style>
                body {
                  background-color: $bgHex;
                  color: $fgHex;
                  font-family: monospace;
                  padding: 16px;
                  margin: 0;
                  line-height: 1.55;
                }
                .csp-banner {
                  border: 1px solid $borderHex;
                  border-left: 3px solid $accentHex;
                  padding: 8px 12px;
                  margin-bottom: 14px;
                  font-size: 12px;
                }
                pre {
                  white-space: pre-wrap;
                  word-wrap: break-word;
                  margin: 0;
                  font-size: 14px;
                }
              </style>
            </head>
            <body>
              <div class="csp-banner">
                <strong>CSP Sandbox Active:</strong> script-src 'none'; connect-src 'none'; HTML entities escaped.
              </div>
              <pre>$safeBody</pre>
            </body>
            </html>
        """.trimIndent()
    }
}
