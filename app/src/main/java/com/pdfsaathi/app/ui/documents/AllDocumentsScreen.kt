package com.pdfsaathi.app.ui.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.ui.components.EmptyStateWidget
import com.pdfsaathi.app.ui.components.PdfDocumentCardRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllDocumentsScreen(
    onOpenReader: (PdfDocument) -> Unit,
    onBackClick: (() -> Unit)? = null,
    viewModel: AllDocumentsViewModel = hiltViewModel()
) {
    val documents by viewModel.documents.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<PdfDocument?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "File Explorer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanStorage() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan Storage")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 1000.dp)
            ) {
                // Filter Toolbar (Sort Dropdown + View Mode Toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort Dropdown Button
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.clickable { showSortMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (currentSort) {
                                        SortOption.NAME -> "Sort by Name"
                                        SortOption.DATE -> "Sort by Recent"
                                        SortOption.SIZE -> "Sort by Size"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Name") },
                                onClick = {
                                    viewModel.sortOption.value = SortOption.NAME
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Recent") },
                                onClick = {
                                    viewModel.sortOption.value = SortOption.DATE
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Size") },
                                onClick = {
                                    viewModel.sortOption.value = SortOption.SIZE
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // View Mode Toggle (List vs Grid)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            IconButton(
                                onClick = { isGridView = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.List,
                                    contentDescription = "List View",
                                    tint = if (!isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { isGridView = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = "Grid View",
                                    tint = if (isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Documents Content View
                if (documents.isEmpty()) {
                    EmptyStateWidget(
                        title = "No Documents Found",
                        subtitle = "PDF Saathi is scanning internal & external storage for all .pdf files."
                    )
                } else if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    items(documents) { doc ->
                        PdfDocumentCardRow(
                            document = doc,
                            onClick = { onOpenReader(doc) },
                            onToggleFavorite = { viewModel.toggleFavorite(doc.id, doc.isFavorite) },
                            onDelete = { documentToDelete = doc }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(documents) { doc ->
                        PdfDocumentCardRow(
                            document = doc,
                            onClick = { onOpenReader(doc) },
                            onToggleFavorite = { viewModel.toggleFavorite(doc.id, doc.isFavorite) },
                            onDelete = { documentToDelete = doc }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
        documentToDelete?.let { doc ->
            AlertDialog(
                onDismissRequest = { documentToDelete = null },
                title = { Text("Delete Document?") },
                text = { Text("Are you sure you want to remove '${doc.name}' from PDF Saathi library?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteDocument(doc.id)
                            documentToDelete = null
                        }
                    ) {
                        Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { documentToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
