package com.pdfsaathi.app.data.repository

import android.content.Context
import android.net.Uri
import com.pdfsaathi.app.data.local.dao.BookmarkDao
import com.pdfsaathi.app.data.local.dao.PdfDocumentDao
import com.pdfsaathi.app.data.local.entity.BookmarkEntity
import com.pdfsaathi.app.data.local.entity.PdfDocumentEntity
import com.pdfsaathi.app.domain.model.Bookmark
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.domain.repository.PdfRepository
import com.pdfsaathi.app.utils.FileUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfDocumentDao: PdfDocumentDao,
    private val bookmarkDao: BookmarkDao
) : PdfRepository {

    override fun getAllDocuments(): Flow<List<PdfDocument>> {
        return pdfDocumentDao.getAllDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentDocuments(): Flow<List<PdfDocument>> {
        return pdfDocumentDao.getRecentDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteDocuments(): Flow<List<PdfDocument>> {
        return pdfDocumentDao.getFavoriteDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchDocuments(query: String): Flow<List<PdfDocument>> {
        return pdfDocumentDao.searchDocuments(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDocumentById(id: String): PdfDocument? {
        return pdfDocumentDao.getDocumentById(id)?.toDomain()
    }

    override suspend fun getDocumentByUri(uri: String): PdfDocument? {
        return pdfDocumentDao.getDocumentByUri(uri)?.toDomain()
    }

    override suspend fun importDocumentFromUri(uri: Uri): PdfDocument? {
        val uriString = uri.toString()

        if (uri.scheme == "content") {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val existing = pdfDocumentDao.getDocumentByUri(uriString)
        val safeId = existing?.id ?: UUID.randomUUID().toString()
        val pdfDir = java.io.File(context.filesDir, "saved_pdfs").apply { if (!exists()) mkdirs() }
        val safeFileName = "${safeId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf"
        val cachedFile = java.io.File(pdfDir, safeFileName)

        if (!cachedFile.exists() || cachedFile.length() == 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(cachedFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val storedUri = if (cachedFile.exists() && cachedFile.length() > 0) {
            Uri.fromFile(cachedFile).toString()
        } else {
            uriString
        }

        if (existing != null) {
            val updated = existing.copy(
                uri = storedUri,
                lastOpened = System.currentTimeMillis()
            )
            pdfDocumentDao.updateDocument(updated)
            return updated.toDomain()
        }

        val name = FileUtil.getFileName(context, uri)
        val size = if (cachedFile.exists() && cachedFile.length() > 0) cachedFile.length() else FileUtil.getFileSize(context, uri)
        val newEntity = PdfDocumentEntity(
            id = safeId,
            uri = storedUri,
            name = name,
            size = size,
            lastModified = System.currentTimeMillis(),
            lastOpened = System.currentTimeMillis(),
            lastPage = 1,
            totalPages = 1,
            isFavorite = false
        )

        pdfDocumentDao.insertDocument(newEntity)
        return newEntity.toDomain()
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        pdfDocumentDao.updateFavoriteState(id, isFavorite)
    }

    override suspend fun saveReadingPosition(id: String, page: Int) {
        pdfDocumentDao.updateReadingPosition(id, page)
    }

    override suspend fun updateTotalPages(id: String, totalPages: Int) {
        pdfDocumentDao.updateDocumentTotalPages(id, totalPages)
    }

    override suspend fun clearRecents() {
        pdfDocumentDao.clearRecents()
    }

    override suspend fun deleteDocument(id: String) {
        pdfDocumentDao.deleteDocument(id)
        try {
            val safeFileName = "${id.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf"
            val pdfDir = java.io.File(context.filesDir, "saved_pdfs")
            val cachedFile = java.io.File(pdfDir, safeFileName)
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun scanStorageForPdfs() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val projection = arrayOf(
                    android.provider.MediaStore.Files.FileColumns._ID,
                    android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
                    android.provider.MediaStore.Files.FileColumns.SIZE,
                    android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED,
                    android.provider.MediaStore.Files.FileColumns.DATA
                )
                val selection = "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pdf'"
                val selectionArgs = arrayOf("application/pdf")
                val sortOrder = "${android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

                val cursor = context.contentResolver.query(
                    android.provider.MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns._ID)
                    val nameCol = c.getColumnIndex(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeCol = c.getColumnIndex(android.provider.MediaStore.Files.FileColumns.SIZE)
                    val dateCol = c.getColumnIndex(android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val dataCol = c.getColumnIndex(android.provider.MediaStore.Files.FileColumns.DATA)

                    val entities = mutableListOf<PdfDocumentEntity>()
                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val contentUri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Files.getContentUri("external"), id
                        ).toString()
                        val filePath = if (dataCol != -1) c.getString(dataCol) else null
                        val finalUri = if (!filePath.isNullOrBlank() && java.io.File(filePath).exists()) filePath else contentUri

                        val name = if (nameCol != -1) c.getString(nameCol) ?: "Document.pdf" else "Document.pdf"
                        val size = if (sizeCol != -1) c.getLong(sizeCol) else 0L
                        val dateModified = if (dateCol != -1) c.getLong(dateCol) * 1000L else System.currentTimeMillis()

                        val entity = PdfDocumentEntity(
                            id = id.toString(),
                            uri = finalUri,
                            name = name,
                            size = size,
                            lastModified = dateModified,
                            lastOpened = 0L,
                            lastPage = 1,
                            totalPages = 1,
                            isFavorite = false
                        )
                        entities.add(entity)
                    }
                    if (entities.isNotEmpty()) {
                        for (entity in entities) {
                            val existing = pdfDocumentDao.getDocumentById(entity.id)
                            if (existing != null) {
                                if (existing.uri != entity.uri) {
                                    pdfDocumentDao.updateDocument(existing.copy(uri = entity.uri))
                                }
                            } else {
                                pdfDocumentDao.insertDocument(entity)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getBookmarksForDocument(documentId: String): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksForDocument(documentId).map { list ->
            list.map { Bookmark(it.id, it.documentId, it.pageNumber, it.title, it.createdAt) }
        }
    }

    override suspend fun addBookmark(documentId: String, pageNumber: Int, title: String): Long {
        val entity = BookmarkEntity(
            documentId = documentId,
            pageNumber = pageNumber,
            title = title
        )
        return bookmarkDao.insertBookmark(entity)
    }

    override suspend fun removeBookmark(documentId: String, pageNumber: Int) {
        bookmarkDao.deleteBookmark(documentId, pageNumber)
    }

    private fun PdfDocumentEntity.toDomain(): PdfDocument {
        return PdfDocument(
            id = id,
            uri = uri,
            name = name,
            size = size,
            formattedSize = FileUtil.formatFileSize(size),
            lastModified = lastModified,
            formattedDate = FileUtil.formatDate(lastModified),
            lastOpened = lastOpened,
            lastPage = lastPage,
            totalPages = totalPages,
            isFavorite = isFavorite
        )
    }
}
