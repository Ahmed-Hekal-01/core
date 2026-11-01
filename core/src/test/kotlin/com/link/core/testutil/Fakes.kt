package com.link.core.testutil

import com.link.core.data.datastore.AppLanguage
import com.link.core.data.datastore.AppPreferences
import com.link.core.data.datastore.AppTheme
import com.link.core.domain.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeAppPreferences : AppPreferences {
    val themeFlow = MutableStateFlow(AppTheme.LIGHT)
    val languageFlow = MutableStateFlow(AppLanguage.EN)
    val firstLaunchFlow = MutableStateFlow(true)
    val guestFlow = MutableStateFlow(false)

    override val theme = themeFlow
    override val language = languageFlow
    override val isFirstLaunch = firstLaunchFlow
    override val isGuest = guestFlow

    override suspend fun setTheme(theme: AppTheme) { themeFlow.value = theme }
    override suspend fun setLanguage(language: AppLanguage) { languageFlow.value = language }
    override suspend fun setFirstLaunchDone() { firstLaunchFlow.value = false }
    override suspend fun setGuest(isGuest: Boolean) { guestFlow.value = isGuest }
}

internal class TestDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val main = dispatcher
    override val default = dispatcher
}
