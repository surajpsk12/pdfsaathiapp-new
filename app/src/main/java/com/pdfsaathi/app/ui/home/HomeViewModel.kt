package com.pdfsaathi.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.domain.repository.PdfRepository
import com.pdfsaathi.app.domain.usecase.GetFavoriteDocumentsUseCase
import com.pdfsaathi.app.domain.usecase.GetRecentDocumentsUseCase
import com.pdfsaathi.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentDocumentsUseCase: GetRecentDocumentsUseCase,
    private val getFavoriteDocumentsUseCase: GetFavoriteDocumentsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val pdfRepository: PdfRepository
) : ViewModel() {

    init {
        scanStorage()
    }

    fun scanStorage() {
        viewModelScope.launch {
            pdfRepository.scanStorageForPdfs()
        }
    }

    val recentDocuments: StateFlow<List<PdfDocument>> = getRecentDocumentsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDocuments: StateFlow<List<PdfDocument>> = getFavoriteDocumentsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<PdfDocument>> = pdfRepository.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(documentId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(documentId, !currentStatus)
        }
    }

    fun importPdfFromUri(uri: Uri, onImported: (PdfDocument) -> Unit) {
        viewModelScope.launch {
            val doc = pdfRepository.importDocumentFromUri(uri)
            doc?.let { onImported(it) }
        }
    }
}
