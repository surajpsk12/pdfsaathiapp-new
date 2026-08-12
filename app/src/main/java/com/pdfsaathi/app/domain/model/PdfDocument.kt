package com.pdfsaathi.app.domain.model

data class PdfDocument(
    val id: String,
    val uri: String,
    val name: String,
    val size: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String,
    val lastOpened: Long,
    val lastPage: Int = 1,
    val totalPages: Int = 0,
    val isFavorite: Boolean = false
)
