package com.pdfsaathi.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.pdfsaathi.app.data.repository.SettingsRepository
import com.pdfsaathi.app.domain.model.ReadingTheme
import com.pdfsaathi.app.ui.navigation.PdfNavGraph
import com.pdfsaathi.app.ui.theme.PDFSaathiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val externalPdfUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            val appTheme by settingsRepository.appTheme.collectAsState()
            val isDarkTheme = appTheme == ReadingTheme.DARK

            PDFSaathiTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfNavGraph(externalPdfUri = externalPdfUri.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_VIEW == action && type == "application/pdf") {
            intent.data?.let { uri ->
                externalPdfUri.value = uri
            }
        } else if (Intent.ACTION_SEND == action && type == "application/pdf") {
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                externalPdfUri.value = uri
            }
        }
    }
}
