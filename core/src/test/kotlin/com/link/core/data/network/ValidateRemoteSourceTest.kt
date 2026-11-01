package com.link.core.data.network

import com.link.core.domain.models.NetworkError
import com.link.core.domain.models.Resource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ValidateRemoteSourceTest {

    private val sut = ValidateRemoteSourceImpl()

    private fun retrofitError(code: Int, body: String = "") = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))
    )

    private fun ktorClient(status: HttpStatusCode) =
        HttpClient(MockEngine { respond("", status) }) { expectSuccess = true }

    @Test
    fun `success wraps value`() = runBlocking {
        assertEquals(Resource.Success(42), sut.validate { 42 })
    }

    @Test
    fun `success with null value`() = runBlocking {
        assertEquals(Resource.Success(null), sut.validate<String?> { null })
    }

    @Test
    fun `success with Unit`() = runBlocking {
        assertEquals(Resource.Success(Unit), sut.validate { })
    }

    // ---- Retrofit ----
    @Test
    fun `retrofit 401 403 404`() = runBlocking {
        assertEquals(Resource.Error(NetworkError.Unauthorized), sut.validate<Unit> { throw retrofitError(401) })
        assertEquals(Resource.Error(NetworkError.Forbidden), sut.validate<Unit> { throw retrofitError(403) })
        val nf = sut.validate<Unit> { throw retrofitError(404) } as Resource.Error
        assert(nf.error is NetworkError.NotFound)
    }

    @Test
    fun `retrofit every other 4xx and 5xx code maps to ServerError with code`() = runBlocking {
        val codes = (400..599) - setOf(401, 403, 404)
        for (c in codes) {
            val r = sut.validate<Unit> { throw retrofitError(c) } as Resource.Error
            val e = r.error as NetworkError.ServerError
            assertEquals(c, e.code)
        }
    }

    // ---- Ktor ----
    @Test
    fun `ktor 401 403 404`() = runBlocking {
        val c401 = ktorClient(HttpStatusCode.Unauthorized)
        val c403 = ktorClient(HttpStatusCode.Forbidden)
        val c404 = ktorClient(HttpStatusCode.NotFound)
        assertEquals(Resource.Error(NetworkError.Unauthorized), sut.validate { c401.get("https://x.test/") })
        assertEquals(Resource.Error(NetworkError.Forbidden), sut.validate { c403.get("https://x.test/") })
        assertEquals(
            Resource.Error(NetworkError.NotFound("Not Found")),
            sut.validate { c404.get("https://x.test/") },
        )
    }

    @Test
    fun `ktor 5xx maps to ServerError with code and description`() = runBlocking {
        val c = ktorClient(HttpStatusCode.ServiceUnavailable)
        assertEquals(
            Resource.Error(NetworkError.ServerError(503, "Service Unavailable")),
            sut.validate { c.get("https://x.test/") },
        )
    }

    @Test
    fun `ktor 429 and 418 are ServerError not Unknown`() = runBlocking {
        for (s in listOf(HttpStatusCode.TooManyRequests, HttpStatusCode(418, "I'm a teapot"))) {
            val c = ktorClient(s)
            val r = sut.validate { c.get("https://x.test/") } as Resource.Error
            assertEquals(s.value, (r.error as NetworkError.ServerError).code)
        }
    }

    @Test
    fun `ktor redirect without following is ServerError with 3xx code`() = runBlocking {
        val c = HttpClient(MockEngine { respond("", HttpStatusCode.MovedPermanently) }) {
            expectSuccess = true
            followRedirects = false
        }
        val r = sut.validate { c.get("https://x.test/") } as Resource.Error
        assertEquals(301, (r.error as NetworkError.ServerError).code)
    }

    // ---- other failures ----
    @Test
    fun `io failures map to Unknown`() = runBlocking {
        for (t in listOf(IOException("x"), UnknownHostException("h"), SocketTimeoutException("t"),
            IllegalStateException("s"), NullPointerException())) {
            assertEquals(Resource.Error(NetworkError.Unknown), sut.validate<Unit> { throw t })
        }
    }

    /**
     * BUG HUNT: catch(Exception) also swallows CancellationException, so a cancelled
     * coroutine turns into a Resource.Error(Unknown) instead of cancelling.
     */
    @Test
    fun `CancellationException must propagate`() {
        assertThrows(CancellationException::class.java) {
            runBlocking { sut.validate<Unit> { throw CancellationException("cancelled") } }
        }
    }

    /** Errors (OOM, StackOverflow, ...) should never be converted into a Resource. */
    @Test
    fun `Error subclasses are not swallowed`() {
        assertThrows(StackOverflowError::class.java) {
            runBlocking { sut.validate<Unit> { throw StackOverflowError() } }
        }
    }
}
