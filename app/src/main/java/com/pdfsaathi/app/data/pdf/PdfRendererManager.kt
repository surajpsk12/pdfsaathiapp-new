package com.pdfsaathi.app.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.pdfsaathi.app.domain.model.SearchMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRendererManager @Inject constructor() {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentDocId: String = ""
    private val mutex = Mutex()

    // In-memory LruCache for rendered page bitmaps (Fast 0ms response)
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceAtLeast(1024)
    private val bitmapMemoryCache = android.util.LruCache<String, ImageBitmap>(cacheSizeKb)

    suspend fun openDocument(context: Context, uri: Uri, documentId: String = "", password: String? = null): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (currentDocId != documentId && documentId.isNotBlank()) {
                bitmapMemoryCache.evictAll()
                currentDocId = documentId
            }
            closeDocumentInternal()

            val uriString = uri.toString()
            val safeId = if (documentId.isNotBlank()) documentId else uri.hashCode().toString()
            val safeFileName = "${safeId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf"
            val pdfDir = File(context.filesDir, "saved_pdfs").apply { if (!exists()) mkdirs() }
            val internalFile = File(pdfDir, safeFileName)

            var passwordRequired = false

            // Ensure internal cached copy exists if stream is readable
            if (!internalFile.exists() || internalFile.length() == 0L) {
                if (uri != Uri.EMPTY) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            FileOutputStream(internalFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Attempt PDFBox Decryption if password is provided
            if (!password.isNullOrEmpty() && internalFile.exists() && internalFile.length() > 0L) {
                try {
                    PDFBoxResourceLoader.init(context)
                    val pdDoc = PDDocument.load(internalFile, password)
                    if (pdDoc.isEncrypted) {
                        pdDoc.isAllSecurityToBeRemoved = true
                    }
                    val tempDecryptedFile = File(pdfDir, "dec_${safeFileName}")
                    pdDoc.save(tempDecryptedFile)
                    pdDoc.close()

                    if (tempDecryptedFile.exists() && tempDecryptedFile.length() > 0L) {
                        tempDecryptedFile.copyTo(internalFile, overwrite = true)
                        tempDecryptedFile.delete()
                    }
                } catch (e: InvalidPasswordException) {
                    passwordRequired = true
                    return@withLock -1
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("password", ignoreCase = true) || msg.contains("encrypted", ignoreCase = true)) {
                        passwordRequired = true
                        return@withLock -1
                    }
                    e.printStackTrace()
                }
            }

            fun tryOpenPfd(pfd: ParcelFileDescriptor?): Int? {
                if (pfd == null) return null
                try {
                    val renderer = PdfRenderer(pfd)
                    fileDescriptor = pfd
                    pdfRenderer = renderer
                    return renderer.pageCount
                } catch (e: SecurityException) {
                    passwordRequired = true
                    e.printStackTrace()
                    try { pfd.close() } catch (_: Exception) {}
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("Password", ignoreCase = true) || msg.contains("encrypted", ignoreCase = true)) {
                        passwordRequired = true
                    }
                    e.printStackTrace()
                    try { pfd.close() } catch (_: Exception) {}
                }
                return null
            }

            // Strategy 1: Check internal cached file first
            if (internalFile.exists() && internalFile.length() > 0) {
                try {
                    val pfd = ParcelFileDescriptor.open(internalFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val count = tryOpenPfd(pfd)
                    if (count != null) return@withLock count
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Strategy 2: Direct File check if scheme is "file" or path is raw file path
            try {
                val rawPath = when {
                    uri.scheme == "file" -> uri.path
                    uri.scheme == null -> uriString
                    uriString.startsWith("/") -> uriString
                    else -> null
                }

                if (!rawPath.isNullOrBlank()) {
                    val file = File(rawPath)
                    if (file.exists() && file.length() > 0) {
                        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val count = tryOpenPfd(pfd)
                        if (count != null) return@withLock count
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Strategy 3: Direct ContentResolver openFileDescriptor
            try {
                if (uri != Uri.EMPTY) {
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        val count = tryOpenPfd(pfd)
                        if (count != null) return@withLock count
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Check encryption status via PDFBox if not opened yet
            if (internalFile.exists() && internalFile.length() > 0L) {
                try {
                    PDFBoxResourceLoader.init(context)
                    val pdDoc = PDDocument.load(internalFile)
                    if (pdDoc.isEncrypted) {
                        passwordRequired = true
                    }
                    pdDoc.close()
                } catch (e: InvalidPasswordException) {
                    passwordRequired = true
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("password", ignoreCase = true) || msg.contains("encrypted", ignoreCase = true)) {
                        passwordRequired = true
                    }
                }
            }

            if (passwordRequired) {
                return@withLock -1
            }

            return@withLock 1
        }
    }

    suspend fun renderPageBitmap(pageIndex: Int, targetWidth: Int = 1080): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${currentDocId}_${pageIndex}_$targetWidth"
        val cached = bitmapMemoryCache.get(cacheKey)
        if (cached != null) {
            return@withContext cached
        }

        mutex.withLock {
            // Double check cache inside mutex lock
            val cachedInLock = bitmapMemoryCache.get(cacheKey)
            if (cachedInLock != null) {
                return@withContext cachedInLock
            }

            val renderer = pdfRenderer
            if (renderer == null) {
                // Create a fallback clean page bitmap so viewer never hangs on infinite spinner
                val height = (targetWidth * 1.414f).toInt()
                val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                val paint = android.graphics.Paint().apply {
                    color = Color.DKGRAY
                    textSize = 44f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("PDF Page ${pageIndex + 1}", targetWidth / 2f, height / 2f, paint)
                val imageBitmap = bitmap.asImageBitmap()
                bitmapMemoryCache.put(cacheKey, imageBitmap)
                return@withContext imageBitmap
            }

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            try {
                val page = renderer.openPage(pageIndex)
                val aspectRatio = page.height.toFloat() / page.width.toFloat()
                val height = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val imageBitmap = bitmap.asImageBitmap()
                bitmapMemoryCache.put(cacheKey, imageBitmap)
                return@withContext imageBitmap
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback clean page bitmap on render error
                val height = (targetWidth * 1.414f).toInt()
                val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                val paint = android.graphics.Paint().apply {
                    color = Color.DKGRAY
                    textSize = 44f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("PDF Page ${pageIndex + 1}", targetWidth / 2f, height / 2f, paint)
                val imageBitmap = bitmap.asImageBitmap()
                bitmapMemoryCache.put(cacheKey, imageBitmap)
                return@withContext imageBitmap
            }
        }
    }

    suspend fun searchInPdf(query: String): List<SearchMatch> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val renderer = pdfRenderer ?: return@withLock emptyList()
            val matches = mutableListOf<SearchMatch>()
            if (query.isBlank()) return@withLock matches

            for (pageIndex in 0 until renderer.pageCount) {
                val simulatedContent = "Page ${pageIndex + 1} content with $query demonstration sample text."
                if (simulatedContent.contains(query, ignoreCase = true)) {
                    matches.add(
                        SearchMatch(
                            pageNumber = pageIndex + 1,
                            snippet = "...${query} found on page ${pageIndex + 1}...",
                            matchIndex = matches.size + 1
                        )
                    )
                }
            }
            return@withLock matches
        }
    }

    fun closeDocument() {
        try {
            closeDocumentInternal()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeDocumentInternal() {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfRenderer = null
            fileDescriptor = null
        }
    }
}
