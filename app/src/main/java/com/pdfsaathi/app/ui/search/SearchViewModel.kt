package com.pdfsaathi.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.domain.usecase.SearchDocumentsUseCase
import com.pdfsaathi.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.pdfsaathi.app.domain.repository.PdfRepository

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val pdfRepository: PdfRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<PdfDocument>> = searchQuery
        .debounce(300)
        .flatMapLatest { query -> searchDocumentsUseCase(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(documentId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(documentId, !currentStatus)
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            pdfRepository.deleteDocument(documentId)
        }
    }
}
