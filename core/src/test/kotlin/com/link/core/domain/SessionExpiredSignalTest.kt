package com.link.core.domain

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExpiredSignalTest {

    @Test
    fun `notify from many threads never throws or blocks`() = runBlocking {
        val signal = SessionExpiredSignal()
        withTimeout(5_000) {
            (1..64).map { async(Dispatchers.Default) { repeat(10_000) { signal.notifyExpired() } } }.awaitAll()
        }
        Unit
    }

    @Test
    fun `slow collector sees a bounded number of events from a burst`() = runBlocking {
        val signal = SessionExpiredSignal()
        val received = java.util.concurrent.atomic.AtomicInteger()
        val job = launch(Dispatchers.Default) {
            signal.events.collect { received.incrementAndGet(); delay(300) }
        }
        delay(100) // let the collector subscribe
        repeat(1_000) { signal.notifyExpired() }
        delay(1_000)
        job.cancelAndJoin()
        assertTrue("got ${received.get()}", received.get() in 1..3)
    }

    @Test
    fun `late subscriber gets no replay`() = runBlocking {
        val signal = SessionExpiredSignal()
        signal.notifyExpired()
        signal.events.test { expectNoEvents() }
    }

    @Test
    fun `two collectors both receive the event`() = runBlocking {
        val signal = SessionExpiredSignal()
        signal.events.test {
            signal.events.test {
                signal.notifyExpired()
                assertEquals(Unit, awaitItem())
            }
            assertEquals(Unit, awaitItem())
        }
    }
}
