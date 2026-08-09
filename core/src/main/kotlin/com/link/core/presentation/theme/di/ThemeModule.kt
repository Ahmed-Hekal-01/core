package com.link.core.presentation.theme.di

import com.link.core.presentation.theme.LocaleController
import com.link.core.presentation.theme.LocaleControllerImpl
import com.link.core.presentation.theme.ThemeController
import com.link.core.presentation.theme.ThemeControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeController(impl: ThemeControllerImpl): ThemeController

    @Binds
    @Singleton
    abstract fun bindLocaleController(impl: LocaleControllerImpl): LocaleController
}
