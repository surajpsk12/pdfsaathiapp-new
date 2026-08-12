package com.pdfsaathi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pdfsaathi.app.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getBookmarksForDocument(documentId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE documentId = :documentId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getBookmark(documentId: String, pageNumber: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE documentId = :documentId AND pageNumber = :pageNumber")
    suspend fun deleteBookmark(documentId: String, pageNumber: Int)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
}
