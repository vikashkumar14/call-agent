package com.codeninjavik.myra.data.repository

import android.telecom.PhoneAccountHandle
import com.codeninjavik.myra.core.network.MyraApi
import com.codeninjavik.myra.core.storage.SettingsStore
import com.codeninjavik.myra.core.util.Outcome
import com.codeninjavik.myra.data.model.DisableForwardingRequest
import com.codeninjavik.myra.data.model.ForwardingTargetResponse
import com.codeninjavik.myra.data.model.VerifyForwardingRequest
import com.codeninjavik.myra.telephony.ForwardingManager
import com.codeninjavik.myra.telephony.UssdResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ForwardingRepository(
    private val api: MyraApi,
    private val settingsStore: SettingsStore,
    private val forwardingManager: ForwardingManager
) {

    /**
     * Fetch MYRA's inbound target number and configuration from server.
     */
    suspend fun fetchForwardingTarget(): Outcome<ForwardingTargetResponse> {
        return try {
            val response = api.getForwardingTarget()
            // Cache locally
            settingsStore.myraTargetNumber = response.number
            settingsStore.myraDisplayLabel = response.displayLabel
            Outcome.Success(response)
        } catch (e: Exception) {
            // Fallback to local cache if offline
            val cachedNum = settingsStore.myraTargetNumber
            if (cachedNum != null) {
                Outcome.Success(
                    ForwardingTargetResponse(
                        number = cachedNum,
                        mode = settingsStore.forwardingMode,
                        displayLabel = settingsStore.myraDisplayLabel ?: "MYRA Inbound"
                    )
                )
            } else {
                Outcome.Failure("Failed to retrieve forwarding target: ${e.localizedMessage}", e)
            }
        }
    }

    /**
     * Initiates the forwarding verification test-call sequence and polls for the outcome.
     * Status values returned by server: PENDING, CONFIRMED, NOT_FORWARDED, WRONG_TARGET, TIMED_OUT
     */
    fun verifyForwardingTestCall(userNumber: String, isUnconditional: Boolean): Flow<Outcome<String>> = flow {
        emit(Outcome.Loading)
        try {
            val modeStr = if (isUnconditional) "unconditional" else "conditional"
            val request = VerifyForwardingRequest(userNumber, modeStr)
            val response = api.verifyForwarding(request)
            val ticket = response.ticket

            // Poll the verification status every 3 seconds, up to 15 times (45 seconds max)
            var attempts = 0
            val maxAttempts = 15
            var isCompleted = false

            while (attempts < maxAttempts && !isCompleted) {
                delay(3000)
                attempts++
                try {
                    val statusResponse = api.getForwardingVerificationStatus(ticket)
                    when (statusResponse.status) {
                        "CONFIRMED" -> {
                            settingsStore.isForwardingActive = true
                            emit(Outcome.Success("Forwarding successfully verified and active!"))
                            isCompleted = true
                        }
                        "NOT_FORWARDED" -> {
                            settingsStore.isForwardingActive = false
                            emit(Outcome.Failure("The test call rang your phone instead of reaching MYRA. If you have two SIMs, check you picked the right one."))
                            isCompleted = true
                        }
                        "WRONG_TARGET" -> {
                            settingsStore.isForwardingActive = false
                            emit(Outcome.Failure("The forwarding rule is registered to a different number, not your designated MYRA line."))
                            isCompleted = true
                        }
                        "TIMED_OUT" -> {
                            emit(Outcome.Failure("The verification test call timed out. Please check your cellular connection and retry."))
                            isCompleted = true
                        }
                        "PENDING" -> {
                            // Continue polling
                        }
                        else -> {
                            emit(Outcome.Failure("Unknown status: ${statusResponse.status} - ${statusResponse.detail}"))
                            isCompleted = true
                        }
                    }
                } catch (e: Exception) {
                    // Polling network error, don't abort immediately unless consecutive failures
                }
            }

            if (!isCompleted) {
                emit(Outcome.Failure("Verification timed out. Please try again."))
            }

        } catch (e: Exception) {
            emit(Outcome.Failure("Failed to start forwarding verification: ${e.localizedMessage}", e))
        }
    }

    /**
     * Local carrier interrogation (Stage 2) using TelephonyManager ussd request.
     */
    suspend fun interrogateCarrier(
        phoneAccountHandle: PhoneAccountHandle?,
        targetNumber: String
    ): Outcome<UssdResult.Success> {
        return when (val result = forwardingManager.interrogateCarrier(phoneAccountHandle, targetNumber)) {
            is UssdResult.Success -> Outcome.Success(result)
            is UssdResult.Failed -> Outcome.Failure("Interrogation failed: ${result.message}")
        }
    }

    /**
     * Disable MYRA forwarding rules both locally and remotely.
     */
    suspend fun disableForwarding(userNumber: String): Outcome<String> {
        return try {
            api.disableForwarding(DisableForwardingRequest(userNumber))
            settingsStore.isForwardingActive = false
            Outcome.Success("Forwarding rule disabled.")
        } catch (e: Exception) {
            // Local fallback
            settingsStore.isForwardingActive = false
            Outcome.Success("Forwarding disabled locally, but remote status sync failed: ${e.localizedMessage}")
        }
    }
}
