package com.nhviewer.data.settings

import com.nhviewer.testutil.MainDispatcherRule
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDataStoreTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var store: SettingsDataStore
    private lateinit var dataFile: File

    @Before
    fun setUp() {
        val dir = createTempDirectory("nhviewer-settings-test").toFile()
        dataFile = File(dir, "nhviewer_settings.preferences_pb")
        dataFile.delete()
        store = SettingsDataStore(dataFile)
    }

    @Test
    fun `observeSettings returns defaults when empty`() = runTest {
        val settings = store.observeSettings().first()

        assertEquals("high", settings.imageQuality)
        assertEquals(3, settings.maxConcurrency)
        assertEquals("system", settings.themeMode)
        assertEquals("zh-CN", settings.language)
        assertFalse(settings.preferJapaneseTitle)
        assertTrue(settings.showChineseTags)
        assertEquals("all", settings.homeLanguageFilter)
        assertEquals("recent", settings.homeSortOption)
        assertEquals("", settings.apiKey)
        assertTrue(settings.splashAnimationEnabled)
        assertFalse(settings.hideBlacklisted)
        assertEquals("local", settings.favoritesSource)
        assertTrue(settings.readerTapPagingEnabled)
        assertTrue(settings.readerSwipePagingEnabled)
        assertTrue(settings.readerTapToToggleChromeEnabled)
        assertFalse(settings.readerReverseTapZones)
        assertTrue(settings.readerGestureEnabled)
        assertFalse(settings.readerLeftHandedMode)
        assertEquals("single", settings.readerPagingMode)
    }

    @Test
    fun `setters persist values and apply constraints`() = runTest {
        store.setImageQuality("medium")
        store.setMaxConcurrency(99) // should be coerced to 8
        store.setThemeMode("dark")
        store.setLanguage("en-US")
        store.setPreferJapaneseTitle(true)
        store.setShowChineseTags(false)
        store.setHomeLanguageFilter("japanese")
        store.setHomeSortOption("popular")
        store.setApiKey("  my-key  ")
        store.setSplashAnimationEnabled(false)
        store.setHideBlacklisted(true)
        store.setFavoritesSource("online")
        store.setReaderTapPagingEnabled(false)
        store.setReaderSwipePagingEnabled(false)
        store.setReaderTapToToggleChromeEnabled(false)
        store.setReaderReverseTapZones(true)
        store.setReaderGestureEnabled(false)
        store.setReaderLeftHandedMode(true)
        store.setReaderPagingMode("continuous")

        val settings = store.observeSettings().first()
        assertEquals("medium", settings.imageQuality)
        assertEquals(8, settings.maxConcurrency)
        assertEquals("dark", settings.themeMode)
        assertEquals("en-US", settings.language)
        assertTrue(settings.preferJapaneseTitle)
        assertFalse(settings.showChineseTags)
        assertEquals("japanese", settings.homeLanguageFilter)
        assertEquals("popular", settings.homeSortOption)
        assertEquals("my-key", settings.apiKey)
        assertFalse(settings.splashAnimationEnabled)
        assertTrue(settings.hideBlacklisted)
        assertEquals("online", settings.favoritesSource)
        assertFalse(settings.readerTapPagingEnabled)
        assertFalse(settings.readerSwipePagingEnabled)
        assertFalse(settings.readerTapToToggleChromeEnabled)
        assertTrue(settings.readerReverseTapZones)
        assertFalse(settings.readerGestureEnabled)
        assertTrue(settings.readerLeftHandedMode)
        assertEquals("continuous", settings.readerPagingMode)
    }
}
