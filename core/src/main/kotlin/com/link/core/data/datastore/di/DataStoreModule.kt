package com.link.core.data.datastore.di

import com.link.core.data.datastore.AppPreferences
import com.link.core.data.datastore.AppPreferencesImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindAppPreferences(appPreferencesImpl: AppPreferencesImpl): AppPreferences
}
