package com.codeninjavik.myra.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenStore(context: Context) {

    private val sharedPrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "myra_secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard SharedPreferences if encryption fails (e.g., in some testing environments)
            context.getSharedPreferences("myra_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_NUMBER = "user_number"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
    }

    var accessToken: String?
        get() = sharedPrefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = sharedPrefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = sharedPrefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = sharedPrefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var userNumber: String?
        get() = sharedPrefs.getString(KEY_USER_NUMBER, null)
        set(value) = sharedPrefs.edit().putString(KEY_USER_NUMBER, value).apply()

    var isSetupCompleted: Boolean
        get() = sharedPrefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}
