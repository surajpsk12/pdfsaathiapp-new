package com.pdfsaathi.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pdfsaathi.app.data.local.dao.BookmarkDao
import com.pdfsaathi.app.data.local.dao.PdfDocumentDao
import com.pdfsaathi.app.data.local.entity.BookmarkEntity
import com.pdfsaathi.app.data.local.entity.PdfDocumentEntity

@Database(
    entities = [PdfDocumentEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfDocumentDao(): PdfDocumentDao
    abstract fun bookmarkDao(): BookmarkDao
}
