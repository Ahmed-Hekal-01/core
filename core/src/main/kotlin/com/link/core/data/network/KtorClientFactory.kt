package com.link.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import javax.inject.Inject

public class KtorClientFactory @Inject constructor(
    private val httpClient: HttpClient
) {
    /**
     * Creates a Ktor [HttpClient] dynamically.
     *
     * @param baseUrl The base URL for this specific API.
     * @param headers Optional static headers (e.g. API key) for this API only.
     * @param configure Optional extra config (e.g. Auth plugin, request interceptors) for this API only.
     */
    public fun createClient(
        baseUrl: String,
        headers: Map<String, String> = emptyMap(),
        configure: HttpClientConfig<*>.() -> Unit = {}
    ): HttpClient {
        // .config() clones the base client (shared engine, copied plugins).
        // This prevents config from leaking across different APIs!
        return httpClient.config {
            defaultRequest {
                url(baseUrl)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                headers.forEach { (k, v) -> this@defaultRequest.header(k, v) }
            }
            configure()
        }
    }
}
