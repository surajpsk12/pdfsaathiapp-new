package com.pdfsaathi.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.ui.components.EmptyStateWidget
import com.pdfsaathi.app.ui.components.PdfDocumentCardRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenReader: (PdfDocument) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    var documentToDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<PdfDocument?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Search PDFs") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by document title...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            if (results.isEmpty() && query.isNotEmpty()) {
                EmptyStateWidget(
                    title = "No Matches Found",
                    subtitle = "No PDF files match '$query'. Try another keyword."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results) { doc ->
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

        // Delete Confirmation Dialog
        documentToDelete?.let { doc ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { documentToDelete = null },
                title = { Text("Delete Document?") },
                text = { Text("Are you sure you want to remove '${doc.name}' from PDF Saathi library?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.deleteDocument(doc.id)
                            documentToDelete = null
                        }
                    ) {
                        Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { documentToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
