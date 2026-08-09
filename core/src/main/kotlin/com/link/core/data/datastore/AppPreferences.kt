package com.link.core.data.datastore

import kotlinx.coroutines.flow.Flow

public enum class AppTheme { LIGHT, DARK }
public enum class AppLanguage { EN, AR }

internal interface AppPreferences {
    val theme: Flow<AppTheme>
    val language: Flow<AppLanguage>
    val isFirstLaunch: Flow<Boolean>
    val isGuest: Flow<Boolean>
    
    suspend fun setTheme(theme: AppTheme)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setFirstLaunchDone()
    suspend fun setGuest(isGuest: Boolean)
}
