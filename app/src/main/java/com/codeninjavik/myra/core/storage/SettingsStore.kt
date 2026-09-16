package com.codeninjavik.myra.core.storage

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("myra_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FORWARDING_ACTIVE = "forwarding_active"
        private const val KEY_FORWARDING_MODE = "forwarding_mode" // "unconditional" or "conditional"
        private const val KEY_MYRA_TARGET_NUMBER = "myra_target_number"
        private const val KEY_MYRA_DISPLAY_LABEL = "myra_display_label"
    }

    var isForwardingActive: Boolean
        get() = prefs.getBoolean(KEY_FORWARDING_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_FORWARDING_ACTIVE, value).apply()

    var forwardingMode: String
        get() = prefs.getString(KEY_FORWARDING_MODE, "unconditional") ?: "unconditional"
        set(value) = prefs.edit().putString(KEY_FORWARDING_MODE, value).apply()

    var myraTargetNumber: String?
        get() = prefs.getString(KEY_MYRA_TARGET_NUMBER, null)
        set(value) = prefs.edit().putString(KEY_MYRA_TARGET_NUMBER, value).apply()

    var myraDisplayLabel: String?
        get() = prefs.getString(KEY_MYRA_DISPLAY_LABEL, null)
        set(value) = prefs.edit().putString(KEY_MYRA_DISPLAY_LABEL, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
