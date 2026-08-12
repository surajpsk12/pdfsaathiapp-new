package com.pdfsaathi.app.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.domain.repository.PdfRepository
import com.pdfsaathi.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption {
    NAME,
    DATE,
    SIZE
}

@HiltViewModel
class AllDocumentsViewModel @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    val sortOption = MutableStateFlow(SortOption.NAME)
    val isAscending = MutableStateFlow(true)
    val selectedFolder = MutableStateFlow<String?>(null)

    fun scanStorage() {
        viewModelScope.launch {
            pdfRepository.scanStorageForPdfs()
        }
    }

    fun selectFolder(folderName: String?) {
        selectedFolder.value = folderName
    }

    val documents: StateFlow<List<PdfDocument>> = combine(
        pdfRepository.getAllDocuments(),
        sortOption,
        selectedFolder
    ) { docs, option, folder ->
        val filtered = if (folder == null) {
            docs
        } else {
            docs.filter { it.name.contains(folder, ignoreCase = true) || folder.contains("Semester", ignoreCase = true) }
        }

        when (option) {
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.DATE -> filtered.sortedByDescending { it.lastModified }
            SortOption.SIZE -> filtered.sortedByDescending { it.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(documentId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(documentId, !currentStatus)
        }
    }
}
