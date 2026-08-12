package com.pdfsaathi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pdfsaathi.app.data.local.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDocumentDao {

    @Query("SELECT * FROM pdf_documents ORDER BY lastOpened DESC")
    fun getAllDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE lastOpened > 0 ORDER BY lastOpened DESC LIMIT 10")
    fun getRecentDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchDocuments(query: String): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): PdfDocumentEntity?

    @Query("SELECT * FROM pdf_documents WHERE uri = :uri LIMIT 1")
    suspend fun getDocumentByUri(uri: String): PdfDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: PdfDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocumentsIfNotExist(documents: List<PdfDocumentEntity>)

    @Update
    suspend fun updateDocument(document: PdfDocumentEntity)

    @Query("UPDATE pdf_documents SET lastPage = :page, lastOpened = :timestamp WHERE id = :id")
    suspend fun updateReadingPosition(id: String, page: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pdf_documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteState(id: String, isFavorite: Boolean)

    @Query("UPDATE pdf_documents SET lastOpened = 0")
    suspend fun clearRecents()

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteDocument(id: String)
}
