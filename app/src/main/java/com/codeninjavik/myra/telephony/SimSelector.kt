package com.codeninjavik.myra.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

data class SimCardInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val number: String,
    val phoneAccountHandle: PhoneAccountHandle?
)

class SimSelector(private val context: Context) {

    private val subscriptionManager: SubscriptionManager by lazy {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    }

    private val telecomManager: TelecomManager by lazy {
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }

    /**
     * Retrieves the list of active SIM cards on the device.
     * Requires READ_PHONE_STATE permission.
     */
    @SuppressLint("MissingPermission")
    fun getActiveSims(): List<SimCardInfo> {
        val simList = mutableListOf<SimCardInfo>()
        try {
            val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList ?: return emptyList()
            val callCapablePhoneAccounts = telecomManager.callCapablePhoneAccounts

            for (info in activeSubscriptionInfoList) {
                // Find matching PhoneAccountHandle by checking ICC ID or subscription ID matching
                val handle = findPhoneAccountHandle(info, callCapablePhoneAccounts)
                simList.add(
                    SimCardInfo(
                        subscriptionId = info.subscriptionId,
                        slotIndex = info.simSlotIndex,
                        displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                        carrierName = info.carrierName?.toString() ?: "Unknown Carrier",
                        number = info.number ?: "",
                        phoneAccountHandle = handle
                    )
                )
            }
        } catch (e: SecurityException) {
            // Permission not granted, return empty or default list
        } catch (e: Exception) {
            // Safe fallback
        }
        return simList
    }

    /**
     * Maps subscription info to a PhoneAccountHandle.
     */
    private fun findPhoneAccountHandle(
        info: SubscriptionInfo,
        accounts: List<PhoneAccountHandle>
    ): PhoneAccountHandle? {
        // Try to match the subscription ID
        for (account in accounts) {
            if (account.id == info.subscriptionId.toString() || account.id == info.iccId) {
                return account
            }
        }
        // Fallback: match by index if telecom manager has handles ordered
        try {
            val telecomServiceAccount = telecomManager.getPhoneAccount(accounts.getOrNull(info.simSlotIndex))
            if (telecomServiceAccount != null) {
                return accounts.getOrNull(info.simSlotIndex)
            }
        } catch (e: Exception) {
            // Ignore
        }
        return accounts.firstOrNull()
    }
}
