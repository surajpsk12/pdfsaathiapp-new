package com.pdfsaathi.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfsaathi.app.domain.model.ReadingTheme
import com.pdfsaathi.app.ui.components.cleanDocumentTitle
import com.pdfsaathi.app.ui.theme.SepiaSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    documentId: String,
    onBackClick: () -> Unit,
    viewModel: PdfReaderViewModel = hiltViewModel()
) {
    val document by viewModel.currentDocument.collectAsState()
    val page by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val theme by viewModel.readingTheme.collectAsState()
    val showControls by viewModel.isControlsVisible.collectAsState()
    val bitmap by viewModel.currentPageBitmap.collectAsState()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    val displayTitle = cleanDocumentTitle(document?.name ?: "Document.pdf")

    val backgroundColor = when (theme) {
        ReadingTheme.LIGHT -> Color(0xFFF3F4F6)
        ReadingTheme.DARK -> Color(0xFF121212)
        ReadingTheme.SEPIA -> SepiaSurface
        ReadingTheme.SYSTEM -> MaterialTheme.colorScheme.background
    }

    val paperColor = when (theme) {
        ReadingTheme.LIGHT -> Color.White
        ReadingTheme.DARK -> Color(0xFF1E1E1E)
        ReadingTheme.SEPIA -> Color(0xFFFBF0D9)
        ReadingTheme.SYSTEM -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable { viewModel.toggleControlsVisibility() }
    ) {
        // PDF Canvas Viewport (Full width edge-to-edge reading space)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 50.dp, horizontal = 4.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                shape = RoundedCornerShape(8.dp),
                color = paperColor,
                shadowElevation = 4.dp
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = "PDF Page $page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = if (theme == ReadingTheme.DARK) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Page $page of $totalPages",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Pinch to zoom • Double tap to reset scale",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Top Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page $page of $totalPages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (document?.isFavorite == true) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star",
                            tint = if (document?.isFavorite == true) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.addBookmark() }) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Bookmark", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val nextTheme = when (theme) {
                            ReadingTheme.LIGHT -> ReadingTheme.DARK
                            ReadingTheme.DARK -> ReadingTheme.SEPIA
                            else -> ReadingTheme.LIGHT
                        }
                        viewModel.changeTheme(nextTheme)
                    }) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            )
        }

        // Bottom Seeker Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$page / $totalPages",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (page > 1) viewModel.renderPage(page - 1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Page", modifier = Modifier.height(28.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { if (page < totalPages) viewModel.renderPage(page + 1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", modifier = Modifier.height(28.dp))
                            }
                        }
                    }

                    Slider(
                        value = page.toFloat(),
                        onValueChange = { viewModel.renderPage(it.toInt()) },
                        valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
