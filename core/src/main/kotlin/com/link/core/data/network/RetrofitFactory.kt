package com.link.core.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject

public class RetrofitFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val retrofitBuilder: Retrofit.Builder
) {
    /**
     * Creates a Retrofit service dynamically.
     * 
     * @param serviceClass The API interface to implement.
     * @param baseUrl The base URL for this specific API.
     * @param interceptors Optional specific interceptors (e.g. Auth Token, API Key) for this API.
     */
    public fun <Service> createService(
        serviceClass: Class<Service>,
        baseUrl: String,
        interceptors: List<Interceptor> = emptyList()
    ): Service {
        // We use .newBuilder() to clone the base OkHttpClient. 
        // This prevents interceptors from leaking across different APIs!
        val clientBuilder = okHttpClient.newBuilder()
        
        interceptors.forEach { interceptor ->
            clientBuilder.addInterceptor(interceptor)
        }
        
        return retrofitBuilder
            .client(clientBuilder.build())
            .baseUrl(baseUrl)
            .build()
            .create(serviceClass)
    }
}
