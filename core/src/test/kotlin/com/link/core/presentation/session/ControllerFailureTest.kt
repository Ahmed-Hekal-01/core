package com.link.core.presentation.session

import com.link.core.data.datastore.AppPreferences
import com.link.core.data.datastore.AppTheme
import com.link.core.presentation.theme.ThemeControllerImpl
import com.link.core.testutil.FakeAppPreferences
import com.link.core.testutil.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Isolated class: the failure under test leaks uncaught exceptions into sibling tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class ControllerFailureTest {
    private val prefs = FakeAppPreferences()
    private val dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher())

    /**
     * BUG HUNT: the upstream prefs flow throws (e.g. corrupted/renamed enum in DataStore).
     * stateIn(Eagerly) in a scope without a handler => uncaught exception on the scope.
     * The controller should keep serving its initial value rather than crash the process.
     */
    @Test
    fun `failing prefs flow does not crash the controller`() {
        val failing = object : com.link.core.data.datastore.AppPreferences by prefs {
            override val theme = flow<AppTheme> { throw IllegalArgumentException("No enum constant") }
        }
        val caught = mutableListOf<Throwable>()
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> caught += e }
        try {
            val t = ThemeControllerImpl(failing, dispatchers)
            assertEquals(AppTheme.LIGHT, t.currentTheme.value)
            assertTrue("uncaught: $caught", caught.isEmpty())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(old)
            // drain exceptions kotlinx-coroutines-test collected, so sibling tests are not polluted
            runCatching { kotlinx.coroutines.test.runTest { } }
        }
    }
}
