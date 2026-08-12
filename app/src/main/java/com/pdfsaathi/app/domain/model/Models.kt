package com.pdfsaathi.app.domain.model

data class Bookmark(
    val id: Long = 0,
    val documentId: String,
    val pageNumber: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReadingTheme {
    LIGHT,
    DARK,
    SEPIA,
    SYSTEM
}

data class SearchMatch(
    val pageNumber: Int,
    val snippet: String,
    val matchIndex: Int
)
