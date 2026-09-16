package com.codeninjavik.myra.ui.forwarding

import android.telecom.PhoneAccountHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeninjavik.myra.ServiceLocator
import com.codeninjavik.myra.core.storage.SettingsStore
import com.codeninjavik.myra.core.util.Outcome
import com.codeninjavik.myra.data.repository.ForwardingRepository
import com.codeninjavik.myra.telephony.ForwardingCodes
import com.codeninjavik.myra.telephony.SimCardInfo
import com.codeninjavik.myra.telephony.SimSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ForwardingSetupStage {
    IDLE,
    DIAL_MMI,      // Stage 1: Initiated Dial
    ASK_CARRIER,   // Stage 2: TelephonyManager.sendUssdRequest
    TEST_CALL,     // Stage 3: Backend places a test call
    ACTIVE         // Stage 4: Verified and Active
}

data class ForwardingUiState(
    val stage: ForwardingSetupStage = ForwardingSetupStage.IDLE,
    val userNumber: String = "",
    val availableSims: List<SimCardInfo> = emptyList(),
    val selectedSim: SimCardInfo? = null,
    val targetNumber: String = "Fetching...",
    val displayLabel: String = "MYRA AI Inbound",
    val isUnconditional: Boolean = true,
    val isBusyActive: Boolean = false,
    val isOn: Boolean = false,
    val ussdQueryResult: String? = null,
    val errorMsg: String? = null,
    val infoMsg: String? = null,
    val isProcessing: Boolean = false
)

class ForwardingViewModel(
    private val repository: ForwardingRepository = ServiceLocator.forwardingRepository,
    private val simSelector: SimSelector = ServiceLocator.simSelector,
    private val settingsStore: SettingsStore = ServiceLocator.settingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForwardingUiState())
    val uiState: StateFlow<ForwardingUiState> = _uiState.asStateFlow()

    init {
        loadSettingsAndSims()
        fetchTargetNumber()
    }

    private fun loadSettingsAndSims() {
        viewModelScope.launch {
            val sims = simSelector.getActiveSims()
            val savedNumber = settingsStore.myraTargetNumber ?: ""
            val savedLabel = settingsStore.myraDisplayLabel ?: "MYRA AI"
            val isUnconditional = settingsStore.forwardingMode == "unconditional"
            val isActive = settingsStore.isForwardingActive

            _uiState.value = _uiState.value.copy(
                availableSims = sims,
                selectedSim = sims.firstOrNull(),
                targetNumber = if (savedNumber.isNotEmpty()) savedNumber else "Fetching...",
                displayLabel = savedLabel,
                isUnconditional = isUnconditional,
                isOn = isActive,
                stage = if (isActive) ForwardingSetupStage.ACTIVE else ForwardingSetupStage.IDLE
            )
        }
    }

    fun selectSim(sim: SimCardInfo) {
        _uiState.value = _uiState.value.copy(selectedSim = sim)
    }

    fun setUserNumber(number: String) {
        _uiState.value = _uiState.value.copy(userNumber = number)
    }

    fun setForwardingMode(isUnconditional: Boolean) {
        _uiState.value = _uiState.value.copy(isUnconditional = isUnconditional)
        settingsStore.forwardingMode = if (isUnconditional) "unconditional" else "conditional"
    }

    private fun fetchTargetNumber() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            when (val outcome = repository.fetchForwardingTarget()) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(
                        targetNumber = outcome.data.number,
                        displayLabel = outcome.data.displayLabel,
                        isProcessing = false
                    )
                }
                is Outcome.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMsg = outcome.errorMsg,
                        isProcessing = false
                    )
                }
                else -> {}
            }
        }
    }

    /**
     * Stage 1: Dial MMI Code via ACTION_CALL
     */
    fun startStage1Dial() {
        val state = _uiState.value
        val targetNum = state.targetNumber
        if (targetNum == "Fetching..." || targetNum.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMsg = "Please wait, fetching target MYRA number first.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            errorMsg = null,
            infoMsg = "Dialing registration code on your SIM..."
        )

        viewModelScope.launch {
            try {
                ServiceLocator.forwardingManager.enableForwarding(
                    targetNumber = targetNum,
                    isUnconditional = state.isUnconditional,
                    phoneAccountHandle = state.selectedSim?.phoneAccountHandle,
                    timeoutSeconds = 20
                ) { code ->
                    _uiState.value = _uiState.value.copy(
                        infoMsg = "Dialed MMI: $code. Please confirm carrier dialog on screen."
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    stage = ForwardingSetupStage.DIAL_MMI,
                    isProcessing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMsg = "Dial failed: ${e.localizedMessage}",
                    isProcessing = false
                )
            }
        }
    }

    /**
     * Stage 2: Ask Carrier (TelephonyManager.sendUssdRequest)
     */
    fun startStage2AskCarrier() {
        val state = _uiState.value
        val targetNum = state.targetNumber
        
        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            errorMsg = null,
            infoMsg = "Interrogating carrier forwarding status..."
        )

        viewModelScope.launch {
            when (val result = repository.interrogateCarrier(state.selectedSim?.phoneAccountHandle, targetNum)) {
                is Outcome.Success -> {
                    val ussdText = result.data.responseText
                    val matchesTarget = result.data.isForwardedToTarget
                    
                    _uiState.value = _uiState.value.copy(
                        ussdQueryResult = ussdText,
                        stage = ForwardingSetupStage.ASK_CARRIER,
                        infoMsg = if (matchesTarget) {
                            "Carrier reports active forwarding to target!"
                        } else {
                            "Carrier interrogation complete. (Prose vary, proceeding to test call to be fully secure)"
                        },
                        isProcessing = false
                    )
                }
                is Outcome.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        ussdQueryResult = "Failed to query carrier automatically: ${result.errorMsg}",
                        stage = ForwardingSetupStage.ASK_CARRIER,
                        infoMsg = "Carrier query failed. Proceeding to rigorous Stage 3 test call verification.",
                        isProcessing = false
                    )
                }
                else -> {}
            }
        }
    }

    /**
     * Stage 3: Test Call Verification
     */
    fun startStage3TestCall() {
        val state = _uiState.value
        val userPhone = state.userNumber
        if (userPhone.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMsg = "Please enter your 10-digit Indian mobile number to trigger verification.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            stage = ForwardingSetupStage.TEST_CALL,
            errorMsg = null,
            infoMsg = "Triggering test call from MYRA. Do not answer if your phone rings..."
        )

        viewModelScope.launch {
            repository.verifyForwardingTestCall(userPhone, state.isUnconditional).collect { outcome ->
                when (outcome) {
                    is Outcome.Loading -> {
                        _uiState.value = _uiState.value.copy(isProcessing = true)
                    }
                    is Outcome.Success -> {
                        settingsStore.isForwardingActive = true
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            stage = ForwardingSetupStage.ACTIVE,
                            isOn = true,
                            infoMsg = outcome.data,
                            errorMsg = null
                        )
                    }
                    is Outcome.Failure -> {
                        settingsStore.isForwardingActive = false
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            isOn = false,
                            errorMsg = outcome.errorMsg,
                            infoMsg = null
                        )
                    }
                }
            }
        }
    }

    /**
     * Turn OFF Forwarding
     */
    fun turnOffForwarding() {
        val state = _uiState.value
        _uiState.value = _uiState.value.copy(isProcessing = true, errorMsg = null)

        viewModelScope.launch {
            // Place the MMI code to erase settings on device
            try {
                ServiceLocator.forwardingManager.disableForwarding(
                    isUnconditional = state.isUnconditional,
                    phoneAccountHandle = state.selectedSim?.phoneAccountHandle
                ) { code ->
                    _uiState.value = _uiState.value.copy(infoMsg = "Dialed Erase: $code")
                }

                // Call remote disable
                repository.disableForwarding(state.userNumber)
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    stage = ForwardingSetupStage.IDLE,
                    isOn = false,
                    infoMsg = "Forwarding turned off successfully."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMsg = "Error turning off forwarding: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearNotifications() {
        _uiState.value = _uiState.value.copy(errorMsg = null, infoMsg = null)
    }
}
