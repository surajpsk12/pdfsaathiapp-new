package com.pdfsaathi.app.domain.repository

import android.net.Uri
import com.pdfsaathi.app.domain.model.Bookmark
import com.pdfsaathi.app.domain.model.PdfDocument
import kotlinx.coroutines.flow.Flow

interface PdfRepository {
    fun getAllDocuments(): Flow<List<PdfDocument>>
    fun getRecentDocuments(): Flow<List<PdfDocument>>
    fun getFavoriteDocuments(): Flow<List<PdfDocument>>
    fun searchDocuments(query: String): Flow<List<PdfDocument>>
    suspend fun getDocumentById(id: String): PdfDocument?
    suspend fun getDocumentByUri(uri: String): PdfDocument?
    suspend fun importDocumentFromUri(uri: Uri): PdfDocument?
    suspend fun toggleFavorite(id: String, isFavorite: Boolean)
    suspend fun saveReadingPosition(id: String, page: Int)
    suspend fun clearRecents()
    
    // Bookmarks
    fun getBookmarksForDocument(documentId: String): Flow<List<Bookmark>>
    suspend fun addBookmark(documentId: String, pageNumber: Int, title: String): Long
    suspend fun removeBookmark(documentId: String, pageNumber: Int)
}
