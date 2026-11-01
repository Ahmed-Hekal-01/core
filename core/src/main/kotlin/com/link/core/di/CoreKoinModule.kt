package com.link.core.di

import android.net.ConnectivityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.link.core.BuildConfig
import com.link.core.data.datastore.AppPreferences
import com.link.core.data.datastore.AppPreferencesImpl
import com.link.core.data.firebase.auth.AuthService
import com.link.core.data.firebase.auth.FirebaseAuthService
import com.link.core.data.firebase.firestore.FirestoreService
import com.link.core.data.firebase.firestore.FirestoreServiceImpl
import com.link.core.data.network.CheckNetworkState
import com.link.core.data.network.CheckNetworkStateImpl
import com.link.core.data.network.KtorClientFactory
import com.link.core.data.network.createBaseKtorClient
import io.ktor.client.HttpClient
import com.link.core.data.network.RetrofitFactory
import com.link.core.data.network.SafeApiCall
import com.link.core.data.network.ValidateRemoteSource
import com.link.core.data.network.ValidateRemoteSourceImpl
import com.link.core.data.utils.Constants
import com.link.core.domain.DefaultDispatcherProvider
import com.link.core.domain.DispatcherProvider
import com.link.core.domain.models.NetworkError
import com.link.core.domain.models.Resource
import com.link.core.presentation.session.SessionController
import com.link.core.presentation.session.SessionControllerImpl
import com.link.core.presentation.theme.LocaleController
import com.link.core.presentation.theme.LocaleControllerImpl
import com.link.core.presentation.theme.ThemeController
import com.link.core.presentation.theme.ThemeControllerImpl
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Koin equivalent of the Hilt modules. Requires `androidContext(...)` in `startKoin`.
 *
 * ```
 * startKoin { androidContext(this@App); modules(coreKoinModule) }
 * ```
 */
public val coreKoinModule: Module = module {
    // Common
    single<DispatcherProvider> { DefaultDispatcherProvider() }

    // DataStore / controllers
    single<AppPreferences> { AppPreferencesImpl(androidContext()) }
    single<SessionController> { SessionControllerImpl(get(), get()) }
    single<ThemeController> { ThemeControllerImpl(get(), get()) }
    single<LocaleController> { LocaleControllerImpl(get(), get()) }

    // Network (shared)
    single<Json> { Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true } }
    single<ConnectivityManager> { androidContext().getSystemService(ConnectivityManager::class.java) }
    single<CheckNetworkState> { CheckNetworkStateImpl(get()) }
    single<ValidateRemoteSource> { ValidateRemoteSourceImpl() }
    single<SafeApiCall> {
        val validate = get<ValidateRemoteSource>()
        val network = get<CheckNetworkState>()
        object : SafeApiCall {
            override suspend fun <T> invoke(apiCall: suspend () -> T): Resource<T> =
                if (network.isConnected()) validate.validate(apiCall)
                else Resource.Error(NetworkError.NoInternet)
        }
    }

    // Retrofit
    single<HttpLoggingInterceptor> {
        HttpLoggingInterceptor { android.util.Log.d("OkHttp", it) }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
    }
    single<OkHttpClient> {
        OkHttpClient.Builder()
            .connectTimeout(Constants.OkHTTPConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.OkHTTPConstants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.OkHTTPConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }
    single<Retrofit.Builder> {
        Retrofit.Builder()
            .client(get<OkHttpClient>())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
    }
    single { RetrofitFactory(get(), get()) }

    // Ktor
    single<HttpClient> { createBaseKtorClient(get()) }
    single { KtorClientFactory(get()) }
}

/**
 * Optional Firebase bindings. Add only if the app uses Firebase (needs google-services setup).
 */
public val firebaseKoinModule: Module = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { FirebaseFirestore.getInstance() }
    single<AuthService> { FirebaseAuthService(get()) }
    single<FirestoreService> { FirestoreServiceImpl(get()) }
}
