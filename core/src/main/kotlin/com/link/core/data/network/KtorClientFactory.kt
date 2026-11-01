package com.link.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import com.link.core.domain.SessionExpiredSignal
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import javax.inject.Inject

public class KtorClientFactory @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionExpiredSignal: SessionExpiredSignal,
) {
    /**
     * Creates a Ktor [HttpClient] dynamically.
     *
     * @param baseUrl The base URL for this specific API.
     * @param headers Optional static headers (e.g. API key) for this API only.
     * @param notifyOnUnauthorized If true, HTTP 401 responses from this client also call
     * [SessionExpiredSignal.notifyExpired]. The exception still propagates. Default false;
     * leave false for login clients (401 there means wrong password, not an expired session).
     * @param configure Optional extra config (e.g. Auth plugin, request interceptors) for this API only.
     */
    public fun createClient(
        baseUrl: String,
        headers: Map<String, String> = emptyMap(),
        notifyOnUnauthorized: Boolean = false,
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
            if (notifyOnUnauthorized) {
                install(HttpCallValidator) {
                    handleResponseExceptionWithRequest { cause, _ ->
                        if (cause is ClientRequestException &&
                            cause.response.status == HttpStatusCode.Unauthorized
                        ) {
                            sessionExpiredSignal.notifyExpired()
                        }
                        // Not rethrown here: the original exception keeps propagating.
                    }
                }
            }
            configure()
        }
    }
}
