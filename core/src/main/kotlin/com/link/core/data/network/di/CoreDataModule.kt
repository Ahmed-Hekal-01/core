package com.link.core.data.network.di

import android.content.Context
import android.net.ConnectivityManager
import com.link.core.data.network.CheckNetworkState
import com.link.core.data.network.CheckNetworkStateImpl
import com.link.core.data.network.ValidateRemoteSource
import com.link.core.data.network.ValidateRemoteSourceImpl
import com.link.core.data.network.SafeApiCall
import com.link.core.domain.models.Resource
import com.link.core.domain.models.NetworkError
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CoreDataModule {

    companion object {
        @Provides
        @Singleton
        fun providesConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
            return context.getSystemService(ConnectivityManager::class.java)
        }

        @Provides
        @Singleton
        fun provideSafeApiCall(
            validateRemoteSource: ValidateRemoteSource,
            checkNetworkState: CheckNetworkState
        ): SafeApiCall = object : SafeApiCall {
            override suspend fun <T> invoke(apiCall: suspend () -> T): Resource<T> {
                return if (checkNetworkState.isConnected()) {
                    validateRemoteSource.validate(apiCall)
                } else {
                    Resource.Error(NetworkError.NoInternet)
                }
            }
        }
    }

    @Binds
    @Singleton
    abstract fun bindsNetworkCheck(checkNetworkStateImpl: CheckNetworkStateImpl): CheckNetworkState

    @Binds
    @Singleton
    abstract fun bindsValidateRemoteSource(validateRemoteSourceImpl: ValidateRemoteSourceImpl): ValidateRemoteSource
}
