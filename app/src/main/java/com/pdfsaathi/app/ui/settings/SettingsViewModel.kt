package com.pdfsaathi.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsaathi.app.data.repository.SettingsRepository
import com.pdfsaathi.app.domain.model.ReadingTheme
import com.pdfsaathi.app.domain.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val appTheme: StateFlow<ReadingTheme> = settingsRepository.appTheme
    val defaultViewerMode: StateFlow<String> = settingsRepository.defaultViewerMode
    val showClearDialog = MutableStateFlow(false)

    fun setTheme(theme: ReadingTheme) {
        settingsRepository.setAppTheme(theme)
    }

    fun setViewerMode(mode: String) {
        settingsRepository.setDefaultViewerMode(mode)
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            pdfRepository.clearRecents()
            showClearDialog.value = false
        }
    }
}
