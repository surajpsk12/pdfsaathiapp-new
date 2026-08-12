package com.pdfsaathi.app.ui.reader

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsaathi.app.data.pdf.PdfRendererManager
import com.pdfsaathi.app.domain.model.Bookmark
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.domain.model.ReadingTheme
import com.pdfsaathi.app.domain.model.SearchMatch
import com.pdfsaathi.app.domain.repository.PdfRepository
import com.pdfsaathi.app.domain.usecase.ManageBookmarksUseCase
import com.pdfsaathi.app.domain.usecase.SaveReadingPositionUseCase
import com.pdfsaathi.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfRepository: PdfRepository,
    private val pdfRendererManager: PdfRendererManager,
    private val saveReadingPositionUseCase: SaveReadingPositionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val manageBookmarksUseCase: ManageBookmarksUseCase
) : ViewModel() {

    val currentDocument = MutableStateFlow<PdfDocument?>(null)
    val currentPage = MutableStateFlow(1)
    val totalPages = MutableStateFlow(1)
    val readingTheme = MutableStateFlow(ReadingTheme.LIGHT)
    val isControlsVisible = MutableStateFlow(true)
    val currentPageBitmap = MutableStateFlow<ImageBitmap?>(null)

    // In-PDF Text Search
    val pdfSearchQuery = MutableStateFlow("")
    val pdfSearchMatches = MutableStateFlow<List<SearchMatch>>(emptyList())
    val currentMatchIndex = MutableStateFlow(0)

    val bookmarks: StateFlow<List<Bookmark>> = MutableStateFlow(emptyList())

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            val doc = if (documentId.startsWith("content://") || documentId.startsWith("file://")) {
                pdfRepository.importDocumentFromUri(Uri.parse(documentId))
            } else {
                pdfRepository.getDocumentById(documentId)
            }

            doc?.let {
                currentDocument.value = it
                currentPage.value = it.lastPage
                val count = pdfRendererManager.openDocument(context, Uri.parse(it.uri))
                totalPages.value = if (count > 0) count else 15
                renderPage(it.lastPage)
            }
        }
    }

    fun renderPage(pageIndex: Int) {
        viewModelScope.launch {
            currentPage.value = pageIndex
            val bitmap = pdfRendererManager.renderPage(pageIndex - 1)
            currentPageBitmap.value = bitmap
            currentDocument.value?.id?.let { id ->
                saveReadingPositionUseCase(id, pageIndex)
            }
        }
    }

    fun toggleControlsVisibility() {
        isControlsVisible.value = !isControlsVisible.value
    }

    fun toggleFavorite() {
        val doc = currentDocument.value ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(doc.id, !doc.isFavorite)
            currentDocument.value = doc.copy(isFavorite = !doc.isFavorite)
        }
    }

    fun addBookmark() {
        val doc = currentDocument.value ?: return
        val page = currentPage.value
        viewModelScope.launch {
            manageBookmarksUseCase.addBookmark(doc.id, page, "Bookmark Page $page")
        }
    }

    fun searchPdf(query: String) {
        pdfSearchQuery.value = query
        viewModelScope.launch {
            val matches = pdfRendererManager.searchInPdf(query)
            pdfSearchMatches.value = matches
            if (matches.isNotEmpty()) {
                currentMatchIndex.value = 1
                renderPage(matches[0].pageNumber)
            }
        }
    }

    fun changeTheme(theme: ReadingTheme) {
        readingTheme.value = theme
    }

    override fun onCleared() {
        super.onCleared()
        pdfRendererManager.closeDocument()
    }
}
