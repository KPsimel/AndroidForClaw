/**
 * OpenClaw Source Reference:
 * - ../openclaw/src/gateway/(all)
 *
 * AndroidForClaw adaptation: Termux one-click setup wizard.
 */
package com.xiaomo.androidforclaw.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Socket

class TermuxSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TermuxSetupScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Status checks
    var termuxInstalled by remember { mutableStateOf(false) }
    var sshReachable by remember { mutableStateOf(false) }
    var sshConfigured by remember { mutableStateOf(false) }
    var pythonAvailable by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(true) }

    // Generate setup command
    val setupCommand = remember {
        buildString {
            appendLine("# AndroidForClaw Termux 一键配置")
            appendLine("pkg update -y && pkg install -y openssh python nodejs &&")
            appendLine("mkdir -p ~/.ssh &&")
            appendLine("cat /sdcard/.androidforclaw/.ssh/id_ed25519.pub >> ~/.ssh/authorized_keys 2>/dev/null ||")
            appendLine("cat ~/storage/shared/.androidforclaw/.ssh/id_ed25519.pub >> ~/.ssh/authorized_keys 2>/dev/null &&")
            appendLine("sort -u ~/.ssh/authorized_keys -o ~/.ssh/authorized_keys &&")
            appendLine("chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys &&")
            appendLine("grep -q 'allow-external-apps' ~/.termux/termux.properties 2>/dev/null ||")
            appendLine("echo 'allow-external-apps=true' >> ~/.termux/termux.properties &&")
            appendLine("sshd &&")
            appendLine("TERMUX_USER=\$(whoami) &&")
            appendLine("echo '{\"user\":\"'\$TERMUX_USER'\",\"key_file\":\"/sdcard/.androidforclaw/.ssh/id_ed25519\"}' > /sdcard/.androidforclaw/termux_ssh.json &&")
            appendLine("echo '✅ 配置完成！用户: '\$TERMUX_USER")
        }.trim()
    }

    // Check status on launch and periodically
    fun checkStatus() {
        scope.launch {
            withContext(Dispatchers.IO) {
                // Check Termux installed
                termuxInstalled = try {
                    context.packageManager.getPackageInfo("com.termux", 0)
                    true
                } catch (_: Exception) { false }

                // Check SSH reachable
                sshReachable = try {
                    Socket().use { socket ->
                        socket.connect(java.net.InetSocketAddress("127.0.0.1", 8022), 1000)
                        true
                    }
                } catch (_: Exception) { false }

                // Check SSH config exists
                sshConfigured = try {
                    val file = File("/sdcard/.androidforclaw/termux_ssh.json")
                    file.exists() && file.readText().contains("user")
                } catch (_: Exception) { false }

                // Check keypair exists
                val keyExists = File("/sdcard/.androidforclaw/.ssh/id_ed25519").exists()
                if (!keyExists) {
                    // Generate keypair
                    try {
                        val keyDir = File("/sdcard/.androidforclaw/.ssh")
                        keyDir.mkdirs()
                        val process = Runtime.getRuntime().exec(arrayOf(
                            "sh", "-c",
                            "ssh-keygen -t ed25519 -f /sdcard/.androidforclaw/.ssh/id_ed25519 -N '' -q 2>/dev/null"
                        ))
                        process.waitFor()
                    } catch (_: Exception) {}
                }

                checking = false
            }
        }
    }

    LaunchedEffect(Unit) {
        checkStatus()
        // Auto-refresh every 3 seconds
        while (true) {
            delay(3000)
            checkStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Termux 配置") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                "🐧 Termux 配置向导",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Termux 让 AI 能在手机上运行 Python、Node.js 和 Shell 命令。\n按以下步骤完成配置：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            // Step 1: Install Termux
            StepCard(
                step = 1,
                title = "安装 Termux",
                description = "从 F-Droid 安装 Termux（不要用 Play Store 版本）",
                done = termuxInstalled,
                action = if (!termuxInstalled) {
                    {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "请手动搜索安装 Termux", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else null,
                actionLabel = "去下载"
            )

            // Step 2: Open Termux and paste command
            StepCard(
                step = 2,
                title = "打开 Termux，粘贴命令",
                description = "复制下面的命令，打开 Termux 粘贴执行（长按粘贴）",
                done = sshReachable && sshConfigured
            )

            // Command box
            if (termuxInstalled && !(sshReachable && sshConfigured)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = setupCommand,
                            color = Color(0xFF00FF00),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("termux-setup", setupCommand))
                            Toast.makeText(context, "✅ 已复制命令", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("复制命令")
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
                                if (intent != null) context.startActivity(intent)
                                else Toast.makeText(context, "Termux 未安装", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("打开 Termux")
                    }
                }
            }

            // Step 3: Verify
            StepCard(
                step = 3,
                title = "验证连接",
                description = if (sshReachable && sshConfigured) "SSH 连接正常，配置完成！"
                    else if (sshReachable) "SSH 可达，但配置文件未创建"
                    else "等待 Termux 配置完成...",
                done = sshReachable && sshConfigured
            )

            // All done
            if (sshReachable && sshConfigured) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "完成",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🎉 Termux 配置完成！",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "AI 现在可以通过 exec 命令执行 Python、Node.js 和 Shell 脚本了。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onBack) {
                            Text("完成")
                        }
                    }
                }
            }

            // Loading indicator
            if (checking) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
fun StepCard(
    step: Int,
    title: String,
    description: String,
    done: Boolean,
    action: (() -> Unit)? = null,
    actionLabel: String = ""
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step number / check
            if (done) {
                Icon(
                    Icons.Default.CheckCircle,
                    "完成",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$step", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (action != null && !done) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = action) { Text(actionLabel) }
                }
            }
        }
    }
}
