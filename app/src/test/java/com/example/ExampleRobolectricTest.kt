package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.SearchProvider
import com.example.security.PrivacyStorageAuditor
import com.example.viewmodel.GhostSearchViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches GhostSearch and runtime storage audit passes all checks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("GhostSearch", context.getString(R.string.app_name))

        val report = PrivacyStorageAuditor.runLiveAudit(context)
        assertTrue("Expected all runtime privacy audit assertions to pass", report.allPassed)
        assertTrue("Expected zero application SQLite databases", report.sqliteDatabasesFound.isEmpty())
        assertTrue("Expected zero SharedPreferences files", report.sharedPrefsFound.isEmpty())
    }

    @Test
    fun `search queries are never written to application storage databases or shared preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = GhostSearchViewModel()

        val secretQuery = "confidential-whistleblower-topic-99481"
        viewModel.updateSearchOrUrlInput(secretQuery)
        viewModel.selectSearchProvider(SearchProvider.BRAVE_SEARCH)
        viewModel.submitSearchOrUrl()

        // Verify active URL is in volatile memory state
        assertTrue(viewModel.sessionState.value.activeWebUrl?.contains(secretQuery) == true)

        // Verify zero SQLite databases, SharedPreferences, or files contain the query on disk
        val dbNames = context.databaseList()?.toList() ?: emptyList()
        assertTrue("No SQLite database should be created", dbNames.isEmpty())

        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val prefFiles = sharedPrefsDir.listFiles()?.toList() ?: emptyList()
        assertTrue("No SharedPreferences file should be created", prefFiles.isEmpty())

        val appFiles = context.filesDir.listFiles()?.toList() ?: emptyList()
        for (file in appFiles) {
            if (file.isFile) {
                val content = file.readText()
                assertFalse("Disk file must never contain search query", content.contains(secretQuery))
            }
        }
    }

    @Test
    fun `clearSession immediately removes all accessible application-managed state`() {
        val viewModel = GhostSearchViewModel()

        viewModel.updateSearchOrUrlInput("https://example.com/private-research?utm_source=test")
        viewModel.submitSearchOrUrl(forceDirectUrl = true)
        viewModel.recordBlockedTracker()
        assertEquals("https://example.com/private-research", viewModel.sessionState.value.activeWebUrl)
        assertEquals(1, viewModel.sessionState.value.blockedTrackersInSession)

        val initialGen = viewModel.sessionState.value.sessionGeneration
        viewModel.clearSession(includeWorkspaceAndDownloads = true)

        val cleared = viewModel.sessionState.value
        assertEquals(initialGen + 1, cleared.sessionGeneration)
        assertEquals("", cleared.queryOrUrlInput)
        assertNull(cleared.activeWebUrl)
        assertNull(cleared.activePageTitle)
        assertEquals(0, cleared.blockedTrackersInSession)
        assertEquals(0, cleared.strippedParamsInSession)
        assertEquals(0, cleared.blockedUnsafeSchemesInSession)
    }

    @Test
    fun `secure document workspace never autosaves to disk and requires confirmation before clearing unsaved content`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = GhostSearchViewModel()

        val draftSecret = "Top secret draft payload that must remain in volatile RAM only."
        viewModel.updateDocumentTitle("incident-report")
        viewModel.updateDocumentContent(draftSecret)

        assertTrue(viewModel.documentState.value.hasUnsavedChanges)
        assertTrue(viewModel.documentState.value.byteSize > 0)

        // Verify nothing was autosaved to disk
        val report = PrivacyStorageAuditor.runLiveAudit(context)
        assertTrue(report.sqliteDatabasesFound.isEmpty())
        assertTrue(report.sharedPrefsFound.isEmpty())
        assertTrue(report.internalFilesFound.isEmpty())

        // Requesting clear on unsaved content must trigger confirmation modal first
        viewModel.requestClearDocument()
        assertTrue(viewModel.documentState.value.showClearConfirmation)
        assertEquals(draftSecret, viewModel.documentState.value.content)

        // Confirming clear wipes volatile content and increments revoked buffer counter
        viewModel.confirmClearDocument()
        assertEquals("", viewModel.documentState.value.content)
        assertFalse(viewModel.documentState.value.hasUnsavedChanges)
        assertFalse(viewModel.documentState.value.showClearConfirmation)
        assertTrue(viewModel.documentState.value.revokedTempUriCount >= 1)
    }
}
