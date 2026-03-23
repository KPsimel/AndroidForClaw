/**
 * OpenClaw Source Reference:
 * - @tencent-weixin/openclaw-weixin/src/auth/accounts.ts
 *
 * Persistent account storage using MMKV.
 */
package com.xiaomo.weixin.storage

import android.util.Log
import com.google.gson.Gson
import com.tencent.mmkv.MMKV

/**
 * Stored account data (persisted after QR login).
 */
data class WeixinAccountData(
    val token: String? = null,
    val baseUrl: String? = null,
    val userId: String? = null,
    val accountId: String? = null,
    val savedAt: String? = null,
)

object WeixinAccountStore {
    private const val TAG = "WeixinAccountStore"
    private const val MMKV_ID = "weixin_accounts"
    private const val KEY_ACCOUNT_DATA = "account_data"
    private const val KEY_SYNC_BUF = "sync_buf"

    private val gson = Gson()
    private val mmkv: MMKV? get() = MMKV.mmkvWithID(MMKV_ID)

    fun saveAccount(data: WeixinAccountData) {
        val json = gson.toJson(data)
        mmkv?.encode(KEY_ACCOUNT_DATA, json)
        Log.i(TAG, "Account saved: accountId=${data.accountId}")
    }

    fun loadAccount(): WeixinAccountData? {
        val json = mmkv?.decodeString(KEY_ACCOUNT_DATA) ?: return null
        return try {
            gson.fromJson(json, WeixinAccountData::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse account data", e)
            null
        }
    }

    fun clearAccount() {
        mmkv?.removeValueForKey(KEY_ACCOUNT_DATA)
        mmkv?.removeValueForKey(KEY_SYNC_BUF)
        Log.i(TAG, "Account cleared")
    }

    fun saveSyncBuf(buf: String) {
        mmkv?.encode(KEY_SYNC_BUF, buf)
    }

    fun loadSyncBuf(): String {
        return mmkv?.decodeString(KEY_SYNC_BUF) ?: ""
    }
}
