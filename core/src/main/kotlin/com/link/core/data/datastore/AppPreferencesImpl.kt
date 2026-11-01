package com.link.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "moviebox_preferences")

internal class AppPreferencesImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppPreferences {

    private val dataStore = context.dataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val IS_GUEST = booleanPreferencesKey("is_guest")
    }

    override val theme: Flow<AppTheme> = dataStore.data.map { prefs ->
        AppTheme.entries.firstOrNull { it.name == prefs[Keys.THEME] } ?: AppTheme.LIGHT
    }

    override val language: Flow<AppLanguage> = dataStore.data.map { prefs ->
        AppLanguage.entries.firstOrNull { it.name == prefs[Keys.LANGUAGE] } ?: AppLanguage.EN
    }

    override val isFirstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.IS_FIRST_LAUNCH] ?: true
    }

    override val isGuest: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.IS_GUEST] ?: false
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { prefs -> prefs[Keys.THEME] = theme.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs -> prefs[Keys.LANGUAGE] = language.name }
    }

    override suspend fun setFirstLaunchDone() {
        dataStore.edit { prefs -> prefs[Keys.IS_FIRST_LAUNCH] = false }
    }

    override suspend fun setGuest(isGuest: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.IS_GUEST] = isGuest }
    }
}
