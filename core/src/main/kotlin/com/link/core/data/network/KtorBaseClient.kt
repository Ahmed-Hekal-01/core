package com.link.core.data.network

import com.link.core.BuildConfig
import com.link.core.data.utils.Constants
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Base client shared by Hilt and Koin; per-API clients are cloned from it by [KtorClientFactory]. */
internal fun createBaseKtorClient(json: Json): HttpClient = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) { json(json, ContentType.Application.Json) }
    install(HttpTimeout) {
        connectTimeoutMillis = Constants.OkHTTPConstants.CONNECT_TIMEOUT * 1000
        requestTimeoutMillis = Constants.OkHTTPConstants.READ_TIMEOUT * 1000
        socketTimeoutMillis = Constants.OkHTTPConstants.READ_TIMEOUT * 1000
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                android.util.Log.d("Ktor", message)
            }
        }
        level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
    }
}
