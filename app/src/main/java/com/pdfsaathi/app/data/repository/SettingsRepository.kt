package com.pdfsaathi.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pdfsaathi.app.domain.model.ReadingTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pdf_saathi_settings", Context.MODE_PRIVATE)

    private val _appTheme = MutableStateFlow(
        when (prefs.getString("app_theme", "light")) {
            "dark" -> ReadingTheme.DARK
            else -> ReadingTheme.LIGHT
        }
    )
    val appTheme: StateFlow<ReadingTheme> = _appTheme

    private val _defaultViewerMode = MutableStateFlow("single").also {
        prefs.edit().putString("default_viewer_mode", "single").apply()
    }
    val defaultViewerMode: StateFlow<String> = _defaultViewerMode

    fun setAppTheme(theme: ReadingTheme) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", if (theme == ReadingTheme.DARK) "dark" else "light").apply()
    }

    fun setDefaultViewerMode(mode: String) {
        _defaultViewerMode.value = mode
        prefs.edit().putString("default_viewer_mode", mode).apply()
    }
}
