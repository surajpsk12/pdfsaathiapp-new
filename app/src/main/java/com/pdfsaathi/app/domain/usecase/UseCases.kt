package com.pdfsaathi.app.domain.usecase

import android.net.Uri
import com.pdfsaathi.app.domain.model.Bookmark
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.domain.repository.PdfRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentDocumentsUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    operator fun invoke(): Flow<List<PdfDocument>> = repository.getRecentDocuments()
}

class GetFavoriteDocumentsUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    operator fun invoke(): Flow<List<PdfDocument>> = repository.getFavoriteDocuments()
}

class SearchDocumentsUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    operator fun invoke(query: String): Flow<List<PdfDocument>> = repository.searchDocuments(query)
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(id: String, isFavorite: Boolean) {
        repository.toggleFavorite(id, isFavorite)
    }
}

class SaveReadingPositionUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(id: String, page: Int) {
        repository.saveReadingPosition(id, page)
    }
}

class ManageBookmarksUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    fun getBookmarks(documentId: String): Flow<List<Bookmark>> = repository.getBookmarksForDocument(documentId)
    suspend fun addBookmark(documentId: String, page: Int, title: String) = repository.addBookmark(documentId, page, title)
    suspend fun removeBookmark(documentId: String, page: Int) = repository.removeBookmark(documentId, page)
}
