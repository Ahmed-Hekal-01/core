package com.link.core.data.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

class RetrofitFactoryTest {

    interface Api { @GET("ping") fun ping(): Call<ResponseBody> }

    private val baseMarker = Interceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("X-Base", "1").build())
    }

    private fun terminal(sink: MutableList<Request>) = Interceptor { chain ->
        sink += chain.request()
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body("{}".toResponseBody("application/json".toMediaType())).build()
    }

    private fun factory(base: OkHttpClient = OkHttpClient.Builder().addInterceptor(baseMarker).build()) =
        RetrofitFactory(base, Retrofit.Builder())

    @Test
    fun `base url and base interceptors are applied`() {
        val seen = mutableListOf<Request>()
        factory().createService(Api::class.java, "https://a.test/v1/", listOf(terminal(seen)))
            .ping().execute()
        assertEquals("https://a.test/v1/ping", seen.single().url.toString())
        assertEquals("1", seen.single().header("X-Base"))
    }

    @Test
    fun `interceptors of one service do not leak into another or into the base client`() {
        val base = OkHttpClient.Builder().addInterceptor(baseMarker).build()
        val f = factory(base)
        val seenA = mutableListOf<Request>()
        val seenB = mutableListOf<Request>()
        val addA = Interceptor { it.proceed(it.request().newBuilder().header("X-A", "a").build()) }

        f.createService(Api::class.java, "https://a.test/", listOf(addA, terminal(seenA))).ping().execute()
        f.createService(Api::class.java, "https://b.test/", listOf(terminal(seenB))).ping().execute()

        assertEquals("a", seenA.single().header("X-A"))
        assertNull(seenB.single().header("X-A"))
        assertEquals(1, base.interceptors.size)
    }

    @Test
    fun `invalid base url throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            factory().createService(Api::class.java, "not a url")
        }
    }

    @Test
    fun `base url without trailing slash throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            factory().createService(Api::class.java, "https://a.test/v1")
        }
    }

    @Test
    fun `non interface service class throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            factory().createService(String::class.java, "https://a.test/")
        }
    }

    /** Shared mutable Retrofit.Builder: second call must not inherit state from the first. */
    @Test
    fun `second service does not reuse first base url`() {
        val seen = mutableListOf<Request>()
        val f = factory()
        f.createService(Api::class.java, "https://first.test/", listOf(terminal(seen))).ping().execute()
        f.createService(Api::class.java, "https://second.test/", listOf(terminal(seen))).ping().execute()
        assertEquals(listOf("first.test", "second.test"), seen.map { it.url.host })
    }
}
