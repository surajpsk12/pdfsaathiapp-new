package com.pdfsaathi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val lastOpened: Long,
    val lastPage: Int = 1,
    val totalPages: Int = 0,
    val isFavorite: Boolean = false
)
