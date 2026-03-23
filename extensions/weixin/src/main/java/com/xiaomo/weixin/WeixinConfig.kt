/**
 * OpenClaw Source Reference:
 * - @tencent-weixin/openclaw-weixin/src/auth/accounts.ts
 *
 * Weixin channel configuration.
 */
package com.xiaomo.weixin

data class WeixinConfig(
    /** Whether the weixin channel is enabled */
    val enabled: Boolean = false,
    /** Bot token (obtained from QR login) */
    val token: String? = null,
    /** API base URL */
    val baseUrl: String = DEFAULT_BASE_URL,
    /** CDN base URL */
    val cdnBaseUrl: String = DEFAULT_CDN_BASE_URL,
    /** Account ID (normalized ilink_bot_id) */
    val accountId: String? = null,
    /** Linked Weixin user ID */
    val userId: String? = null,
    /** Route tag for API requests */
    val routeTag: String? = null,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://ilinkai.weixin.qq.com"
        const val DEFAULT_CDN_BASE_URL = "https://novac2c.cdn.weixin.qq.com/c2c"
    }

    val configured: Boolean get() = !token.isNullOrBlank()
}
