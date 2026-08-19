package com.pdfsaathi.app.ui.reader

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsaathi.app.data.pdf.PdfRendererManager
import com.pdfsaathi.app.data.repository.SettingsRepository
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfRepository: PdfRepository,
    private val pdfRendererManager: PdfRendererManager,
    private val saveReadingPositionUseCase: SaveReadingPositionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val manageBookmarksUseCase: ManageBookmarksUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val currentDocument = MutableStateFlow<PdfDocument?>(null)
    val currentPage = MutableStateFlow(1)
    val totalPages = MutableStateFlow(1)
    val initialRestoredPage = MutableStateFlow<Int?>(null)
    val readingTheme = MutableStateFlow(settingsRepository.appTheme.value)
    val viewerMode = MutableStateFlow(settingsRepository.defaultViewerMode.value)
    val isControlsVisible = MutableStateFlow(true)
    val currentPageBitmap = MutableStateFlow<ImageBitmap?>(null)

    // Map storing rendered page bitmaps for Continuous Scroll Mode
    val pageBitmaps = MutableStateFlow<Map<Int, ImageBitmap>>(emptyMap())

    // In-PDF Text Search
    val pdfSearchQuery = MutableStateFlow("")
    val pdfSearchMatches = MutableStateFlow<List<SearchMatch>>(emptyList())
    val currentMatchIndex = MutableStateFlow(0)

    val bookmarks: StateFlow<List<Bookmark>> = MutableStateFlow(emptyList())

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            var doc = if (documentId.startsWith("content://") || documentId.startsWith("file://") || documentId.contains("/")) {
                pdfRepository.importDocumentFromUri(Uri.parse(documentId))
            } else {
                pdfRepository.getDocumentById(documentId)
            }

            if (doc == null) {
                doc = pdfRepository.getDocumentByUri(documentId)
            }

            if (doc == null && (documentId.startsWith("content://") || documentId.startsWith("file://") || documentId.contains("/"))) {
                val uri = Uri.parse(documentId)
                val name = com.pdfsaathi.app.utils.FileUtil.getFileName(context, uri)
                doc = PdfDocument(
                    id = documentId,
                    uri = documentId,
                    name = name,
                    size = 0L,
                    formattedSize = "",
                    lastModified = System.currentTimeMillis(),
                    formattedDate = "",
                    lastOpened = System.currentTimeMillis(),
                    lastPage = 1,
                    totalPages = 1,
                    isFavorite = false
                )
            }

            doc?.let { pdfDoc ->
                currentDocument.value = pdfDoc
                val targetUri = try { Uri.parse(pdfDoc.uri) } catch (e: Exception) { Uri.EMPTY }
                val realCount = pdfRendererManager.openDocument(context, targetUri, pdfDoc.id)
                val validTotalPages = if (realCount > 0) realCount else 1
                totalPages.value = validTotalPages

                val safeLastPage = pdfDoc.lastPage.coerceIn(1, validTotalPages)
                currentPage.value = safeLastPage

                // Update accurate total pages in database
                pdfRepository.updateTotalPages(pdfDoc.id, validTotalPages)
                initialRestoredPage.value = safeLastPage

                renderPage(safeLastPage)
            }
        }
    }

    fun renderPage(pageIndex: Int) {
        viewModelScope.launch {
            val validPage = pageIndex.coerceIn(1, totalPages.value)
            currentPage.value = validPage

            val bitmap = pdfRendererManager.renderPageBitmap(validPage - 1)
            if (bitmap != null) {
                currentPageBitmap.value = bitmap
                val currentMap = pageBitmaps.value.toMutableMap()
                currentMap[validPage] = bitmap
                pageBitmaps.value = currentMap
            }

            currentDocument.value?.id?.let { id ->
                saveReadingPositionUseCase(id, validPage)
            }

            // Pre-load adjacent pages for seamless horizontal swipe transitions
            if (validPage > 1) {
                loadPageBitmapForContinuous(validPage - 1)
            }
            if (validPage < totalPages.value) {
                loadPageBitmapForContinuous(validPage + 1)
            }
        }
    }

    fun loadPageBitmapForContinuous(pageIndex: Int) {
        if (pageBitmaps.value.containsKey(pageIndex)) return
        viewModelScope.launch {
            val bitmap = pdfRendererManager.renderPageBitmap(pageIndex - 1)
            if (bitmap != null) {
                val currentMap = pageBitmaps.value.toMutableMap()
                currentMap[pageIndex] = bitmap
                pageBitmaps.value = currentMap
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

    fun toggleViewerMode() {
        viewerMode.value = if (viewerMode.value == "continuous") "single" else "continuous"
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
