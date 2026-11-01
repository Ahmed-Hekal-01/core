package com.link.core.data.network

import app.cash.turbine.test
import com.link.core.domain.SessionExpiredSignal
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KtorClientFactoryTest {

    private val signal = SessionExpiredSignal()

    private fun factory(status: HttpStatusCode) = KtorClientFactory(
        HttpClient(MockEngine { respond("", status) }) { expectSuccess = true },
        signal,
    )

    @Test
    fun `401 with notifyOnUnauthorized emits once and still throws`() = runBlocking {
        val client = factory(HttpStatusCode.Unauthorized)
            .createClient("https://example.com/", notifyOnUnauthorized = true)
        signal.events.test {
            val ex = runCatching { client.get("x") }.exceptionOrNull()
            assert(ex is ClientRequestException)
            assertEquals(HttpStatusCode.Unauthorized, (ex as ClientRequestException).response.status)
            assertEquals(Unit, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `401 with notifyOnUnauthorized still maps to Unauthorized via validator`() = runBlocking {
        val client = factory(HttpStatusCode.Unauthorized)
            .createClient("https://example.com/", notifyOnUnauthorized = true)
        val res = ValidateRemoteSourceImpl().validate { client.get("x") }
        assertEquals(
            com.link.core.domain.models.Resource.Error(com.link.core.domain.models.NetworkError.Unauthorized),
            res,
        )
    }

    @Test
    fun `404 with notifyOnUnauthorized does not emit`() = runBlocking {
        val client = factory(HttpStatusCode.NotFound)
            .createClient("https://example.com/", notifyOnUnauthorized = true)
        signal.events.test {
            assertThrows(ClientRequestException::class.java) { runBlocking { client.get("x") } }
            expectNoEvents()
        }
    }

    @Test
    fun `401 with default flag does not emit`() = runBlocking {
        val client = factory(HttpStatusCode.Unauthorized).createClient("https://example.com/")
        signal.events.test {
            assertThrows(ClientRequestException::class.java) { runBlocking { client.get("x") } }
            expectNoEvents()
        }
    }

    @Test
    fun `notifyExpired never blocks with no collector and delivers to first()`() = runBlocking {
        repeat(5) { signal.notifyExpired() } // no subscriber, must not suspend
        assertNull(withTimeoutOrNull(100) { signal.events.first() }) // nothing replayed
        val got = async { withTimeout(1000) { signal.events.first() } }
        kotlinx.coroutines.yield()
        signal.notifyExpired()
        assertEquals(Unit, got.await())
    }
}
