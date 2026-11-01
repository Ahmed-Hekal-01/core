package com.link.core.presentation.session

import kotlinx.coroutines.flow.StateFlow

public interface SessionController {
    public val isFirstLaunch: StateFlow<Boolean>
    public val isGuest: StateFlow<Boolean>
    
    public suspend fun getIsFirstLaunch(): Boolean
    public suspend fun getIsGuest(): Boolean
    
    public suspend fun setFirstLaunchDone()
    public suspend fun setGuest(isGuest: Boolean)
}
