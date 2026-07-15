package com.princevekariya.projectledger.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.common.UiMessage
import com.princevekariya.projectledger.core.common.error
import com.princevekariya.projectledger.core.common.info
import com.princevekariya.projectledger.core.common.toUserFacingError
import com.princevekariya.projectledger.core.common.warning
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.domain.transactions.account.CreateFinancialAccountDraft
import com.princevekariya.projectledger.domain.transactions.account.CreateFinancialAccountUseCase
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AccountSettingsViewModel(
    private val accountRepository: AccountRepository,
    private val createFinancialAccount: CreateFinancialAccountUseCase,
    private val appLogger: AppLogger,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        AccountSettingsUiState(),
    )
    private var accountObservationJob: Job? = null
    private var nextMessageId: Long = 0L

    val uiState: StateFlow<AccountSettingsUiState> =
        mutableUiState.asStateFlow()

    init {
        observeAccounts()
    }

    fun onAction(action: AccountSettingsAction) {
        when (action) {
            AccountSettingsAction.AddAccountRequested -> showAccountForm()
            AccountSettingsAction.CancelAccountRequested -> hideAccountForm()
            is AccountSettingsAction.AccountNameChanged -> {
                updateAccountName(value = action.value)
            }
            is AccountSettingsAction.OpeningBalanceChanged -> {
                updateOpeningBalance(value = action.value)
            }
            is AccountSettingsAction.AccountTypeSelected -> {
                selectAccountType(type = action.value)
            }
            AccountSettingsAction.SaveAccountRequested -> saveAccount()
            AccountSettingsAction.RetryRequested -> observeAccounts()
            is AccountSettingsAction.MessageShown -> {
                consumeMessage(id = action.id)
            }
        }
    }

    private fun observeAccounts() {
        accountObservationJob?.cancel()
        mutableUiState.update { state ->
            state.copy(loadState = UiLoadState.Loading)
        }
        accountObservationJob = viewModelScope.launch {
            accountRepository.observeAll()
                .catch { throwable ->
                    mutableUiState.update { state ->
                        state.copy(
                            loadState = UiLoadState.Error(
                                message = "Unable to load your accounts.",
                            ),
                        )
                    }
                    appLogger.error(
                        event = "account_settings_load_failed",
                        message = "Financial accounts could not be loaded.",
                        throwable = throwable,
                    )
                }
                .collect { accounts ->
                    val items = accounts
                        .sortedWith(
                            compareBy<FinancialAccount> {
                                    account ->
                                account.isArchived
                            }.thenBy { account ->
                                account.name.lowercase()
                            },
                        )
                        .map { account ->
                            AccountSettingsItem(
                                id = account.id,
                                name = account.name,
                                type = account.type,
                                openingBalance = account.openingBalance,
                                currentBalance = account.currentBalance,
                                isArchived = account.isArchived,
                            )
                        }
                    mutableUiState.update { state ->
                        state.copy(
                            accounts = items,
                            loadState = UiLoadState.Content,
                        )
                    }
                }
        }
    }

    private fun showAccountForm() {
        mutableUiState.update { state ->
            state.copy(isFormVisible = true)
        }
    }

    private fun hideAccountForm() {
        mutableUiState.update { state ->
            state.resetForm()
        }
    }

    private fun updateAccountName(value: String) {
        mutableUiState.update { state ->
            state.copy(
                accountNameInput = value.take(MAX_ACCOUNT_NAME_LENGTH),
            )
        }
    }

    private fun updateOpeningBalance(value: String) {
        if (value.length > MAX_BALANCE_INPUT_LENGTH) {
            return
        }
        mutableUiState.update { state ->
            state.copy(openingBalanceInput = value)
        }
    }

    private fun selectAccountType(type: AccountType) {
        mutableUiState.update { state ->
            state.copy(selectedAccountType = type)
        }
    }

    private fun saveAccount() {
        val state = mutableUiState.value
        if (!state.canSave) {
            appLogger.warning(
                event = "account_creation_rejected",
                message = "The account form was incomplete or invalid.",
            )
            showMessage(
                text = "Enter an account name and a valid opening balance.",
            )
            return
        }

        val openingBalance = requireNotNull(state.parsedOpeningBalance)
        mutableUiState.update { current ->
            current.copy(isSaving = true)
        }

        viewModelScope.launch {
            runCatching {
                createFinancialAccount(
                    draft = CreateFinancialAccountDraft(
                        name = state.accountNameInput,
                        type = state.selectedAccountType,
                        openingBalance = openingBalance,
                    ),
                )
            }.onSuccess { account ->
                mutableUiState.update { current ->
                    current.resetForm()
                }
                appLogger.info(
                    event = "financial_account_created",
                    message = "A financial account was created successfully.",
                )
                showMessage(text = "${account.name} account created.")
            }.onFailure { throwable ->
                mutableUiState.update { current ->
                    current.copy(isSaving = false)
                }
                appLogger.error(
                    event = "financial_account_creation_failed",
                    message = "A financial account could not be created.",
                    throwable = throwable,
                )
                val message = if (throwable is IllegalArgumentException) {
                    throwable.message.orEmpty()
                        .ifBlank {
                            throwable.toUserFacingError().message
                        }
                } else {
                    throwable.toUserFacingError().message
                }
                showMessage(text = message)
            }
        }
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

    private fun AccountSettingsUiState.resetForm(): AccountSettingsUiState = copy(
        isFormVisible = false,
        accountNameInput = "",
        openingBalanceInput = "0",
        selectedAccountType = AccountType.CASH,
        isSaving = false,
    )

    private companion object {
        const val MAX_ACCOUNT_NAME_LENGTH: Int = 40
        const val MAX_BALANCE_INPUT_LENGTH: Int = 18
    }
}
