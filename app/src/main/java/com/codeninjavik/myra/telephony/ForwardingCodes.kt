package com.codeninjavik.myra.telephony

import android.net.Uri

object ForwardingCodes {
    /**
     * Build the MMI code to register forwarding.
     * Note: # must be percent-encoded as %23 before going into a tel: Uri.
     * Uri.encode() leaves it intact, Android parses it as a URI fragment, and
     * the carrier receives a truncated code. Setup appears to succeed and nothing is registered.
     */
    fun getRegistrationCode(mode: ForwardingMode, targetNumber: String, timeoutSeconds: Int = 20): String {
        val sanitizedTarget = targetNumber.replace("+", "").replace(" ", "")
        val rawCode = when (mode) {
            ForwardingMode.UNCONDITIONAL -> "**21*$sanitizedTarget#"
            ForwardingMode.ON_BUSY -> "**67*$sanitizedTarget#"
            ForwardingMode.ON_NO_ANSWER -> {
                // Ensure timeout is valid (5..30, multiple of 5)
                val clampedTimeout = (timeoutSeconds.coerceIn(5, 30) / 5) * 5
                "**61*$sanitizedTarget**$clampedTimeout#"
            }
            ForwardingMode.ON_UNREACHABLE -> "**62*$sanitizedTarget#"
        }
        return rawCode
    }

    fun getEraseCode(mode: ForwardingMode): String {
        return when (mode) {
            ForwardingMode.UNCONDITIONAL -> "##21#"
            ForwardingMode.ON_BUSY -> "##67#"
            ForwardingMode.ON_NO_ANSWER -> "##61#"
            ForwardingMode.ON_UNREACHABLE -> "##62#"
        }
    }

    fun getInterrogateCode(mode: ForwardingMode = ForwardingMode.UNCONDITIONAL): String {
        return when (mode) {
            ForwardingMode.UNCONDITIONAL -> "*#21#"
            ForwardingMode.ON_BUSY -> "*#67#"
            ForwardingMode.ON_NO_ANSWER -> "*#61#"
            ForwardingMode.ON_UNREACHABLE -> "*#62#"
        }
    }

    /**
     * Prepares MMI code for dialer URI format by replacing '#' with '%23' percent encoding.
     */
    fun formatForDial(mmiCode: String): String {
        return mmiCode.replace("#", "%23")
    }
}

enum class ForwardingMode {
    UNCONDITIONAL, // **21*
    ON_BUSY,        // **67*
    ON_NO_ANSWER,   // **61*
    ON_UNREACHABLE  // **62*
}
