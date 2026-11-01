package com.link.core.presentation.mvi

import app.cash.turbine.test
import com.link.core.presentation.mvi.BaseViewModelTest.Act
import com.link.core.presentation.mvi.BaseViewModelTest.Ev
import com.link.core.presentation.mvi.BaseViewModelTest.St
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    data class St(val n: Int = 0) : State
    sealed interface Ev : Event { data object Inc : Ev; data class Add(val k: Int) : Ev; data object Boom : Ev; data object Ping : Ev }
    data class Act(val id: Int) : OneTimeAction

    class Vm : BaseViewModel<St, Ev, Act>() {
        var handled = 0
        override fun createInitialState() = St()
        override fun onEvent(event: Ev) {
            handled++
            when (event) {
                Ev.Inc -> setState { copy(n = n + 1) }
                is Ev.Add -> setState { copy(n = n + event.k) }
                Ev.Boom -> error("boom")
                Ev.Ping -> { sendOneTimeAction(Act(1)); sendOneTimeAction(Act(2)); sendOneTimeAction(Act(3)) }
            }
        }
        fun bump() = setState { copy(n = n + 1) }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is exposed`() {
        assertEquals(St(0), Vm().currentState)
    }

    @Test
    fun `events are processed in order`() = runTest {
        val vm = Vm()
        vm.sendEvent(Ev.Add(1)); vm.sendEvent(Ev.Add(10)); vm.sendEvent(Ev.Inc)
        assertEquals(12, vm.currentState.n)
    }

    @Test
    fun `state flow emits each distinct state`() = runTest {
        val vm = Vm()
        vm.state.test {
            assertEquals(St(0), awaitItem())
            vm.sendEvent(Ev.Inc); assertEquals(St(1), awaitItem())
            vm.sendEvent(Ev.Inc); assertEquals(St(2), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reducing to an equal state emits nothing`() = runTest {
        val vm = Vm()
        vm.state.test {
            awaitItem()
            vm.sendEvent(Ev.Add(0))
            expectNoEvents()
        }
    }

    @Test
    fun `one time actions arrive once, in order, to a single collector`() = runTest {
        val vm = Vm()
        vm.oneTimeAction.test {
            vm.sendEvent(Ev.Ping)
            assertEquals(Act(1), awaitItem()); assertEquals(Act(2), awaitItem()); assertEquals(Act(3), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `one time action sent before any collector is not lost`() = runTest {
        val vm = Vm()
        vm.sendEvent(Ev.Ping)
        vm.oneTimeAction.test { assertEquals(Act(1), awaitItem()); cancelAndIgnoreRemainingEvents() }
    }

    /**
     * BUG HUNT: an exception inside onEvent propagates out of the collector coroutine
     * and kills event processing for the ViewModel for good (crash / dead VM).
     */
    @Test
    fun `exception in one event handler does not kill later events`() = runTest {
        val vm = Vm()
        val caught = mutableListOf<Throwable>()
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> caught += e }
        try {
            vm.sendEvent(Ev.Boom)
            vm.sendEvent(Ev.Inc)
            assertEquals("VM must still process events after a failing one", 1, vm.currentState.n)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(old)
            runCatching { runTest { } } // drain collected uncaught exceptions
        }
    }

    /**
     * BUG HUNT: setState is read-modify-write on a StateFlow without update{}, so
     * concurrent callers lose updates.
     */
    @Test
    fun `setState is atomic under concurrency`() = runTest {
        val vm = Vm()
        withContext(Dispatchers.Default) {
            (1..16).map { async { repeat(5_000) { vm.bump() } } }.awaitAll()
        }
        assertEquals(16 * 5_000, vm.currentState.n)
    }

    @Test
    fun `many events sent from many threads are all handled`() = runTest {
        val vm = Vm()
        withContext(Dispatchers.Default) {
            (1..8).map { async { repeat(500) { vm.sendEvent(Ev.Inc) } } }.awaitAll()
        }
        // events go through viewModelScope on the (unconfined) main dispatcher
        assertEquals("handled=${vm.handled}", 4_000, vm.handled)
    }
}
