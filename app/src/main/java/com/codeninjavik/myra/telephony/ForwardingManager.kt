package com.codeninjavik.myra.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

sealed class UssdResult {
    data class Success(val responseText: String, val isForwardedToTarget: Boolean) : UssdResult()
    data class Failed(val errorCode: Int, val message: String) : UssdResult()
}

class ForwardingManager(private val context: Context) {

    private val telecomManager: TelecomManager by lazy {
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }

    /**
     * Executes the dialing of an MMI code to register or erase forwarding rules.
     * Respects key dual-SIM and URL percent encoding requirements:
     *
     * 1. CRITICAL: MMI code '#' must be percent-encoded as '%23' before going into a tel: Uri.
     *    Uri.encode() leaves it intact, Android parses it as a URI fragment, and the carrier receives
     *    a truncated code. Setup appears to succeed and nothing is registered.
     *
     * 2. CRITICAL: EXTRA_PHONE_ACCOUNT_HANDLE must be set on the ACTION_CALL intent for dual-SIM phones.
     *    Without it, the rule lands on the system default SIM. The app shows green, the business number
     *    keeps ringing normally, and the user finds out days later.
     */
    @SuppressLint("MissingPermission")
    fun dialMmiCode(rawCode: String, phoneAccountHandle: PhoneAccountHandle?) {
        val encodedCode = ForwardingCodes.formatForDial(rawCode)
        val dialUri = Uri.parse("tel:$encodedCode")
        val intent = Intent(Intent.ACTION_CALL, dialUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (phoneAccountHandle != null) {
                putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            }
        }
        context.startActivity(intent)
    }

    /**
     * Registers forwarding rules on the selected SIM card.
     *
     * For conditional mode, all three codes (67, 61, 62) must be registered, dialed 2.5 seconds apart
     * — the radio drops codes sent while the previous one is in flight. Registering only 67 leaves
     * unanswered calls going to carrier voicemail.
     */
    suspend fun enableForwarding(
        targetNumber: String,
        isUnconditional: Boolean,
        phoneAccountHandle: PhoneAccountHandle?,
        timeoutSeconds: Int = 20,
        onCodeDialed: (String) -> Unit = {}
    ) {
        if (isUnconditional) {
            val code = ForwardingCodes.getRegistrationCode(ForwardingMode.UNCONDITIONAL, targetNumber)
            onCodeDialed(code)
            dialMmiCode(code, phoneAccountHandle)
        } else {
            // Conditional mode requires registering ON_BUSY (67), ON_NO_ANSWER (61), and ON_UNREACHABLE (62)
            // dialed 2.5 seconds apart.
            val codes = listOf(
                ForwardingCodes.getRegistrationCode(ForwardingMode.ON_BUSY, targetNumber),
                ForwardingCodes.getRegistrationCode(ForwardingMode.ON_NO_ANSWER, targetNumber, timeoutSeconds),
                ForwardingCodes.getRegistrationCode(ForwardingMode.ON_UNREACHABLE, targetNumber)
            )

            for (i in codes.indices) {
                if (i > 0) {
                    delay(2500) // Wait 2.5 seconds for radio channel clearance
                }
                onCodeDialed(codes[i])
                dialMmiCode(codes[i], phoneAccountHandle)
            }
        }
    }

    /**
     * Erases forwarding rules on the selected SIM card.
     */
    suspend fun disableForwarding(
        isUnconditional: Boolean,
        phoneAccountHandle: PhoneAccountHandle?,
        onCodeDialed: (String) -> Unit = {}
    ) {
        if (isUnconditional) {
            val code = ForwardingCodes.getEraseCode(ForwardingMode.UNCONDITIONAL)
            onCodeDialed(code)
            dialMmiCode(code, phoneAccountHandle)
        } else {
            val codes = listOf(
                ForwardingCodes.getEraseCode(ForwardingMode.ON_BUSY),
                ForwardingCodes.getEraseCode(ForwardingMode.ON_NO_ANSWER),
                ForwardingCodes.getEraseCode(ForwardingMode.ON_UNREACHABLE)
            )
            for (i in codes.indices) {
                if (i > 0) {
                    delay(2500)
                }
                onCodeDialed(codes[i])
                dialMmiCode(codes[i], phoneAccountHandle)
            }
        }
    }

    /**
     * Interrogates the carrier using USSD request (TelephonyManager.sendUssdRequest).
     *
     * Stage 2's response matching checks the last 10 digits, not by parsing prose
     * — carrier replies are free text and vary.
     */
    @SuppressLint("MissingPermission")
    suspend fun interrogateCarrier(
        phoneAccountHandle: PhoneAccountHandle?,
        targetNumber: String
    ): UssdResult = suspendCancellableCoroutine { continuation ->
        try {
            val telephonyManager = if (phoneAccountHandle != null) {
                // Get subscription-specific TelephonyManager
                val subId = getSubIdFromHandle(phoneAccountHandle)
                val systemTelephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (subId != null) {
                    systemTelephony.createForSubscriptionId(subId)
                } else {
                    systemTelephony
                }
            } else {
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            }

            val handler = Handler(Looper.getMainLooper())
            val callback = object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    response: CharSequence?
                ) {
                    val responseStr = response?.toString() ?: ""
                    Log.d("ForwardingManager", "USSD Response: $responseStr")
                    
                    // Match last 10 digits of target number to handle free-text formatting variations
                    val last10Digits = extractLast10Digits(targetNumber)
                    val isForwarded = if (last10Digits.isNotEmpty()) {
                        responseStr.replace(" ", "").contains(last10Digits)
                    } else {
                        false
                    }

                    if (continuation.isActive) {
                        continuation.resume(UssdResult.Success(responseStr, isForwarded))
                    }
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    failureCode: Int
                ) {
                    Log.e("ForwardingManager", "USSD Failed. Code: $failureCode")
                    if (continuation.isActive) {
                        continuation.resume(UssdResult.Failed(failureCode, "Carrier USSD request failed. Code: $failureCode"))
                    }
                }
            }

            val queryCode = ForwardingCodes.getInterrogateCode(ForwardingMode.UNCONDITIONAL)
            telephonyManager.sendUssdRequest(queryCode, callback, handler)

        } catch (e: SecurityException) {
            if (continuation.isActive) {
                continuation.resume(UssdResult.Failed(-1, "Security exception: Permission not granted."))
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(UssdResult.Failed(-2, "USSD error: ${e.localizedMessage}"))
            }
        }
    }

    private fun getSubIdFromHandle(handle: PhoneAccountHandle): Int? {
        return try {
            handle.id.toInt()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractLast10Digits(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length >= 10) {
            digits.takeLast(10)
        } else {
            digits
        }
    }
}
