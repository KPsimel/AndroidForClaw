/**
 * OpenClaw Source Reference:
 * - 无 OpenClaw 对应 (Android 平台独有)
 */
package com.xiaomo.androidforclaw.ui.compose

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaomo.androidforclaw.R
import com.xiaomo.androidforclaw.accessibility.AccessibilityProxy
import com.xiaomo.androidforclaw.agent.skills.SkillsLoader
import com.xiaomo.androidforclaw.config.ConfigLoader
import com.xiaomo.androidforclaw.ui.activity.ModelConfigActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AndroidForClaw 状态 tab，替换 OpenClaw 的 Gateway Connection 页面。
 * 显示：LLM API 配置、Gateway、Channels、Skills、权限。
 */
@Composable
fun ForClawConnectTab() {
    val context = LocalContext.current

    // ── LLM 配置 ──────────────────────────────────────────────
    val loadingText = stringResource(R.string.connect_loading)
    var providerName by remember { mutableStateOf(loadingText) }
    var modelId by remember { mutableStateOf("") }
    var apiKeyOk by remember { mutableStateOf(false) }

    // ── Gateway (port 8765) ────────────────────────────────────
    var gatewayRunning by remember { mutableStateOf(false) }

    // ── Skills ─────────────────────────────────────────────────
    var skillsCount by remember { mutableStateOf(0) }

    // ── Channels ───────────────────────────────────────────────
    var feishuEnabled by remember { mutableStateOf(false) }
    var discordEnabled by remember { mutableStateOf(false) }
    var slackEnabled by remember { mutableStateOf(false) }
    var telegramEnabled by remember { mutableStateOf(false) }
    var whatsappEnabled by remember { mutableStateOf(false) }
    var signalEnabled by remember { mutableStateOf(false) }
    var weixinEnabled by remember { mutableStateOf(false) }

    // ── 权限（LiveData 实时同步）────────────────────────────────
    val accessibilityOk by AccessibilityProxy.isConnected.observeAsState(false)
    val overlayOk by AccessibilityProxy.overlayGranted.observeAsState(false)
    val screenCaptureOk by AccessibilityProxy.screenCaptureGranted.observeAsState(false)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // LLM config
            try {
                val loader = ConfigLoader(context)
                val config = loader.loadOpenClawConfig()
                val providers = config.resolveProviders()
                // 使用实际生效的默认模型（与 Agent 运行时一致）
                val resolvedModel = config.resolveDefaultModel()
                val resolvedProvider = resolvedModel.substringBefore("/", "")
                val entry = if (resolvedProvider.isNotEmpty()) {
                    providers[resolvedProvider]?.let { resolvedProvider to it }
                } else {
                    providers.entries.firstOrNull()?.let { it.key to it.value }
                }
                if (entry != null) {
                    providerName = entry.first
                    modelId = resolvedModel  // 显示完整的 provider/model 引用
                    val key = entry.second.apiKey
                    apiKeyOk = !key.isNullOrBlank() && !key.startsWith("\${") && key != "未配置"
                } else {
                    providerName = context.getString(R.string.connect_api_not_configured)
                    apiKeyOk = false
                }

                // Channels
                feishuEnabled = config.channels.feishu.enabled &&
                        config.channels.feishu.appId.isNotBlank()
                discordEnabled = config.channels.discord?.let {
                    it.enabled && !it.token.isNullOrBlank()
                } ?: false
                slackEnabled = config.channels.slack?.let {
                    it.enabled && it.botToken.isNotBlank()
                } ?: false
                telegramEnabled = config.channels.telegram?.let {
                    it.enabled && it.botToken.isNotBlank()
                } ?: false
                whatsappEnabled = config.channels.whatsapp?.let {
                    it.enabled && it.phoneNumber.isNotBlank()
                } ?: false
                signalEnabled = config.channels.signal?.let {
                    it.enabled && it.phoneNumber.isNotBlank()
                } ?: false
                weixinEnabled = config.channels.weixin?.let {
                    it.enabled
                } ?: false
            } catch (_: Exception) {
                providerName = context.getString(R.string.connect_read_failed)
            }

            // Gateway (local in-process channel)
            gatewayRunning = com.xiaomo.androidforclaw.core.MyApplication.isGatewayRunning()

            // Skills
            try {
                skillsCount = SkillsLoader(context).getStatistics().totalSkills
            } catch (_: Exception) {}

            // 权限状态由 AccessibilityProxy LiveData 驱动，无需在此手动检查
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── LLM API 配置 ──────────────────────────────────────
        val notConfigured = stringResource(R.string.connect_api_not_configured)
        val configured = stringResource(R.string.connect_api_configured)
        StatusCard(
            title = stringResource(R.string.connect_llm_api),
            icon = Icons.Default.SmartToy,
            rows = listOf(
                StatusRow(stringResource(R.string.connect_provider), providerName.ifBlank { notConfigured }),
                StatusRow(stringResource(R.string.connect_default_model), modelId.ifBlank { "—" }),
                StatusRow(stringResource(R.string.connect_api_key), if (apiKeyOk) configured else notConfigured, if (apiKeyOk) StatusLevel.Ok else StatusLevel.Error),
            ),
            onClick = {
                context.startActivity(Intent(context, ModelConfigActivity::class.java))
            },
            clickLabel = stringResource(R.string.connect_modify_config),
        )

        // ── Gateway ───────────────────────────────────────────
        StatusCard(
            title = stringResource(R.string.connect_local_gateway),
            icon = Icons.Default.Router,
            rows = listOf(
                StatusRow(stringResource(R.string.connect_port_label), "ws://127.0.0.1:8765"),
                StatusRow(stringResource(R.string.connect_status_label), if (gatewayRunning) stringResource(R.string.connect_running) else stringResource(R.string.connect_not_running),
                    if (gatewayRunning) StatusLevel.Ok else StatusLevel.Neutral),
            ),
        )

        // ── Web Clipboard ────────────────────────────────────
        val localIp = remember {
            try {
                java.net.NetworkInterface.getNetworkInterfaces()?.toList()
                    ?.flatMap { it.inetAddresses.toList() }
                    ?.firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                    ?.hostAddress ?: "未连接 WiFi"
            } catch (_: Exception) { "获取失败" }
        }
        val clipboardUrl = if (localIp.contains(".")) "http://$localIp:19789/clipboard" else localIp
        StatusCard(
            title = "Web Clipboard",
            icon = Icons.Default.ContentPaste,
            rows = listOf(
                StatusRow("地址", clipboardUrl),
                StatusRow("用途", "电脑输入 → 手机剪切板"),
            ),
            onClick = {
                if (clipboardUrl.startsWith("http")) {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(clipboardUrl))
                    context.startActivity(intent)
                }
            },
            clickLabel = "打开",
        )

        // ── Channels ──────────────────────────────────────────
        val enabled = stringResource(R.string.connect_enabled)
        val channelEntries = buildList {
            if (feishuEnabled)   add(StatusRow(stringResource(R.string.connect_feishu), enabled, StatusLevel.Ok))
            if (discordEnabled)  add(StatusRow("Discord",  enabled, StatusLevel.Ok))
            if (telegramEnabled) add(StatusRow("Telegram", enabled, StatusLevel.Ok))
            if (slackEnabled)    add(StatusRow("Slack",    enabled, StatusLevel.Ok))
            if (whatsappEnabled) add(StatusRow("WhatsApp", enabled, StatusLevel.Ok))
            if (signalEnabled)   add(StatusRow("Signal",   enabled, StatusLevel.Ok))
            if (weixinEnabled)   add(StatusRow(stringResource(R.string.connect_weixin), enabled, StatusLevel.Ok))
        }
        StatusCard(
            title = stringResource(R.string.connect_channels),
            icon = Icons.Default.Hub,
            rows = channelEntries.ifEmpty {
                listOf(StatusRow(stringResource(R.string.connect_channels), notConfigured, StatusLevel.Neutral))
            },
            onClick = {
                context.startActivity(
                    Intent().apply {
                        setClassName(
                            context,
                            "com.xiaomo.androidforclaw.ui.activity.ChannelListActivity"
                        )
                    }
                )
            },
            clickLabel = stringResource(R.string.connect_manage),
        )

        // ── Skills ────────────────────────────────────────────
        StatusCard(
            title = stringResource(R.string.connect_skills),
            icon = Icons.Default.Build,
            rows = listOf(
                StatusRow(stringResource(R.string.skill_loaded), if (skillsCount > 0) stringResource(R.string.connect_skills_loaded, skillsCount) else stringResource(R.string.connect_loading)),
            ),
        )

        // ── MCP Server（给外部 Agent 用，非 AndroidForClaw 自身）───
        val mcpRunning = remember { mutableStateOf(com.xiaomo.androidforclaw.mcp.ObserverMcpServer.isRunning()) }
        StatusCard(
            title = stringResource(R.string.connect_mcp_server),
            icon = Icons.Default.Dns,
            rows = listOf(
                StatusRow(stringResource(R.string.connect_status_label), if (mcpRunning.value) stringResource(R.string.connect_running) else stringResource(R.string.connect_mcp_stopped),
                    if (mcpRunning.value) StatusLevel.Ok else StatusLevel.Neutral),
                StatusRow(stringResource(R.string.connect_port_label), "${com.xiaomo.androidforclaw.mcp.ObserverMcpServer.DEFAULT_PORT}"),
            ),
            onClick = {
                context.startActivity(Intent(context,
                    com.xiaomo.androidforclaw.ui.activity.McpConfigActivity::class.java))
            },
            clickLabel = stringResource(R.string.connect_mcp_config),
        )

        // ── 权限 ─────────────────────────────────────────────
        val allPermissionsOk = accessibilityOk && screenCaptureOk
        val granted = stringResource(R.string.connect_granted)
        val notGranted = stringResource(R.string.connect_not_granted)
        StatusCard(
            title = stringResource(R.string.connect_permissions),
            icon = Icons.Default.Security,
            rows = listOf(
                StatusRow(stringResource(R.string.connect_accessibility), if (accessibilityOk) granted else notGranted,
                    if (accessibilityOk) StatusLevel.Ok else StatusLevel.Error),
                StatusRow(stringResource(R.string.connect_overlay), if (overlayOk) granted else notGranted,
                    if (overlayOk) StatusLevel.Ok else StatusLevel.Neutral),
                StatusRow(stringResource(R.string.connect_screen_capture), if (screenCaptureOk) granted else notGranted,
                    if (screenCaptureOk) StatusLevel.Ok else StatusLevel.Error),
            ),
            onClick = {
                try {
                    context.startActivity(Intent().apply {
                        component = ComponentName(
                            "com.xiaomo.androidforclaw",
                            "com.xiaomo.androidforclaw.accessibility.PermissionActivity"
                        )
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                } catch (_: Exception) {
                    context.startActivity(Intent(context,
                        com.xiaomo.androidforclaw.ui.activity.PermissionsActivity::class.java))
                }
            },
            clickLabel = if (allPermissionsOk) stringResource(R.string.connect_view) else stringResource(R.string.connect_go_grant),
        )
    }
}

// ─── helpers ──────────────────────────────────────────────────────────────────

private enum class StatusLevel { Ok, Error, Neutral }

private data class StatusRow(
    val label: String,
    val value: String,
    val level: StatusLevel = StatusLevel.Neutral,
)

@Composable
private fun StatusCard(
    title: String,
    icon: ImageVector,
    rows: List<StatusRow>,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (onClick != null && clickLabel != null) {
                    Text(
                        text = clickLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onClick),
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Data rows
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.label, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = when (row.level) {
                            StatusLevel.Ok -> MaterialTheme.colorScheme.primary
                            StatusLevel.Error -> MaterialTheme.colorScheme.error
                            StatusLevel.Neutral -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}
