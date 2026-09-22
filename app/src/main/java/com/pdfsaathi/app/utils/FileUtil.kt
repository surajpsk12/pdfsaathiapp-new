package com.pdfsaathi.app.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.pdfsaathi.app.domain.model.PdfDocument
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtil {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Document.pdf"
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                }
            }
        } else {
            uri.path?.let { path ->
                val file = java.io.File(path)
                name = file.name
            }
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } else {
            uri.path?.let { path ->
                size = java.io.File(path).length()
            }
        }
        return size
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Resolves and opens an InputStream for the given document from local cache or storage URI.
     */
    fun openPdfInputStream(context: Context, document: PdfDocument): InputStream? {
        val safeId1 = if (document.id.isNotBlank()) document.id else ""
        val safeFileName1 = "${safeId1.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf"
        val pdfDir = File(context.filesDir, "saved_pdfs")
        val cachedFile1 = if (safeId1.isNotBlank()) File(pdfDir, safeFileName1) else null

        val uri = try { Uri.parse(document.uri) } catch (_: Exception) { Uri.EMPTY }
        val safeId2 = uri.hashCode().toString()
        val safeFileName2 = "${safeId2.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf"
        val cachedFile2 = File(pdfDir, safeFileName2)

        return when {
            cachedFile1 != null && cachedFile1.exists() && cachedFile1.length() > 0L -> {
                cachedFile1.inputStream()
            }
            cachedFile2.exists() && cachedFile2.length() > 0L -> {
                cachedFile2.inputStream()
            }
            uri != Uri.EMPTY && uri.scheme == "content" -> {
                context.contentResolver.openInputStream(uri)
            }
            uri != Uri.EMPTY && uri.scheme == "file" -> {
                val path = uri.path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) file.inputStream() else null
                } else null
            }
            document.uri.isNotBlank() && File(document.uri).exists() -> {
                File(document.uri).inputStream()
            }
            else -> null
        }
    }

    /**
     * Prepares a shareable FileProvider content URI with the proper filename for sharing with external apps.
     */
    fun getShareablePdfUri(context: Context, document: PdfDocument): Uri? {
        return try {
            val sourceStream = openPdfInputStream(context, document) ?: return null

            var fileName = document.name.trim()
            if (fileName.isBlank()) {
                fileName = "Document.pdf"
            }
            if (!fileName.endsWith(".pdf", ignoreCase = true)) {
                fileName = "$fileName.pdf"
            }
            fileName = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")

            val shareDir = File(context.cacheDir, "shared_pdfs").apply { if (!exists()) mkdirs() }
            val shareFile = File(shareDir, fileName)

            shareFile.outputStream().use { out ->
                sourceStream.use { input ->
                    input.copyTo(out)
                }
            }

            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                shareFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies the given PDF document into the device's public Downloads directory.
     * Uses MediaStore.Downloads on Android 10+ (API 29+) and public external storage on older versions.
     */
    fun savePdfToDownloads(context: Context, document: PdfDocument): Result<String> {
        return try {
            val sourceStream = openPdfInputStream(context, document)
                ?: throw IOException("Could not locate source PDF data.")

            var fileName = document.name.trim()
            if (fileName.isBlank()) {
                fileName = "Document.pdf"
            }
            if (!fileName.endsWith(".pdf", ignoreCase = true)) {
                fileName = "$fileName.pdf"
            }
            // Sanitize file name for file systems
            fileName = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Failed to create MediaStore entry in Downloads.")

                resolver.openOutputStream(itemUri)?.use { out ->
                    sourceStream.use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IOException("Failed to open output stream to MediaStore.")

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)

                Result.success(fileName)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                var targetFile = File(downloadsDir, fileName)
                var count = 1
                val base = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "pdf")
                while (targetFile.exists()) {
                    targetFile = File(downloadsDir, "$base ($count).$ext")
                    count++
                }

                targetFile.outputStream().use { out ->
                    sourceStream.use { input ->
                        input.copyTo(out)
                    }
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )

                Result.success(targetFile.name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
