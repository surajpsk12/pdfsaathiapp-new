package com.pdfsaathi.app.ui.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.ui.components.EmptyStateWidget
import com.pdfsaathi.app.ui.components.PdfDocumentCardRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllDocumentsScreen(
    onOpenReader: (PdfDocument) -> Unit,
    viewModel: AllDocumentsViewModel = hiltViewModel()
) {
    val documents by viewModel.documents.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Documents (${documents.size})") },
                actions = {
                    IconButton(onClick = { viewModel.scanStorage() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan Storage")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
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
                            text = { Text("Sort by Date") },
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
            )
        }
    ) { innerPadding ->
        if (documents.isEmpty()) {
            EmptyStateWidget(
                title = "No Documents Found",
                subtitle = "Import PDFs from storage to view them listed here.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(documents) { doc ->
                    PdfDocumentCardRow(
                        document = doc,
                        onClick = { onOpenReader(doc) },
                        onToggleFavorite = { viewModel.toggleFavorite(doc.id, doc.isFavorite) }
                    )
                }
            }
        }
    }
}
