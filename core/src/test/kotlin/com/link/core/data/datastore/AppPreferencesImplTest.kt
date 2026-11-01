package com.link.core.data.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPreferencesImplTest {

    // NOTE: the file-level `preferencesDataStore` delegate is process-wide, so state survives
    // between tests. Everything lives in ONE test, ordered: defaults first, then mutations.
    @Test
    fun `defaults then roundtrips then independence`() = runBlocking {
        val p = AppPreferencesImpl(ApplicationProvider.getApplicationContext<Context>())

        assertEquals(AppTheme.LIGHT, p.theme.first())
        assertEquals(AppLanguage.EN, p.language.first())
        assertTrue(p.isFirstLaunch.first())
        assertFalse(p.isGuest.first())

        p.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, p.theme.first())
        assertEquals("language untouched", AppLanguage.EN, p.language.first())

        p.setLanguage(AppLanguage.AR)
        assertEquals(AppLanguage.AR, p.language.first())
        assertEquals("theme untouched", AppTheme.DARK, p.theme.first())

        p.setFirstLaunchDone(); p.setFirstLaunchDone()
        assertFalse(p.isFirstLaunch.first())

        p.setGuest(true); assertTrue(p.isGuest.first())
        p.setGuest(false); assertFalse(p.isGuest.first())

        // a second instance sees the same persisted data
        val p2 = AppPreferencesImpl(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(AppTheme.DARK, p2.theme.first())
        assertEquals(AppLanguage.AR, p2.language.first())

        // rapid writes: last write wins
        repeat(50) { p.setTheme(if (it % 2 == 0) AppTheme.LIGHT else AppTheme.DARK) }
        assertEquals(AppTheme.DARK, p.theme.first())
    }
}
