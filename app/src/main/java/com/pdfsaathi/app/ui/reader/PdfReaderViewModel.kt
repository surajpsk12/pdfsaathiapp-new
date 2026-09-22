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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val viewerMode = MutableStateFlow("single")
    val isControlsVisible = MutableStateFlow(true)
    val currentPageBitmap = MutableStateFlow<ImageBitmap?>(null)

    // Map storing rendered page bitmaps for Continuous Scroll Mode
    val pageBitmaps = MutableStateFlow<Map<Int, ImageBitmap>>(emptyMap())

    // In-PDF Text Search
    val pdfSearchQuery = MutableStateFlow("")
    val pdfSearchMatches = MutableStateFlow<List<SearchMatch>>(emptyList())
    val currentMatchIndex = MutableStateFlow(0)

    val isPasswordRequired = MutableStateFlow(false)
    val passwordError = MutableStateFlow<String?>(null)

    val currentExtractedText = MutableStateFlow("")
    val isTextSelectionMode = MutableStateFlow(false)

    val isDownloading = MutableStateFlow(false)

    fun downloadCurrentDocument(onComplete: (success: Boolean, message: String) -> Unit) {
        val doc = currentDocument.value
        if (doc == null) {
            onComplete(false, "No document opened to download")
            return
        }
        if (isDownloading.value) return

        viewModelScope.launch {
            isDownloading.value = true
            val result = withContext(Dispatchers.IO) {
                com.pdfsaathi.app.utils.FileUtil.savePdfToDownloads(context, doc)
            }
            isDownloading.value = false
            result.fold(
                onSuccess = { savedName ->
                    onComplete(true, "Saved to Downloads: $savedName")
                },
                onFailure = { error ->
                    onComplete(false, error.message ?: "Failed to save PDF to phone storage")
                }
            )
        }
    }

    val isSharing = MutableStateFlow(false)

    fun shareCurrentDocument(targetContext: Context) {
        val doc = currentDocument.value ?: run {
            android.widget.Toast.makeText(targetContext, "No document opened to share", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (isSharing.value) return

        viewModelScope.launch {
            isSharing.value = true
            val shareUri = withContext(Dispatchers.IO) {
                com.pdfsaathi.app.utils.FileUtil.getShareablePdfUri(targetContext, doc)
            }
            isSharing.value = false

            if (shareUri != null) {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, shareUri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, doc.name)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = android.content.Intent.createChooser(intent, "Share PDF to any app").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    targetContext.startActivity(chooser)
                } catch (_: Exception) {
                    android.widget.Toast.makeText(targetContext, "No app found to handle sharing", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(targetContext, "Unable to prepare PDF file for sharing", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleTextSelectionMode() {
        isTextSelectionMode.value = !isTextSelectionMode.value
        if (isTextSelectionMode.value) {
            extractCurrentPageText()
        }
    }

    fun extractCurrentPageText() {
        viewModelScope.launch {
            val text = pdfRendererManager.extractPageText(context, currentPage.value - 1)
            currentExtractedText.value = text
        }
    }

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

                if (realCount == -1) {
                    isPasswordRequired.value = true
                    passwordError.value = null
                } else {
                    isPasswordRequired.value = false
                    passwordError.value = null
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
    }

    fun unlockDocumentWithPassword(password: String) {
        viewModelScope.launch {
            val doc = currentDocument.value ?: return@launch
            val targetUri = try { Uri.parse(doc.uri) } catch (e: Exception) { Uri.EMPTY }
            val realCount = pdfRendererManager.openDocument(context, targetUri, doc.id, password)

            if (realCount > 0) {
                isPasswordRequired.value = false
                passwordError.value = null
                totalPages.value = realCount
                val safeLastPage = doc.lastPage.coerceIn(1, realCount)
                currentPage.value = safeLastPage
                pdfRepository.updateTotalPages(doc.id, realCount)
                initialRestoredPage.value = safeLastPage
                renderPage(safeLastPage)
            } else {
                passwordError.value = "Incorrect password. Please try again."
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
            if (isTextSelectionMode.value) {
                extractCurrentPageText()
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
