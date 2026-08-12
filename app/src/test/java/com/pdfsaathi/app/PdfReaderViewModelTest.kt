package com.pdfsaathi.app

import com.pdfsaathi.app.domain.model.ReadingTheme
import com.pdfsaathi.app.ui.reader.PdfReaderViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class PdfReaderViewModelTest {

    @Test
    fun testThemeChange_updatesThemeState() {
        val viewModel = PdfReaderViewModel(
            mock(), mock(), mock(), mock(), mock(), mock()
        )

        assertEquals(ReadingTheme.LIGHT, viewModel.readingTheme.value)
        viewModel.changeTheme(ReadingTheme.DARK)
        assertEquals(ReadingTheme.DARK, viewModel.readingTheme.value)
        viewModel.changeTheme(ReadingTheme.SEPIA)
        assertEquals(ReadingTheme.SEPIA, viewModel.readingTheme.value)
    }

    @Test
    fun testToggleControlsVisibility() {
        val viewModel = PdfReaderViewModel(
            mock(), mock(), mock(), mock(), mock(), mock()
        )

        assertEquals(true, viewModel.isControlsVisible.value)
        viewModel.toggleControlsVisibility()
        assertEquals(false, viewModel.isControlsVisible.value)
    }
}
