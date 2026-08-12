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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.launch

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
    val viewerMode by viewModel.viewerMode.collectAsState()
    val showControls by viewModel.isControlsVisible.collectAsState()
    val bitmap by viewModel.currentPageBitmap.collectAsState()
    val pageBitmaps by viewModel.pageBitmaps.collectAsState()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpTargetPage by remember { mutableStateOf(page) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    // Sync lazy column scroll position with current page
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            if (viewerMode == "continuous") {
                viewModel.renderPage(index + 1)
            }
        }
    }

    val displayTitle = cleanDocumentTitle(document?.name ?: "Document.pdf")

    val backgroundColor = when (theme) {
        ReadingTheme.LIGHT -> Color(0xFFF3F4F6)
        ReadingTheme.DARK -> Color(0xFF121212)
        ReadingTheme.SEPIA -> SepiaSurface
        ReadingTheme.SYSTEM -> MaterialTheme.colorScheme.background
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable { viewModel.toggleControlsVisibility() }
    ) {
        // Continuous Scroll Mode View (Professional Margins & Responsive Full Width)
        if (viewerMode == "continuous") {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 70.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(totalPages) { index ->
                    val pageNum = index + 1

                    LaunchedEffect(pageNum) {
                        viewModel.loadPageBitmapForContinuous(pageNum)
                    }

                    val pageImg = pageBitmaps[pageNum]

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 3.dp
                    ) {
                        if (pageImg != null) {
                            Image(
                                bitmap = pageImg,
                                contentDescription = "Page $pageNum",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(550.dp)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Loading Page $pageNum of $totalPages...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right-Side Fast Scroll Jump Knob / Page Indicator Button
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .clickable {
                        jumpTargetPage = page
                        showJumpDialog = true
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Fast Scroll",
                        tint = Color.White,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$page / $totalPages",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        } else {
            // Single Page Mode View (Responsive Full Canvas with Professional Padding)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
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
                    color = Color.White,
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Rendering $displayTitle...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
                            }
                        }
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
                    IconButton(onClick = { viewModel.toggleViewerMode() }) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = "Viewer Mode",
                            tint = if (viewerMode == "continuous") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

        // Bottom Seeker Controls Overlay (Only for Single Page Mode)
        if (viewerMode == "single") {
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

        // Interactive Page Jump Dialog
        if (showJumpDialog) {
            AlertDialog(
                onDismissRequest = { showJumpDialog = false },
                title = { Text("Jump to Page") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Page $jumpTargetPage of $totalPages",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = jumpTargetPage.toFloat(),
                            onValueChange = { jumpTargetPage = it.toInt() },
                            valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showJumpDialog = false
                            coroutineScope.launch {
                                listState.scrollToItem((jumpTargetPage - 1).coerceAtLeast(0))
                            }
                        }
                    ) {
                        Text("Jump", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
