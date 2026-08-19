package com.pdfsaathi.app.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.ui.components.EmptyStateWidget
import com.pdfsaathi.app.ui.components.PdfDocumentCardRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onOpenReader: (PdfDocument) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    var documentToDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<PdfDocument?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Starred PDFs",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                }
            )
        }
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            EmptyStateWidget(
                title = "No Favorites Yet",
                subtitle = "Mark PDFs as favorites to easily find and access them offline.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favorites) { doc ->
                    PdfDocumentCardRow(
                        document = doc,
                        onClick = { onOpenReader(doc) },
                        onToggleFavorite = { viewModel.removeFavorite(doc.id) },
                        onDelete = { documentToDelete = doc }
                    )
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
