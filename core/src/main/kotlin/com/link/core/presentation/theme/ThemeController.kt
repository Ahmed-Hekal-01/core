package com.link.core.presentation.theme

import com.link.core.data.datastore.AppTheme
import kotlinx.coroutines.flow.StateFlow

public interface ThemeController {
    public val currentTheme: StateFlow<AppTheme>
    public suspend fun setTheme(theme: AppTheme)
}
