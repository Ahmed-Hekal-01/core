package com.link.core.presentation.session.di

import com.link.core.presentation.session.SessionController
import com.link.core.presentation.session.SessionControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionModule {

    @Binds
    @Singleton
    abstract fun bindSessionController(impl: SessionControllerImpl): SessionController
}
