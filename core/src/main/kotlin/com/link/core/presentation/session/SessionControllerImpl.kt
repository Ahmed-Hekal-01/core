package com.link.core.presentation.session

import com.link.core.data.datastore.AppPreferences
import com.link.core.domain.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SessionControllerImpl @Inject constructor(
    private val appPreferences: AppPreferences,
    dispatcherProvider: DispatcherProvider
) : SessionController {

    private val scope = CoroutineScope(dispatcherProvider.io)

    override val isFirstLaunch: StateFlow<Boolean> = appPreferences.isFirstLaunch
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    override val isGuest: StateFlow<Boolean> = appPreferences.isGuest
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    override suspend fun getIsFirstLaunch(): Boolean = appPreferences.isFirstLaunch.first()
    override suspend fun getIsGuest(): Boolean = appPreferences.isGuest.first()

    override suspend fun setFirstLaunchDone() {
        appPreferences.setFirstLaunchDone()
    }

    override suspend fun setGuest(isGuest: Boolean) {
        appPreferences.setGuest(isGuest)
    }
}
