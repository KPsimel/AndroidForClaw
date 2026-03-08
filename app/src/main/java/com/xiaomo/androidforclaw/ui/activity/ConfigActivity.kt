package com.xiaomo.androidforclaw.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xiaomo.androidforclaw.config.ConfigLoader
import com.xiaomo.androidforclaw.util.AppConstants
import com.draco.ladb.R
import com.draco.ladb.databinding.ActivityConfigBinding
import com.tencent.mmkv.MMKV

/**
 * 配置页面
 * 映射 OpenClaw CLI: openclaw config
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val configLoader by lazy { ConfigLoader(this) }

    companion object {
        private const val TAG = "ConfigActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "配置"
        }

        loadConfig()
        setupListeners()
    }

    private fun loadConfig() {
        try {
            Log.d(TAG, "开始加载配置...")

            // 从 openclaw.json 加载配置
            val config = configLoader.loadOpenClawConfig()
            Log.d(TAG, "配置加载成功")

            binding.apply {
                // 显示模型配置 - 使用 resolveProviders() 方法
                val providers = config.resolveProviders()
                Log.d(TAG, "providers: $providers")
                Log.d(TAG, "providers.size: ${providers.size}")

                if (providers.isNotEmpty()) {
                    val firstProvider = providers.entries.first()
                    val providerConfig = firstProvider.value
                    Log.d(TAG, "第一个 provider: ${firstProvider.key}, baseUrl: ${providerConfig.baseUrl}")

                    etApiBase.setText(providerConfig.baseUrl)
                    etApiKey.setText("*** (已配置)")

                    // 显示第一个模型
                    if (providerConfig.models.isNotEmpty()) {
                        val firstModel = providerConfig.models.first()
                        Log.d(TAG, "第一个模型: ${firstModel.name} (${firstModel.id})")
                    }

                    Log.d(TAG, "模型配置显示成功")
                } else {
                    Log.w(TAG, "providers 为空")
                    etApiBase.setText("未配置")
                    etApiKey.setText("未配置")
                }

                // 功能开关 - 从 openclaw.json 读取
                Log.d(TAG, "thinking.enabled: ${config.thinking.enabled}")
                Log.d(TAG, "agent.mode: ${config.agent.mode}")

                switchReasoning.isChecked = config.thinking.enabled
                switchExploration.isChecked = config.agent.mode == "exploration"

                // Gateway 配置
                Log.d(TAG, "gateway.port: ${config.gateway.port}")
                etGatewayPort.setText(config.gateway.port.toString())

                Log.d(TAG, "所有配置加载完成")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败", e)
            Log.e(TAG, "错误详情: ${e.stackTraceToString()}")
            Toast.makeText(this, "加载配置失败: ${e.message}", Toast.LENGTH_SHORT).show()

            // 加载失败时使用默认值
            binding.apply {
                etApiKey.setText("")
                etApiBase.setText("")
                switchReasoning.isChecked = true
                switchExploration.isChecked = false
                etGatewayPort.setText("8080")
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            // 保存按钮
            btnSave.setOnClickListener {
                saveConfig()
            }

            // 恢复默认按钮
            btnReset.setOnClickListener {
                resetToDefault()
            }

            // Skills 管理入口
            cardSkills.setOnClickListener {
                startActivity(Intent(this@ConfigActivity, SkillsActivity::class.java))
            }

            // Channels 管理入口
            cardChannels.setOnClickListener {
                startActivity(Intent(this@ConfigActivity, ChannelListActivity::class.java))
            }
        }
    }

    private fun saveConfig() {
        mmkv?.apply {
            encode("reasoning_enabled", binding.switchReasoning.isChecked)
            encode("exploration_mode", binding.switchExploration.isChecked)
        }

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun resetToDefault() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("恢复默认")
            .setMessage("确定要恢复所有配置为默认值吗？")
            .setPositiveButton("确定") { _, _ ->
                mmkv?.apply {
                    encode("reasoning_enabled", true)
                    encode("exploration_mode", false)
                }
                loadConfig()
                Toast.makeText(this, "已恢复默认配置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
