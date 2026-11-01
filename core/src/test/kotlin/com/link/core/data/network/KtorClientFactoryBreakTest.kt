package com.link.core.data.network

import com.link.core.domain.SessionExpiredSignal
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorClientFactoryBreakTest {

    private val signal = SessionExpiredSignal()
    private val requests = mutableListOf<HttpRequestData>()

    private fun factory(status: HttpStatusCode = HttpStatusCode.OK) = KtorClientFactory(
        HttpClient(MockEngine { requests += it; respond("", status) }) { expectSuccess = true },
        signal,
    )

    @Test
    fun `baseUrl prefix and relative path are combined`() = runBlocking {
        factory().createClient("https://example.com/api/v1/").get("users")
        assertEquals("https://example.com/api/v1/users", requests.single().url.toString())
    }

    @Test
    fun `accept json and custom headers are sent`() = runBlocking {
        factory().createClient("https://example.com/", mapOf("X-Key" to "k1")).get("x")
        val r = requests.single()
        assertEquals("k1", r.headers["X-Key"])
        assertEquals("application/json", r.headers[HttpHeaders.Accept])
    }

    @Test
    fun `headers of one client do not leak into another`() = runBlocking {
        val f = factory()
        val a = f.createClient("https://a.test/", mapOf("X-A" to "a"))
        val b = f.createClient("https://b.test/")
        a.get("x"); b.get("x")
        assertEquals("a", requests[0].headers["X-A"])
        assertNull(requests[1].headers["X-A"])
        assertEquals("b.test", requests[1].url.host)
    }

    @Test
    fun `notify flag of one client does not leak into another`() = runBlocking {
        val f = factory(HttpStatusCode.Unauthorized)
        f.createClient("https://a.test/", notifyOnUnauthorized = true)
        val plain = f.createClient("https://b.test/")
        runCatching { plain.get("x") }
        assertNull(withTimeoutOrNull(200) { signal.events.first() })
    }

    @Test
    fun `configure block runs and can add headers per request`() = runBlocking {
        val c = factory().createClient("https://example.com/") {
            // extra plugin-level config must be applied
            expectSuccess = false
        }
        // expectSuccess=false override => 4xx would not throw; here 200 just works
        c.get("x") { header("X-Req", "1") }
        assertEquals("1", requests.single().headers["X-Req"])
    }

    @Test
    fun `configure can disable expectSuccess and then 401 does not throw nor notify`() = runBlocking {
        val c = factory(HttpStatusCode.Unauthorized)
            .createClient("https://example.com/", notifyOnUnauthorized = true) { expectSuccess = false }
        val resp = c.get("x")
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
        assertNull(withTimeoutOrNull(200) { signal.events.first() })
    }

    @Test
    fun `403 500 and 429 do not signal expiry`() = runBlocking {
        for (s in listOf(HttpStatusCode.Forbidden, HttpStatusCode.InternalServerError, HttpStatusCode.TooManyRequests)) {
            val c = factory(s).createClient("https://example.com/", notifyOnUnauthorized = true)
            runCatching { c.get("x") }
        }
        assertNull(withTimeoutOrNull(200) { signal.events.first() })
    }

    @Test
    fun `network failure with notify enabled propagates and does not signal`() = runBlocking {
        val f = KtorClientFactory(
            HttpClient(MockEngine { throw java.io.IOException("offline") }) { expectSuccess = true },
            signal,
        )
        val c = f.createClient("https://example.com/", notifyOnUnauthorized = true)
        val ex = runCatching { c.get("x") }.exceptionOrNull()
        assertTrue(ex is java.io.IOException)
        assertNull(withTimeoutOrNull(200) { signal.events.first() })
    }

    @Test
    fun `creating many clients does not mutate the base client`() = runBlocking {
        val f = factory()
        repeat(50) { f.createClient("https://c$it.test/", mapOf("X-$it" to "v")) }
        val fresh = f.createClient("https://fresh.test/")
        fresh.get("x")
        val r = requests.single()
        assertTrue(r.headers.names().none { it.startsWith("X-") })
    }
}
