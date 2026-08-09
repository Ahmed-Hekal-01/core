package com.link.core.presentation.theme

import com.link.core.data.datastore.AppLanguage
import com.link.core.data.datastore.AppPreferences
import com.link.core.domain.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocaleControllerImpl @Inject constructor(
    private val appPreferences: AppPreferences,
    dispatcherProvider: DispatcherProvider
) : LocaleController {
    
    private val scope = CoroutineScope(dispatcherProvider.io)

    override val currentLanguage: StateFlow<AppLanguage> = appPreferences.language
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AppLanguage.EN
        )

    override suspend fun setLanguage(language: AppLanguage) {
        appPreferences.setLanguage(language)
    }
}
