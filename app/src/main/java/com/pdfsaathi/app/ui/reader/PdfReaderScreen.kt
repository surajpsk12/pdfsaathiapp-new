package com.pdfsaathi.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfsaathi.app.domain.model.ReadingTheme
import com.pdfsaathi.app.ui.components.cleanDocumentTitle
import com.pdfsaathi.app.ui.theme.SepiaSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PdfReaderScreen(
    documentId: String,
    onBackClick: () -> Unit,
    viewModel: PdfReaderViewModel = hiltViewModel()
) {
    val document by viewModel.currentDocument.collectAsState()
    val page by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val initialRestoredPage by viewModel.initialRestoredPage.collectAsState()
    val theme by viewModel.readingTheme.collectAsState()
    val viewerMode by viewModel.viewerMode.collectAsState()
    val showControls by viewModel.isControlsVisible.collectAsState()
    val bitmap by viewModel.currentPageBitmap.collectAsState()
    val pageBitmaps by viewModel.pageBitmaps.collectAsState()
    val isPasswordRequired by viewModel.isPasswordRequired.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    val isTextSelectionMode by viewModel.isTextSelectionMode.collectAsState()
    val currentExtractedText by viewModel.currentExtractedText.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val isSharing by viewModel.isSharing.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpTargetPage by remember { mutableIntStateOf(page) }
    var hasRestoredInitialScroll by remember { mutableStateOf(false) }
    var inputPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = (page - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
        pageCount = { totalPages.coerceAtLeast(1) }
    )

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    // Reset zoom when page or view mode changes
    LaunchedEffect(pagerState.currentPage, viewerMode) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // Restore last read page position on document load
    LaunchedEffect(initialRestoredPage) {
        initialRestoredPage?.let { restoredPage ->
            if (!hasRestoredInitialScroll) {
                hasRestoredInitialScroll = true
                val targetIndex = (restoredPage - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                coroutineScope.launch {
                    listState.scrollToItem(targetIndex)
                    pagerState.scrollToPage(targetIndex)
                }
            }
        }
    }

    // Sync lazy column scroll position with current page
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            if (viewerMode == "continuous") {
                viewModel.renderPage(index + 1)
            }
        }
    }

    // Sync horizontal pager position with view model page
    LaunchedEffect(pagerState.currentPage) {
        if (viewerMode == "single" && hasRestoredInitialScroll) {
            val targetPage = pagerState.currentPage + 1
            if (page != targetPage) {
                viewModel.renderPage(targetPage)
            }
        }
    }

    // Sync page state when updated externally (e.g. from slider/jump)
    LaunchedEffect(page) {
        val targetIndex = (page - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
        if (pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    val displayTitle = cleanDocumentTitle(document?.name ?: "Document.pdf")

    val backgroundColor = when (theme) {
        ReadingTheme.LIGHT -> Color(0xFFF3F4F6)
        ReadingTheme.DARK -> Color(0xFF121212)
        ReadingTheme.SEPIA -> SepiaSurface
        ReadingTheme.SYSTEM -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        topBar = {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.downloadCurrentDocument { success, message ->
                                android.widget.Toast.makeText(
                                    context,
                                    message,
                                    if (success) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isDownloading
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download PDF to Phone Storage",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.shareCurrentDocument(context) },
                        enabled = !isSharing
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share PDF to any app",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.toggleViewerMode() }) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = "Viewer Mode",
                            tint = if (viewerMode == "continuous") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        val nextTheme = when (theme) {
                            ReadingTheme.LIGHT -> ReadingTheme.DARK
                            ReadingTheme.DARK -> ReadingTheme.SEPIA
                            else -> ReadingTheme.LIGHT
                        }
                        viewModel.changeTheme(nextTheme)
                    }) {
                        Icon(Icons.Default.Palette, contentDescription = "Change Reader Theme")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
        ) {
            // Continuous Scroll Mode View (Vertical Scroll)
            if (viewerMode == "continuous") {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 10.dp,
                    bottom = 70.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = scale <= 1.05f
            ) {
                items(totalPages) { index ->
                    val pageNum = index + 1

                    LaunchedEffect(pageNum) {
                        viewModel.loadPageBitmapForContinuous(pageNum)
                    }

                    val pageImg = pageBitmaps[pageNum] ?: if (pageNum == page) bitmap else null

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            shadowElevation = 3.dp
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
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
                }
            }

            // Compact Right-Side Dynamic Fast Scroll Knob
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 80.dp)
                    .width(60.dp)
                    .pointerInput(totalPages) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                val fraction = (offset.y / size.height).coerceIn(0f, 1f)
                                val targetPage = (fraction * (totalPages - 1)).toInt() + 1
                                viewModel.renderPage(targetPage)
                                coroutineScope.launch {
                                    listState.scrollToItem((targetPage - 1).coerceAtLeast(0))
                                }
                            },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val fraction = (change.position.y / size.height).coerceIn(0f, 1f)
                                val targetPage = (fraction * (totalPages - 1)).toInt() + 1
                                viewModel.renderPage(targetPage)
                                coroutineScope.launch {
                                    listState.scrollToItem((targetPage - 1).coerceAtLeast(0))
                                }
                            }
                        )
                    }
            ) {
                val usableHeight = this.maxHeight - 50.dp
                val scrollProgress = ((listState.firstVisibleItemIndex.toFloat() + (listState.firstVisibleItemScrollOffset.toFloat() / 800f)) / (totalPages - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                val knobYOffset = usableHeight * scrollProgress

                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier
                            .offset(y = knobYOffset)
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp)
                            .clickable {
                                jumpTargetPage = page
                                showJumpDialog = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Fast Scroll",
                                tint = Color.White,
                                modifier = Modifier.height(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$page/$totalPages",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // Horizontal Pager Mode View (Slide transition effect on swipe)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = scale <= 1.05f
            ) { pageIdx ->
                val pageNum = pageIdx + 1

                LaunchedEffect(pageNum) {
                    viewModel.loadPageBitmapForContinuous(pageNum)
                }

                val pageImg = pageBitmaps[pageNum] ?: if (pageNum == page) bitmap else null

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, bottom = 80.dp, start = 10.dp, end = 10.dp)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.5f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            )
                        }
                        .pointerInput(scale) {
                            awaitEachGesture {
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size > 1 || scale > 1.05f) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (zoomChange != 1f || panChange != Offset.Zero) {
                                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                            scale = newScale
                                            if (newScale > 1.05f) {
                                                val maxX = (size.width * (newScale - 1f)) / 2f
                                                val maxY = (size.height * (newScale - 1f)) / 2f
                                                offsetX = (offsetX + panChange.x * newScale).coerceIn(-maxX, maxX)
                                                offsetY = (offsetY + panChange.y * newScale).coerceIn(-maxY, maxY)
                                            } else {
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                                clip = true
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (pageImg != null) {
                                Image(
                                    bitmap = pageImg,
                                    contentDescription = "PDF Page $pageNum",
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
                                            text = "Rendering Page $pageNum...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Reset Zoom Button Pill (Visible when scale > 1.05f)
        AnimatedVisibility(
            visible = scale > 1.05f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) {
            Surface(
                modifier = Modifier.clickable {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Zoom",
                        tint = Color.White,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(scale * 100).toInt()}% • Tap to Reset",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Bottom Seeker Controls Overlay (Only for Single/Horizontal Page Mode)
        if (viewerMode == "single") {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Surface(
                    modifier = Modifier.wrapContentWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1E1E1E).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF818CF8).copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "$page / $totalPages",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF818CF8)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (page > 1) {
                                            val prev = page - 1
                                            viewModel.renderPage(prev)
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(prev - 1)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Prev Page",
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        if (page < totalPages) {
                                            val next = page + 1
                                            viewModel.renderPage(next)
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(next - 1)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next Page",
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = page.toFloat(),
                            onValueChange = { target ->
                                val newPage = target.toInt().coerceIn(1, totalPages)
                                viewModel.renderPage(newPage)
                                coroutineScope.launch {
                                    pagerState.scrollToPage(newPage - 1)
                                }
                            },
                            valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF818CF8),
                                activeTrackColor = Color(0xFF818CF8),
                                inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )
                    }
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
                            viewModel.renderPage(jumpTargetPage)
                            coroutineScope.launch {
                                if (viewerMode == "continuous") {
                                    listState.scrollToItem((jumpTargetPage - 1).coerceAtLeast(0))
                                } else {
                                    pagerState.scrollToPage((jumpTargetPage - 1).coerceAtLeast(0))
                                }
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

        // Password Protected PDF Unlock Dialog
        if (isPasswordRequired) {
            AlertDialog(
                onDismissRequest = { /* Require user action */ },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(32.dp)
                    )
                },
                title = { Text("Password Protected PDF") },
                text = {
                    Column {
                        Text(
                            text = "This document is encrypted. Please enter the password to open.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = inputPassword,
                            onValueChange = { inputPassword = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Password Visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        passwordError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (inputPassword.isNotBlank()) {
                                viewModel.unlockDocumentWithPassword(inputPassword)
                            }
                        }
                    ) {
                        Text("Unlock", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onBackClick) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Text Selection & Copy Modal Overlay
        if (isTextSelectionMode) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleTextSelectionMode() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Text Selection (Page $page)")
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Tap & select text, copy page text, define words, or share snippets:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            SelectionContainer(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = if (currentExtractedText.isNotBlank()) currentExtractedText else "Extracting text from page $page...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (currentExtractedText.isNotBlank()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("PDF Page $page Text", currentExtractedText)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Page text copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Copy All", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                if (currentExtractedText.isNotBlank()) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH).apply {
                                        putExtra(android.app.SearchManager.QUERY, currentExtractedText.take(150))
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                }
                            }
                        ) {
                            Text("Search")
                        }
                        TextButton(
                            onClick = {
                                if (currentExtractedText.isNotBlank()) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, currentExtractedText)
                                    }
                                    try { context.startActivity(android.content.Intent.createChooser(intent, "Share Snippet")) } catch (_: Exception) {}
                                }
                            }
                        ) {
                            Text("Share")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.toggleTextSelectionMode() }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
