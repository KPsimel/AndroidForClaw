/**
 * Weixin channel setup page — QR code login + status display.
 */
package com.xiaomo.androidforclaw.ui.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xiaomo.weixin.WeixinChannel
import com.xiaomo.weixin.WeixinConfig
import com.xiaomo.weixin.storage.WeixinAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeixinChannelActivity : ComponentActivity() {
    companion object {
        private const val TAG = "WeixinChannelActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WeixinChannelScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeixinChannelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val configLoader = remember { com.xiaomo.androidforclaw.config.ConfigLoader(context) }
    val openClawConfig = remember { configLoader.loadOpenClawConfig() }
    val weixinCfg = openClawConfig.channels.weixin

    var statusText by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var accountInfo by remember { mutableStateOf("") }

    // Check existing account on load
    LaunchedEffect(Unit) {
        val account = WeixinAccountStore.loadAccount()
        if (account != null && !account.token.isNullOrBlank()) {
            isLoggedIn = true
            accountInfo = "账号: ${account.accountId ?: "未知"}\n用户: ${account.userId ?: "未知"}"
            statusText = "✅ 已登录"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("微信 (Weixin)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "通过微信 ClawBot 插件接入",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isLoggedIn) {
                // Show logged-in state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("✅ 已连接微信", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(accountInfo, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Logout button
                OutlinedButton(
                    onClick = {
                        WeixinAccountStore.clearAccount()
                        isLoggedIn = false
                        accountInfo = ""
                        statusText = "已退出登录"
                        qrBitmap = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("退出登录")
                }
            } else {
                // QR code display
                qrBitmap?.let { bmp ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("使用微信扫描二维码", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "微信登录二维码",
                                modifier = Modifier.size(250.dp),
                            )
                        }
                    }
                }

                // Login button
                Button(
                    onClick = {
                        isLoggingIn = true
                        statusText = "正在获取二维码..."
                        qrBitmap = null

                        scope.launch {
                            try {
                                val baseUrl = weixinCfg?.baseUrl
                                    ?.takeIf { it.isNotBlank() }
                                    ?: WeixinConfig.DEFAULT_BASE_URL

                                val channel = WeixinChannel(
                                    WeixinConfig(baseUrl = baseUrl, routeTag = weixinCfg?.routeTag)
                                )
                                val qrLogin = channel.createQRLogin()

                                // Fetch QR code
                                val qrResult = qrLogin.fetchQRCode()
                                if (qrResult == null) {
                                    statusText = "❌ 获取二维码失败"
                                    isLoggingIn = false
                                    return@launch
                                }

                                val (qrcodeUrl, qrcode) = qrResult

                                // Download QR image
                                statusText = "正在加载二维码..."
                                val bitmap = withContext(Dispatchers.IO) {
                                    downloadQRBitmap(qrcodeUrl)
                                }
                                if (bitmap != null) {
                                    qrBitmap = bitmap
                                    statusText = "请使用微信扫描二维码"
                                } else {
                                    statusText = "⚠️ 二维码加载失败，请重试"
                                    isLoggingIn = false
                                    return@launch
                                }

                                // Wait for login
                                val loginResult = qrLogin.waitForLogin(
                                    qrcode = qrcode,
                                    onStatusUpdate = { status ->
                                        statusText = when (status) {
                                            "wait" -> "等待扫码..."
                                            "scaned" -> "👀 已扫码，请在微信上确认"
                                            "expired" -> "二维码已过期，正在刷新..."
                                            "confirmed" -> "✅ 登录成功！"
                                            else -> status
                                        }
                                    },
                                    onQRRefreshed = { newUrl ->
                                        scope.launch {
                                            val newBitmap = withContext(Dispatchers.IO) {
                                                downloadQRBitmap(newUrl)
                                            }
                                            if (newBitmap != null) {
                                                qrBitmap = newBitmap
                                            }
                                        }
                                    }
                                )

                                if (loginResult.connected) {
                                    isLoggedIn = true
                                    accountInfo = "账号: ${loginResult.accountId ?: "未知"}\n用户: ${loginResult.userId ?: "未知"}"
                                    statusText = loginResult.message
                                    qrBitmap = null
                                } else {
                                    statusText = "❌ ${loginResult.message}"
                                }
                            } catch (e: Exception) {
                                Log.e("WeixinLogin", "Login error", e)
                                statusText = "❌ 登录失败: ${e.message}"
                            } finally {
                                isLoggingIn = false
                            }
                        }
                    },
                    enabled = !isLoggingIn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoggingIn) "登录中..." else "扫码登录")
                }
            }

            // Status text
            if (statusText.isNotBlank()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusText.startsWith("✅")) {
                        MaterialTheme.colorScheme.primary
                    } else if (statusText.startsWith("❌")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("说明", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "基于微信 ClawBot 插件协议，扫码后即可通过微信与 AI 对话。\n" +
                                "• 仅支持私聊消息\n" +
                                "• 支持文字、图片、语音、文件\n" +
                                "• 登录凭证保存在本地",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/**
 * Download QR code image as Bitmap.
 */
private fun downloadQRBitmap(url: String): Bitmap? {
    return try {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.connect()
        val inputStream = connection.inputStream
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        connection.disconnect()
        bitmap
    } catch (e: Exception) {
        Log.e("WeixinQR", "Failed to download QR bitmap", e)
        null
    }
}
