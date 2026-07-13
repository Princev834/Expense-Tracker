package com.princevekariya.projectledger.feature.dashboard

import androidx.lifecycle.ViewModel
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.common.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    initialState: DashboardUiState,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState)
    private var nextMessageId: Long = initialState.userMessage?.id ?: 0L

    val uiState: StateFlow<DashboardUiState> = mutableUiState.asStateFlow()

    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.DescriptionChanged -> updateDescription(action.value)
            is DashboardAction.AmountChanged -> updateAmount(action.value)
            DashboardAction.AddExpenseClicked -> showMessage(
                text = "Expense entry will be connected in the transaction-entry phase.",
            )
            DashboardAction.AddIncomeClicked -> showMessage(
                text = "Income entry will be connected in the transaction-entry phase.",
            )
            DashboardAction.RetryRequested -> retryContent()
            is DashboardAction.MessageShown -> consumeMessage(action.id)
        }
    }

    private fun updateDescription(value: String) {
        mutableUiState.value = mutableUiState.value.copy(description = value)
    }

    private fun updateAmount(value: String) {
        mutableUiState.value = mutableUiState.value.copy(amount = value)
    }

    private fun retryContent() {
        mutableUiState.value = mutableUiState.value.copy(
            loadState = UiLoadState.Content,
        )
        showMessage(text = "Dashboard content is ready.")
    }

    private fun showMessage(text: String) {
        nextMessageId += 1L
        mutableUiState.value = mutableUiState.value.copy(
            userMessage = UiMessage(
                id = nextMessageId,
                text = text,
            ),
        )
    }

    private fun consumeMessage(id: Long) {
        val currentMessage = mutableUiState.value.userMessage
        if (currentMessage?.id == id) {
            mutableUiState.value = mutableUiState.value.copy(userMessage = null)
        }
    }
}
