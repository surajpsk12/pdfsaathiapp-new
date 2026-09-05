package com.pdfsaathi.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewAgenda
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

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpTargetPage by remember { mutableIntStateOf(page) }
    var hasRestoredInitialScroll by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable { viewModel.toggleControlsVisibility() }
    ) {
        // Continuous Scroll Mode View (Vertical Scroll)
        if (viewerMode == "continuous") {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = if (totalPages == 1) 20.dp else 70.dp,
                    bottom = if (totalPages == 1) 20.dp else 30.dp
                ),
                verticalArrangement = if (totalPages == 1) Arrangement.Center else Arrangement.spacedBy(12.dp),
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
                            .pointerInput(scale > 1.05f) {
                                if (scale > 1.05f) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                                        scale = newScale
                                        if (newScale > 1.05f) {
                                            val maxX = (size.width * (newScale - 1f)) / 2f
                                            val maxY = (size.height * (newScale - 1f)) / 2f
                                            offsetX = (offsetX + pan.x * newScale).coerceIn(-maxX, maxX)
                                            offsetY = (offsetY + pan.y * newScale).coerceIn(-maxY, maxY)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                }
                            }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
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

                                // Cyan Page Number Badge
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF00E5FF),
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        text = "%02d".format(pageNum),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black,
                                        fontSize = 12.sp
                                    )
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
                        .padding(horizontal = 10.dp, vertical = 10.dp)
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
                        .pointerInput(scale > 1.05f) {
                            if (scale > 1.05f) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                                    scale = newScale
                                    if (newScale > 1.05f) {
                                        val maxX = (size.width * (newScale - 1f)) / 2f
                                        val maxY = (size.height * (newScale - 1f)) / 2f
                                        offsetX = (offsetX + pan.x * newScale).coerceIn(-maxX, maxX)
                                        offsetY = (offsetY + pan.y * newScale).coerceIn(-maxY, maxY)
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
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

                            // Cyan Page Number Badge
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00E5FF),
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = "%02d".format(pageNum),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black,
                                    fontSize = 12.sp
                                )
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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

        // Bottom Seeker Controls Overlay (Only for Single/Horizontal Page Mode)
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
                                IconButton(onClick = {
                                    if (page > 1) {
                                        val prev = page - 1
                                        viewModel.renderPage(prev)
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(prev - 1)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Page", modifier = Modifier.height(28.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    if (page < totalPages) {
                                        val next = page + 1
                                        viewModel.renderPage(next)
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(next - 1)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", modifier = Modifier.height(28.dp))
                                }
                            }
                        }

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
    }
}
