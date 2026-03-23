/**
 * OpenClaw Source Reference:
 * - @tencent-weixin/openclaw-weixin/src/channel.ts
 *
 * Weixin channel runtime — main entry point.
 * Manages QR login, long-poll monitor, and message send/receive.
 */
package com.xiaomo.weixin

import android.util.Log
import com.xiaomo.weixin.api.WeixinApi
import com.xiaomo.weixin.auth.QRLoginResult
import com.xiaomo.weixin.auth.WeixinQRLogin
import com.xiaomo.weixin.messaging.ContextTokenStore
import com.xiaomo.weixin.messaging.WeixinInboundMessage
import com.xiaomo.weixin.messaging.WeixinSender
import com.xiaomo.weixin.storage.WeixinAccountData
import com.xiaomo.weixin.storage.WeixinAccountStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow

/**
 * Main entry point for the Weixin channel.
 *
 * Usage:
 * 1. Create WeixinChannel(config)
 * 2. If not configured, call startQRLogin() and follow the flow
 * 3. Call start() to begin receiving messages
 * 4. Collect messageFlow for inbound messages
 * 5. Use sender to send replies
 */
class WeixinChannel(private val config: WeixinConfig) {
    companion object {
        private const val TAG = "WeixinChannel"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var api: WeixinApi? = null
    private var monitor: WeixinMonitor? = null
    private var _sender: WeixinSender? = null

    /** Current account data (from store or login). */
    private var accountData: WeixinAccountData? = null

    /** Inbound message flow — collect this in the app to dispatch to Agent. */
    val messageFlow: SharedFlow<WeixinInboundMessage>?
        get() = monitor?.messageFlow

    /** Outbound message sender. */
    val sender: WeixinSender?
        get() = _sender

    /** Whether the channel is actively receiving messages. */
    @Volatile var connected: Boolean = false
        private set

    /**
     * Initialize the channel. Loads saved account if available.
     * Returns true if the account is configured (has a token).
     */
    fun init(): Boolean {
        accountData = WeixinAccountStore.loadAccount()
        val data = accountData

        if (data != null && !data.token.isNullOrBlank()) {
            val baseUrl = data.baseUrl?.takeIf { it.isNotBlank() } ?: config.baseUrl
            api = WeixinApi(
                baseUrl = baseUrl,
                token = data.token,
                routeTag = config.routeTag,
            )
            val accountId = data.accountId ?: "unknown"
            _sender = WeixinSender(api!!, accountId)
            monitor = WeixinMonitor(api!!, accountId)
            Log.i(TAG, "Initialized with saved account: $accountId")
            return true
        }

        Log.i(TAG, "No saved account, QR login required")
        return false
    }

    /**
     * Start the long-poll monitor. Call init() first.
     */
    fun start(): Boolean {
        if (api == null || monitor == null) {
            Log.e(TAG, "Cannot start: not initialized. Call init() first.")
            return false
        }

        monitor?.start(scope)
        connected = true
        Log.i(TAG, "✅ Weixin channel started")
        return true
    }

    /**
     * Stop the channel.
     */
    fun stop() {
        monitor?.stop()
        api?.shutdown()
        connected = false
        Log.i(TAG, "Weixin channel stopped")
    }

    /**
     * Create a QR login manager.
     */
    fun createQRLogin(): WeixinQRLogin {
        val baseUrl = accountData?.baseUrl?.takeIf { it.isNotBlank() }
            ?: config.baseUrl
        return WeixinQRLogin(apiBaseUrl = baseUrl, routeTag = config.routeTag)
    }

    /**
     * Apply login result: update API, sender, monitor with new credentials.
     */
    fun applyLoginResult(result: QRLoginResult): Boolean {
        if (!result.connected || result.botToken.isNullOrBlank()) {
            Log.e(TAG, "Cannot apply: login not successful")
            return false
        }

        val baseUrl = result.baseUrl?.takeIf { it.isNotBlank() } ?: config.baseUrl
        val accountId = result.accountId ?: "unknown"

        // Recreate API with new token
        api?.shutdown()
        api = WeixinApi(
            baseUrl = baseUrl,
            token = result.botToken,
            routeTag = config.routeTag,
        )
        _sender = WeixinSender(api!!, accountId)
        monitor = WeixinMonitor(api!!, accountId)

        accountData = WeixinAccountStore.loadAccount()

        Log.i(TAG, "Applied login result: accountId=$accountId")
        return true
    }

    /**
     * Logout: clear stored credentials and stop.
     */
    fun logout() {
        stop()
        WeixinAccountStore.clearAccount()
        ContextTokenStore.clear()
        api = null
        _sender = null
        monitor = null
        accountData = null
        Log.i(TAG, "Logged out")
    }

    /**
     * Get stored account data.
     */
    fun getAccountData(): WeixinAccountData? = accountData ?: WeixinAccountStore.loadAccount()

    /**
     * Check if account is configured (has token).
     */
    fun isConfigured(): Boolean {
        val data = accountData ?: WeixinAccountStore.loadAccount()
        return !data?.token.isNullOrBlank()
    }
}
