package com.link.core.presentation.theme

import com.link.core.data.datastore.AppPreferences
import com.link.core.data.datastore.AppTheme
import com.link.core.domain.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ThemeControllerImpl @Inject constructor(
    private val appPreferences: AppPreferences,
    dispatcherProvider: DispatcherProvider
) : ThemeController {
    
    private val scope = CoroutineScope(dispatcherProvider.io)
    
    override val currentTheme: StateFlow<AppTheme> = appPreferences.theme
        .catch { }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AppTheme.LIGHT
        )

    override suspend fun setTheme(theme: AppTheme) {
        appPreferences.setTheme(theme)
    }
}
