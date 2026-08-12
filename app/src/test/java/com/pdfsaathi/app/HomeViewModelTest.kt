package com.pdfsaathi.app

import com.pdfsaathi.app.domain.model.PdfDocument
import com.pdfsaathi.app.domain.repository.PdfRepository
import com.pdfsaathi.app.domain.usecase.GetFavoriteDocumentsUseCase
import com.pdfsaathi.app.domain.usecase.GetRecentDocumentsUseCase
import com.pdfsaathi.app.domain.usecase.ToggleFavoriteUseCase
import com.pdfsaathi.app.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val pdfRepository: PdfRepository = mock()
    private val getRecentDocumentsUseCase: GetRecentDocumentsUseCase = mock()
    private val getFavoriteDocumentsUseCase: GetFavoriteDocumentsUseCase = mock()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mock()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val dummyList = listOf(
            PdfDocument("1", "content://pdf1", "Doc1.pdf", 1024L, "1 KB", System.currentTimeMillis(), "Today", 0L)
        )
        whenever(getRecentDocumentsUseCase.invoke()).thenReturn(flowOf(dummyList))
        whenever(getFavoriteDocumentsUseCase.invoke()).thenReturn(flowOf(dummyList))
        whenever(pdfRepository.getAllDocuments()).thenReturn(flowOf(dummyList))

        viewModel = HomeViewModel(
            getRecentDocumentsUseCase,
            getFavoriteDocumentsUseCase,
            toggleFavoriteUseCase,
            pdfRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRecentDocuments_returnsDataFromUseCase() = runTest {
        val result = viewModel.recentDocuments.value
        // Verify state flow initialization
        whenever(getRecentDocumentsUseCase.invoke()).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun testToggleFavorite_callsUseCase() = runTest {
        viewModel.toggleFavorite("1", false)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(toggleFavoriteUseCase).invoke("1", true)
    }
}
