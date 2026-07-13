package com.princevekariya.projectledger.feature.dashboard

import androidx.lifecycle.ViewModel
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.common.UiMessage
import com.princevekariya.projectledger.core.common.info
import com.princevekariya.projectledger.core.common.warning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    initialState: DashboardUiState,
    private val appLogger: AppLogger,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState)
    private var nextMessageId: Long = initialState.userMessage?.id ?: 0L

    val uiState: StateFlow<DashboardUiState> = mutableUiState.asStateFlow()

    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.DescriptionChanged -> updateDescription(action.value)
            is DashboardAction.AmountChanged -> updateAmount(action.value)
            DashboardAction.AddExpenseClicked -> validateDraft(transactionType = "expense")
            DashboardAction.AddIncomeClicked -> validateDraft(transactionType = "income")
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

    private fun validateDraft(transactionType: String) {
        val state = mutableUiState.value
        if (state.description.isBlank()) {
            appLogger.warning(
                event = "transaction_draft_rejected",
                message = "A transaction draft was missing its description.",
            )
            showMessage(text = "Enter a description before continuing.")
            return
        }

        val amount = state.amount.toBigDecimalOrNull()
        if (amount == null || amount.signum() <= 0) {
            appLogger.warning(
                event = "transaction_draft_rejected",
                message = "A transaction draft contained an invalid amount.",
            )
            showMessage(text = "Enter a valid amount greater than zero.")
            return
        }

        appLogger.info(
            event = "transaction_draft_validated",
            message = "A $transactionType draft passed local validation.",
        )
        showMessage(
            text = "${transactionType.replaceFirstChar { character -> character.uppercase() }} draft is ready.",
        )
    }

    private fun retryContent() {
        mutableUiState.value = mutableUiState.value.copy(
            loadState = UiLoadState.Content,
        )
        appLogger.info(
            event = "dashboard_retry_completed",
            message = "Dashboard content returned to the content state.",
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
