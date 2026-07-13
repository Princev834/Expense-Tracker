package com.princevekariya.projectledger.feature.dashboard

import com.princevekariya.projectledger.core.common.AppLogLevel
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.model.AppDistribution
import com.princevekariya.projectledger.core.model.AppVariantConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardViewModelTest {
    @Test
    fun `description actions update the observable UI state`() {
        val viewModel = createViewModel()

        viewModel.onAction(
            DashboardAction.DescriptionChanged(value = "Evening tea"),
        )

        assertEquals("Evening tea", viewModel.uiState.value.description)
    }

    @Test
    fun `amount actions update the observable UI state`() {
        val viewModel = createViewModel()

        viewModel.onAction(
            DashboardAction.AmountChanged(value = "25.50"),
        )

        assertEquals("25.50", viewModel.uiState.value.amount)
    }

    @Test
    fun `valid expense action emits safe confirmation and info log`() {
        val logger = RecordingAppLogger()
        val viewModel = createViewModel(logger = logger)

        viewModel.onAction(DashboardAction.AddExpenseClicked)

        assertEquals("Expense draft is ready.", viewModel.uiState.value.userMessage?.text)
        assertEquals(AppLogLevel.INFO, logger.entries.single().level)
        assertFalse(logger.entries.single().message.contains("120"))
        assertFalse(logger.entries.single().message.contains("Lunch"))
    }

    @Test
    fun `blank description is rejected without logging entered data`() {
        val logger = RecordingAppLogger()
        val viewModel = createViewModel(logger = logger)
        viewModel.onAction(DashboardAction.DescriptionChanged(value = ""))

        viewModel.onAction(DashboardAction.AddExpenseClicked)

        assertEquals(
            "Enter a description before continuing.",
            viewModel.uiState.value.userMessage?.text,
        )
        assertEquals(AppLogLevel.WARNING, logger.entries.single().level)
    }

    @Test
    fun `invalid amount is rejected with user readable guidance`() {
        val logger = RecordingAppLogger()
        val viewModel = createViewModel(logger = logger)
        viewModel.onAction(DashboardAction.AmountChanged(value = "zero"))

        viewModel.onAction(DashboardAction.AddIncomeClicked)

        assertEquals(
            "Enter a valid amount greater than zero.",
            viewModel.uiState.value.userMessage?.text,
        )
        assertEquals(AppLogLevel.WARNING, logger.entries.single().level)
    }

    @Test
    fun `amount with more than two decimal places is rejected`() {
        val logger = RecordingAppLogger()
        val viewModel = createViewModel(logger = logger)
        viewModel.onAction(DashboardAction.AmountChanged(value = "10.999"))

        viewModel.onAction(DashboardAction.AddExpenseClicked)

        assertEquals(
            "Enter a valid amount greater than zero.",
            viewModel.uiState.value.userMessage?.text,
        )
        assertEquals(AppLogLevel.WARNING, logger.entries.single().level)
    }

    @Test
    fun `only the matching message identifier is consumed`() {
        val viewModel = createViewModel()
        viewModel.onAction(DashboardAction.AddIncomeClicked)
        val message = requireNotNull(viewModel.uiState.value.userMessage)

        viewModel.onAction(DashboardAction.MessageShown(id = message.id + 1L))
        assertEquals(message, viewModel.uiState.value.userMessage)

        viewModel.onAction(DashboardAction.MessageShown(id = message.id))
        assertNull(viewModel.uiState.value.userMessage)
    }

    private fun createViewModel(logger: AppLogger = RecordingAppLogger()): DashboardViewModel = DashboardViewModel(
        initialState = createInitialState(),
        appLogger = logger,
    )

    private fun createInitialState(): DashboardUiState = DashboardUiState(
        variant = AppVariantConfiguration(
            distribution = AppDistribution.PERSONAL,
            displayName = "Personal APK",
            supportsSmsAutomation = true,
            isPlayStoreSafe = false,
        ),
        platformDescription = "Android test device",
        moduleCount = 8,
    )

    private class RecordingAppLogger : AppLogger {
        val entries = mutableListOf<LogEntry>()

        override fun log(level: AppLogLevel, event: String, message: String, throwable: Throwable?) {
            entries += LogEntry(
                level = level,
                event = event,
                message = message,
            )
        }
    }

    private data class LogEntry(
        val level: AppLogLevel,
        val event: String,
        val message: String,
    )
}
