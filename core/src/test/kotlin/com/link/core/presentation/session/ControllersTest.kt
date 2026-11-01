package com.link.core.presentation.session

import com.link.core.data.datastore.AppLanguage
import com.link.core.data.datastore.AppTheme
import com.link.core.presentation.theme.LocaleControllerImpl
import com.link.core.presentation.theme.ThemeControllerImpl
import com.link.core.testutil.FakeAppPreferences
import com.link.core.testutil.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ControllersTest {

    private val prefs = FakeAppPreferences()
    private val dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher())

    @Test
    fun `session reflects initial prefs eagerly`() {
        val s = SessionControllerImpl(prefs, dispatchers)
        assertTrue(s.isFirstLaunch.value)
        assertFalse(s.isGuest.value)
    }

    @Test
    fun `session picks up prefs that were already set before construction`() {
        prefs.firstLaunchFlow.value = false
        prefs.guestFlow.value = true
        val s = SessionControllerImpl(prefs, dispatchers)
        assertFalse(s.isFirstLaunch.value)
        assertTrue(s.isGuest.value)
    }

    @Test
    fun `session setters propagate to state and suspend getters`() = runTest {
        val s = SessionControllerImpl(prefs, dispatchers)
        s.setFirstLaunchDone(); s.setGuest(true)
        assertFalse(s.isFirstLaunch.value); assertTrue(s.isGuest.value)
        assertFalse(s.getIsFirstLaunch()); assertTrue(s.getIsGuest())
        s.setGuest(false)
        assertFalse(s.getIsGuest()); assertFalse(s.isGuest.value)
    }

    @Test
    fun `setFirstLaunchDone is idempotent`() = runTest {
        val s = SessionControllerImpl(prefs, dispatchers)
        repeat(3) { s.setFirstLaunchDone() }
        assertFalse(s.getIsFirstLaunch())
    }

    @Test
    fun `theme controller starts light and follows changes`() = runTest {
        val t = ThemeControllerImpl(prefs, dispatchers)
        assertEquals(AppTheme.LIGHT, t.currentTheme.value)
        t.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, t.currentTheme.value)
        t.setTheme(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, t.currentTheme.value)
    }

    @Test
    fun `theme controller adopts persisted dark theme`() {
        prefs.themeFlow.value = AppTheme.DARK
        assertEquals(AppTheme.DARK, ThemeControllerImpl(prefs, dispatchers).currentTheme.value)
    }

    @Test
    fun `locale controller starts en and follows changes`() = runTest {
        val l = LocaleControllerImpl(prefs, dispatchers)
        assertEquals(AppLanguage.EN, l.currentLanguage.value)
        l.setLanguage(AppLanguage.AR)
        assertEquals(AppLanguage.AR, l.currentLanguage.value)
    }
}
