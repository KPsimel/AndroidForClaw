/**
 * OpenClaw Source Reference:
 * - 无 OpenClaw 对应 (Android 平台独有)
 */
package com.xiaomo.androidforclaw.ui.compose

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import com.xiaomo.androidforclaw.R
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tencent.mmkv.MMKV
import com.xiaomo.androidforclaw.ui.activity.*
import com.xiaomo.androidforclaw.ui.activity.LegalActivity
import com.xiaomo.androidforclaw.ui.float.SessionFloatWindow
import com.xiaomo.androidforclaw.updater.AppUpdater
import com.xiaomo.androidforclaw.util.MMKVKeys
import kotlinx.coroutines.launch

@Composable
fun ForClawSettingsTab() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 配置 ─────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.settings_section_config)) {
            SettingsNavItem(
                icon = Icons.Default.SmartToy,
                title = stringResource(R.string.settings_model_config),
                subtitle = stringResource(R.string.settings_model_config_desc),
                onClick = { context.startActivity(Intent(context, ModelConfigActivity::class.java)) }
            )
            SettingsNavItem(
                icon = Icons.Default.Hub,
                title = stringResource(R.string.settings_channels),
                subtitle = stringResource(R.string.settings_channels_desc),
                onClick = { context.startActivity(Intent(context, ChannelListActivity::class.java)) }
            )
            SettingsNavItem(
                icon = Icons.Default.Extension,
                title = stringResource(R.string.settings_skills),
                subtitle = stringResource(R.string.settings_skills_desc),
                onClick = { context.startActivity(Intent(context, SkillsActivity::class.java)) }
            )
            SettingsNavItem(
                icon = Icons.Default.Terminal,
                title = stringResource(R.string.settings_termux),
                subtitle = stringResource(R.string.settings_termux_desc),
                onClick = { context.startActivity(Intent(context, TermuxSetupActivity::class.java)) }
            )
        }

        // ── 文件 ─────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.settings_section_files)) {
            SettingsNavItem(
                icon = Icons.Default.Description,
                title = "openclaw.json",
                subtitle = "/sdcard/.androidforclaw/openclaw.json",
                onClick = {
                    val file = java.io.File("/sdcard/.androidforclaw/openclaw.json")
                    if (file.exists()) {
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.provider", file
                            )
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "text/plain")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    },
                                    context.getString(R.string.settings_select_editor)
                                )
                            )
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, context.getString(R.string.settings_cannot_open, e.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.widget.Toast.makeText(context, context.getString(R.string.settings_file_not_found), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // ── 界面 ─────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.settings_section_ui)) {
            FloatWindowToggleItem()
        }

        // ── 应用 ─────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.settings_section_app)) {
            CheckUpdateItem()
            RestartAppItem()
        }

        // ── 法律 ─────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.settings_section_legal)) {
            SettingsNavItem(
                icon = Icons.Default.Policy,
                title = stringResource(R.string.settings_privacy_policy),
                subtitle = stringResource(R.string.settings_privacy_desc),
                onClick = { LegalActivity.start(context, LegalActivity.TYPE_PRIVACY) }
            )
            SettingsNavItem(
                icon = Icons.Default.Gavel,
                title = stringResource(R.string.settings_terms),
                subtitle = stringResource(R.string.settings_terms_desc),
                onClick = { LegalActivity.start(context, LegalActivity.TYPE_TERMS) }
            )
        }

        // ── 关于 ─────────────────────────────────────────────────
        AboutSection()
    }
}

// ─── Section wrapper ─────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content()
        }
    }
}

// ─── Nav item ────────────────────────────────────────────────────────────────

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ─── Specific items ───────────────────────────────────────────────────────────

@Composable
private fun FloatWindowToggleItem() {
    val context = LocalContext.current
    val mmkv = remember { MMKV.defaultMMKV() }
    var enabled by remember { mutableStateOf(mmkv.decodeBool(MMKVKeys.FLOAT_WINDOW_ENABLED.key, false)) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PictureInPicture,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_float_window), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.settings_float_window_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { v ->
                    enabled = v
                    SessionFloatWindow.setEnabled(context, v)
                }
            )
        }
    }
}

@Composable
private fun CheckUpdateItem() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val updater = remember { AppUpdater(context) }
    val currentVersion = remember { updater.getCurrentVersion() }

    SettingsNavItem(
        icon = Icons.Default.SystemUpdate,
        title = stringResource(R.string.settings_check_update),
        subtitle = stringResource(R.string.settings_current_version, currentVersion),
        onClick = {
            android.widget.Toast.makeText(context, context.getString(R.string.settings_checking_update), android.widget.Toast.LENGTH_SHORT).show()
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val info = updater.checkForUpdate()
                    if (info.hasUpdate && info.downloadUrl != null) {
                        val success = updater.downloadAndInstall(info.downloadUrl, info.latestVersion)
                        if (!success) {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl))
                                )
                            } catch (_: Exception) {}
                        }
                    } else {
                        android.widget.Toast.makeText(context, context.getString(R.string.settings_up_to_date, info.currentVersion), android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_check_failed, e.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

@Composable
private fun RestartAppItem() {
    val context = LocalContext.current

    SettingsNavItem(
        icon = Icons.Default.RestartAlt,
        title = stringResource(R.string.settings_restart_app),
        subtitle = stringResource(R.string.settings_restart_desc),
        onClick = {
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.settings_restart_title))
                .setMessage(context.getString(R.string.settings_restart_message))
                .setPositiveButton(context.getString(R.string.settings_restart_confirm)) { _, _ ->
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent?.let { context.startActivity(it) }
                    (context as? android.app.Activity)?.finishAffinity()
                }
                .setNegativeButton(context.getString(R.string.action_cancel), null)
                .show()
        }
    )
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val packageInfo = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (_: Exception) { null }
    }
    val versionName = packageInfo?.versionName ?: "Unknown"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.settings_section_about),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            AboutRow(stringResource(R.string.settings_version), "v$versionName")
            AboutRow(stringResource(R.string.settings_email), "xiaomochn@gmail.com", onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:xiaomochn@gmail.com") })
                } catch (_: Exception) {
                    val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cb.setPrimaryClip(android.content.ClipData.newPlainText("Email", "xiaomochn@gmail.com"))
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_copied), android.widget.Toast.LENGTH_SHORT).show()
                }
            })
            AboutRow(stringResource(R.string.settings_wechat), "xiaomocn", onClick = {
                val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cb.setPrimaryClip(android.content.ClipData.newPlainText("WeChat", "xiaomocn"))
                android.widget.Toast.makeText(context, context.getString(R.string.settings_copied), android.widget.Toast.LENGTH_SHORT).show()
            })
            AboutRow(stringResource(R.string.settings_feishu_group), stringResource(R.string.settings_feishu_join), onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://applink.feishu.cn/client/chat/chatter/add_by_link?link_token=566r8836-6547-43e0-b6be-d6c4a5b12b74"
                    )))
                } catch (_: Exception) {
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_cannot_open_link), android.widget.Toast.LENGTH_SHORT).show()
                }
            })
            AboutRow(stringResource(R.string.settings_github), stringResource(R.string.settings_github_desc), onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://github.com/SelectXn00b/AndroidForClaw"
                    )))
                } catch (_: Exception) {
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_cannot_open_link), android.widget.Toast.LENGTH_SHORT).show()
                }
            })
            // 版权信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    stringResource(R.string.settings_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.settings_inspired),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    val mod = if (onClick != null)
        Modifier.fillMaxWidth()
    else
        Modifier.fillMaxWidth()

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = mod,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
