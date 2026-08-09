package com.link.core.presentation.theme

import com.link.core.data.datastore.AppLanguage
import kotlinx.coroutines.flow.StateFlow

public interface LocaleController {
    public val currentLanguage: StateFlow<AppLanguage>
    public suspend fun setLanguage(language: AppLanguage)
}
