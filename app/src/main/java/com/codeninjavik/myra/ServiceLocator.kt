package com.codeninjavik.myra

import android.content.Context
import com.codeninjavik.myra.core.network.ApiClient
import com.codeninjavik.myra.core.network.MyraApi
import com.codeninjavik.myra.core.storage.SettingsStore
import com.codeninjavik.myra.core.storage.TokenStore
import com.codeninjavik.myra.data.repository.ForwardingRepository
import com.codeninjavik.myra.telephony.ForwardingManager
import com.codeninjavik.myra.telephony.SimSelector

object ServiceLocator {

    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
    }

    private fun getContextOrThrow(): Context {
        return context ?: throw IllegalStateException("ServiceLocator has not been initialized. Call initialize(Context) first.")
    }

    val tokenStore: TokenStore by lazy {
        TokenStore(getContextOrThrow())
    }

    val settingsStore: SettingsStore by lazy {
        SettingsStore(getContextOrThrow())
    }

    val myraApi: MyraApi by lazy {
        ApiClient.createMyraApi(tokenStore)
    }

    val simSelector: SimSelector by lazy {
        SimSelector(getContextOrThrow())
    }

    val forwardingManager: ForwardingManager by lazy {
        ForwardingManager(getContextOrThrow())
    }

    val forwardingRepository: ForwardingRepository by lazy {
        ForwardingRepository(myraApi, settingsStore, forwardingManager)
    }
}
