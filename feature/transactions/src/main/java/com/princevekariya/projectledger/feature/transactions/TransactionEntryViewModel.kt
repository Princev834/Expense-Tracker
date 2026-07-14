package com.princevekariya.projectledger.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.UiMessage
import com.princevekariya.projectledger.core.common.error
import com.princevekariya.projectledger.core.common.info
import com.princevekariya.projectledger.core.common.toUserFacingError
import com.princevekariya.projectledger.core.common.warning
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.command.ManualTransactionDraft
import com.princevekariya.projectledger.domain.transactions.command.SaveManualTransactionUseCase
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionEntryViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val saveManualTransaction: SaveManualTransactionUseCase,
    private val appLogger: AppLogger,
    initialTransactionType: TransactionType = TransactionType.EXPENSE,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        TransactionEntryUiState(
            transactionType = initialTransactionType.requireEntryType(),
        ),
    )
    private var accountObservationJob: Job? = null
    private var categoryObservationJob: Job? = null
    private var nextMessageId: Long = 0L

    val uiState: StateFlow<TransactionEntryUiState> =
        mutableUiState.asStateFlow()

    init {
        observeAccounts()
        observeCategories(type = initialTransactionType)
    }

    fun onAction(action: TransactionEntryAction) {
        when (action) {
            is TransactionEntryAction.TransactionTypeChanged -> {
                changeTransactionType(type = action.value)
            }
            is TransactionEntryAction.AmountChanged -> {
                updateAmount(value = action.value)
            }
            is TransactionEntryAction.NoteChanged -> {
                updateNote(value = action.value)
            }
            is TransactionEntryAction.AccountSelected -> {
                selectAccount(accountId = action.accountId)
            }
            is TransactionEntryAction.CategorySelected -> {
                selectCategory(categoryId = action.categoryId)
            }
            is TransactionEntryAction.PaymentMethodSelected -> {
                mutableUiState.update { state ->
                    state.copy(paymentMethod = action.value)
                }
            }
            TransactionEntryAction.SaveClicked -> saveTransaction()
            TransactionEntryAction.RetryReferences -> retryReferences()
            is TransactionEntryAction.MessageShown -> {
                consumeMessage(id = action.id)
            }
        }
    }

    private fun observeAccounts() {
        accountObservationJob?.cancel()
        accountObservationJob = viewModelScope.launch {
            accountRepository.observeAll()
                .catch { throwable ->
                    handleReferenceFailure(
                        event = "transaction_accounts_load_failed",
                        throwable = throwable,
                    )
                }
                .collect { accounts ->
                    val activeAccounts = accounts.filterNot { account ->
                        account.isArchived
                    }
                    mutableUiState.update { state ->
                        val selectedId = state.selectedAccountId
                            ?.takeIf { accountId ->
                                activeAccounts.any { account ->
                                    account.id == accountId
                                }
                            }
                            ?: activeAccounts.firstOrNull()?.id

                        state.copy(
                            accounts = activeAccounts,
                            selectedAccountId = selectedId,
                            isLoadingReferences = false,
                        )
                    }
                }
        }
    }

    private fun observeCategories(type: TransactionType) {
        categoryObservationJob?.cancel()
        categoryObservationJob = viewModelScope.launch {
            categoryRepository.observeActive(type = type.toCategoryType())
                .catch { throwable ->
                    handleReferenceFailure(
                        event = "transaction_categories_load_failed",
                        throwable = throwable,
                    )
                }
                .collect { categories ->
                    mutableUiState.update { state ->
                        val selectedId = state.selectedCategoryId
                            ?.takeIf { categoryId ->
                                categories.any { category ->
                                    category.id == categoryId
                                }
                            }
                            ?: categories.firstOrNull()?.id

                        state.copy(
                            categories = categories,
                            selectedCategoryId = selectedId,
                            isLoadingReferences = false,
                        )
                    }
                }
        }
    }

    private fun changeTransactionType(type: TransactionType) {
        val entryType = type.requireEntryType()
        if (entryType == mutableUiState.value.transactionType) {
            return
        }

        mutableUiState.update { state ->
            state.copy(
                transactionType = entryType,
                categories = emptyList(),
                selectedCategoryId = null,
                isLoadingReferences = true,
            )
        }
        observeCategories(type = entryType)
    }

    private fun updateAmount(value: String) {
        if (value.length > MAX_AMOUNT_INPUT_LENGTH) {
            return
        }
        mutableUiState.update { state ->
            state.copy(amountInput = value)
        }
    }

    private fun updateNote(value: String) {
        mutableUiState.update { state ->
            state.copy(
                noteInput = value.take(MAX_NOTE_LENGTH),
            )
        }
    }

    private fun selectAccount(accountId: String) {
        val exists = mutableUiState.value.accounts.any { account ->
            account.id == accountId
        }
        if (!exists) {
            appLogger.warning(
                event = "transaction_account_selection_rejected",
                message = "An unavailable account selection was ignored.",
            )
            return
        }
        mutableUiState.update { state ->
            state.copy(selectedAccountId = accountId)
        }
    }

    private fun selectCategory(categoryId: String) {
        val exists = mutableUiState.value.categories.any { category ->
            category.id == categoryId
        }
        if (!exists) {
            appLogger.warning(
                event = "transaction_category_selection_rejected",
                message = "An unavailable category selection was ignored.",
            )
            return
        }
        mutableUiState.update { state ->
            state.copy(selectedCategoryId = categoryId)
        }
    }

    private fun saveTransaction() {
        val state = mutableUiState.value

        if (!state.canSave) {
            appLogger.warning(
                event = "transaction_entry_save_rejected",
                message = "The transaction form was incomplete or invalid.",
            )
            showMessage(text = "Complete the required transaction details.")
            return
        }

        val amount = requireNotNull(state.parsedAmount)
        val accountId = requireNotNull(state.selectedAccountId)
        val categoryId = requireNotNull(state.selectedCategoryId)

        mutableUiState.update { current ->
            current.copy(isSaving = true)
        }

        viewModelScope.launch {
            runCatching {
                saveManualTransaction(
                    draft = ManualTransactionDraft(
                        type = state.transactionType,
                        amount = amount,
                        accountId = accountId,
                        categoryId = categoryId,
                        paymentMethod = state.paymentMethod,
                        note = state.noteInput,
                    ),
                )
            }.onSuccess {
                mutableUiState.update { current ->
                    current.copy(
                        amountInput = "",
                        noteInput = "",
                        isSaving = false,
                    )
                }
                appLogger.info(
                    event = "manual_transaction_saved",
                    message = "A manual transaction was saved successfully.",
                )
                showMessage(text = "Transaction saved.")
            }.onFailure { throwable ->
                mutableUiState.update { current ->
                    current.copy(isSaving = false)
                }
                appLogger.error(
                    event = "manual_transaction_save_failed",
                    message = "A manual transaction could not be saved.",
                    throwable = throwable,
                )
                showMessage(
                    text = throwable.toUserFacingError().message,
                )
            }
        }
    }

    private fun retryReferences() {
        mutableUiState.update { state ->
            state.copy(isLoadingReferences = true)
        }
        observeAccounts()
        observeCategories(type = mutableUiState.value.transactionType)
    }

    private fun handleReferenceFailure(event: String, throwable: Throwable) {
        mutableUiState.update { state ->
            state.copy(isLoadingReferences = false)
        }
        appLogger.error(
            event = event,
            message = "Transaction reference data could not be loaded.",
            throwable = throwable,
        )
        showMessage(
            text = throwable.toUserFacingError().message,
        )
    }

    private fun showMessage(text: String) {
        nextMessageId += 1L
        mutableUiState.update { state ->
            state.copy(
                userMessage = UiMessage(
                    id = nextMessageId,
                    text = text,
                ),
            )
        }
    }

    private fun consumeMessage(id: Long) {
        mutableUiState.update { state ->
            if (state.userMessage?.id == id) {
                state.copy(userMessage = null)
            } else {
                state
            }
        }
    }

    private fun TransactionType.requireEntryType(): TransactionType {
        require(this != TransactionType.TRANSFER) {
            "Transaction entry supports expense and income only."
        }
        return this
    }

    private fun TransactionType.toCategoryType(): CategoryType = when (this) {
        TransactionType.EXPENSE -> CategoryType.EXPENSE
        TransactionType.INCOME -> CategoryType.INCOME
        TransactionType.TRANSFER -> error(
            "Transfer categories are not supported by transaction entry.",
        )
    }

    private companion object {
        const val MAX_AMOUNT_INPUT_LENGTH: Int = 18
        const val MAX_NOTE_LENGTH: Int = 120
    }
}
