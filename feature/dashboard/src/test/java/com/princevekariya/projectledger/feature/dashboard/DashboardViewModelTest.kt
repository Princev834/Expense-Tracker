package com.princevekariya.projectledger.feature.dashboard

import com.princevekariya.projectledger.core.model.AppDistribution
import com.princevekariya.projectledger.core.model.AppVariantConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardViewModelTest {
    @Test
    fun `description actions update the observable UI state`() {
        val viewModel = DashboardViewModel(initialState = createInitialState())

        viewModel.onAction(
            DashboardAction.DescriptionChanged(value = "Evening tea"),
        )

        assertEquals("Evening tea", viewModel.uiState.value.description)
    }

    @Test
    fun `amount actions update the observable UI state`() {
        val viewModel = DashboardViewModel(initialState = createInitialState())

        viewModel.onAction(
            DashboardAction.AmountChanged(value = "25.50"),
        )

        assertEquals("25.50", viewModel.uiState.value.amount)
    }

    @Test
    fun `expense action emits a one-off user message`() {
        val viewModel = DashboardViewModel(initialState = createInitialState())

        viewModel.onAction(DashboardAction.AddExpenseClicked)

        val message = viewModel.uiState.value.userMessage
        assertTrue(message?.text?.contains("Expense entry") == true)
    }

    @Test
    fun `only the matching message identifier is consumed`() {
        val viewModel = DashboardViewModel(initialState = createInitialState())
        viewModel.onAction(DashboardAction.AddIncomeClicked)
        val message = requireNotNull(viewModel.uiState.value.userMessage)

        viewModel.onAction(DashboardAction.MessageShown(id = message.id + 1L))
        assertEquals(message, viewModel.uiState.value.userMessage)

        viewModel.onAction(DashboardAction.MessageShown(id = message.id))
        assertNull(viewModel.uiState.value.userMessage)
    }

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
}
