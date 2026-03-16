package com.agent.mobile.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.agent.mobile.Point
import com.agent.mobile.ViewNode
import com.agent.mobile.core.MyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.agent.mobile.util.LayoutExceptionLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

// adb 抓view tree结构 遇到bug
class PhoneAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PhoneAccessibilityService"
        @JvmField
        var Accessibility: PhoneAccessibilityService? = null

        // 无障碍权限状态常量
        const val STATUS_SYSTEM_DISABLED = "系统无障碍未开启"
        const val STATUS_SERVICE_NOT_ENABLED = "服务未在系统设置中启用"
        const val STATUS_SERVICE_NOT_CONNECTED = "服务未连接"
        const val STATUS_AUTHORIZED = "已授权"
        const val STATUS_CHECK_FAILED = "检查失败"

        // 使用 LiveData 存储无障碍服务状态
        val accessibilityEnabled = MutableLiveData<Boolean>().apply {
            postValue(false) // 初始状态为 false
        }

        // 周期监控与节流
        private val monitorScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        /**
         * 检查无障碍服务是否已开启
         */
        fun isAccessibilityServiceEnabled(): Boolean {
//            val isEnabled = Accessibility != null
            val isEnabled = isSystemAccessibilityEnabled(MyApplication.application.applicationContext)
            accessibilityEnabled.postValue(isEnabled) // 同步更新 LiveData 状态
            return isEnabled
        }

        fun requestAccessibilityPermission(context: Context) {
            try {
                Log.d(TAG, "开始申请无障碍权限")
                try {
                    val appPkg = context.applicationContext.packageName
                    val serviceClass = PhoneAccessibilityService::class.java.name
                    val serviceName = "$appPkg/$serviceClass"
                    Settings.Secure.putString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        serviceName
                    )
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED,
                        1
                    )
                    Log.d(TAG, "无障碍权限申请命令已发送: $serviceName")
                } catch (e: Exception) {
                    LayoutExceptionLogger.log("PhoneAccessibilityService#requestAccessibilityPermission#sendCommand", e)
                    Log.w(TAG, "代码申请无障碍权限失败: ${'$'}{e.message}")
                }

                monitorScope.launch {
                    try {
                        delay(1000)
                        val isEnabled = isSystemAccessibilityEnabled(context)
                        if (isEnabled) {
                            Log.d(TAG, "无障碍权限申请成功")
                        } else {
                            Log.d(TAG, "代码申请失败，跳转到系统设置页面")
                            Toast.makeText(context, "代码申请失败，请手动开启无障碍权限", Toast.LENGTH_LONG).show()
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        LayoutExceptionLogger.log("PhoneAccessibilityService#requestAccessibilityPermission#checkResult", e)
                        Log.e(TAG, "异步检查权限申请结果异常", e)
                    }
                }
            } catch (e: Exception) {
                LayoutExceptionLogger.log("PhoneAccessibilityService#requestAccessibilityPermission", e)
                Log.e(TAG, "申请无障碍权限失败", e)
            }
        }
        
        /**
         * 检查系统无障碍权限是否已开启
         */
        fun isSystemAccessibilityEnabled(context: Context): Boolean {
            if (accessibilityEnabled.value == true) return true
            return try {
                val accessibilityEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                ) == 1
                
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                
                val serviceName = "${context.packageName}/${PhoneAccessibilityService::class.java.name}"
                val isServiceEnabled = enabledServices?.contains(serviceName) == true
                
                Log.d(TAG, "系统无障碍权限: $accessibilityEnabled")
                Log.d(TAG, "服务已启用: $isServiceEnabled")
                Log.d(TAG, "服务实例存在: ${Accessibility != null}")
                
                accessibilityEnabled && isServiceEnabled && Accessibility != null
            } catch (e: Exception) {
                LayoutExceptionLogger.log("PhoneAccessibilityService#isSystemAccessibilityEnabled", e)
                Log.e(TAG, "检查无障碍权限失败", e)
                false
            }
        }
        
        /**
         * 获取无障碍权限详细状态
         */
        fun getAccessibilityStatus(context: Context): String {
            return try {
                val accessibilityEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                ) == 1
                
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                
                val serviceName = "${context.packageName}/${PhoneAccessibilityService::class.java.name}"
                val isServiceEnabled = enabledServices?.contains(serviceName) == true
                val isServiceConnected = Accessibility != null
                
                when {
                    !accessibilityEnabled -> STATUS_SYSTEM_DISABLED
                    !isServiceEnabled -> STATUS_SERVICE_NOT_ENABLED
                    !isServiceConnected -> STATUS_SERVICE_NOT_CONNECTED
                    else -> STATUS_AUTHORIZED
                }
            } catch (e: Exception) {
                LayoutExceptionLogger.log("PhoneAccessibilityService#checkAccessibilityStatus", e)
                "$STATUS_CHECK_FAILED: ${e.message}"
            }
        }
    }

    var currentPackageName = ""
    var activityClassName = ""
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Accessibility = this
        accessibilityEnabled.postValue(true) // 直接更新 LiveData
        Log.d(TAG, "onServiceConnected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        Log.d(TAG, "onAccessibilityEvent")
        Accessibility = this
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.packageName != packageName) {
                currentPackageName = event.packageName?.toString() ?: ""
                activityClassName = event.className?.toString() ?: ""

                Log.d(TAG, "当前前台App: $currentPackageName, 当前Activity: $activityClassName")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
        Accessibility = null
        accessibilityEnabled.postValue(false) // 更新 LiveData
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind - 无障碍服务断开")
        Accessibility = null
        accessibilityEnabled.postValue(false) // 更新 LiveData
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - 无障碍服务销毁")
        Accessibility = null
        accessibilityEnabled.postValue(false) // 更新 LiveData
    }

    // 保存遍历的全局Index
    private var globalIndex = 0

    fun dumpView(): List<ViewNode> {
        // 使用 getWindows() 方法获取所有窗口，而不仅仅是当前活动窗口
        val windows = this.windows
        if (windows.isEmpty()) {
            Log.w(TAG, "No windows available, trying rootInActiveWindow as fallback")
            // 尝试使用传统的 rootInActiveWindow 作为备选方案
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                globalIndex = 0
                val nodesList = mutableListOf<ViewNode>()
                traverseNode(rootNode, nodesList)
                return nodesList
            }
            return emptyList()
        }

        globalIndex = 0  // 每次dump时重置计数
        val nodesList = mutableListOf<ViewNode>()

        // 遍历所有窗口，按Z-order排序，顶层窗口优先
        val sortedWindows = windows.sortedByDescending { it.layer }
        Log.d(TAG, "Found ${sortedWindows.size} windows")

        // 遍历所有窗口
        for ((index, window) in sortedWindows.withIndex()) {
            val rootNode = window.root
            if (rootNode == null) {
                Log.w(TAG, "Window $index has no root node")
                continue
            }

            Log.d(
                TAG,
                "Processing window $index: ${window.title}, type: ${window.type}, layer: ${window.layer}"
            )
            try {
                traverseNode(rootNode, nodesList)
            } catch (e: Exception) {
                LayoutExceptionLogger.log("PhoneAccessibilityService#getWindowNodes#traverse", e)
                Log.e(TAG, "Error traversing window $index", e)
            }
        }

        Log.d(TAG, "Total nodes collected: ${nodesList.size}")
        return nodesList
    }

    private fun traverseNode(node: AccessibilityNodeInfo, nodesList: MutableList<ViewNode>) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        // 验证边界矩形是否有效
        val isValidRect = rect.left >= 0 && rect.top >= 0 && rect.right > rect.left && rect.bottom > rect.top
        
        if (!isValidRect) {
            // 边界无效的节点不保存，但继续遍历子节点（子节点可能有效）
            val nodeText = node.text?.toString() ?: ""
            val nodeContentDesc = node.contentDescription?.toString() ?: ""
            Log.w(TAG, "traverseNode跳过边界无效的节点: text='$nodeText', contentDesc='$nodeContentDesc', " +
                    "边界=[left=${rect.left}, top=${rect.top}, right=${rect.right}, bottom=${rect.bottom}], " +
                    "可能是ViewPager中未显示的Tab页面节点")
            // 继续遍历子节点
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { childNode ->
                    traverseNode(childNode, nodesList)
                }
            }
            return
        }
        
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        
        // 验证中心坐标是否有效（非负数）
        if (centerX < 0 || centerY < 0) {
            val nodeText = node.text?.toString() ?: ""
            val nodeContentDesc = node.contentDescription?.toString() ?: ""
            Log.w(TAG, "traverseNode跳过坐标无效的节点: text='$nodeText', contentDesc='$nodeContentDesc', " +
                    "centerX=$centerX, centerY=$centerY, 边界=[left=${rect.left}, top=${rect.top}, right=${rect.right}, bottom=${rect.bottom}]")
            // 继续遍历子节点
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { childNode ->
                    traverseNode(childNode, nodesList)
                }
            }
            return
        }

        val nodeInfo = ViewNode(
            index = globalIndex++,
            text = node.text?.toString(),
            resourceId = node.viewIdResourceName,
            className = node.className?.toString(),
            packageName = node.packageName?.toString(),
            contentDesc = node.contentDescription?.toString(),
            clickable = node.isClickable,
            enabled = node.isEnabled,
            focusable = node.isFocusable,
            focused = node.isFocused,
            scrollable = node.isScrollable,
            point = Point(centerX, centerY),
            left = rect.left,
            right = rect.right,
            top = rect.top,
            bottom = rect.bottom,
            node = node
        )
        nodesList.add(nodeInfo)

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { childNode ->
                traverseNode(childNode, nodesList)
            }
        }
    }

    /**
     * 根据文本查找并点击某个节点
     */
    suspend fun clickViewByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodeList = rootNode.findAccessibilityNodeInfosByText(text)
        nodeList.firstOrNull()?.let { node ->
            return performClick(node)
        }
        Log.w(TAG, "No node found with text: $text")
        return false
    }

    public suspend fun performClick(
        node: AccessibilityNodeInfo,
        isLongClick: Boolean = false
    ): Boolean {
        if (isLongClick) {
            return performLongClick(node)
        }
        Log.d(TAG, "performClick: ${node}")
        // 如果节点可点击并成功点击，直接返回
        if ( node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        // 向上查找父节点并尝试点击
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            parent = parent.parent
        }

        // 设置无障碍焦点和选择状态（对弹窗中的控件特别重要）
        node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        node.performAction(AccessibilityNodeInfo.ACTION_SELECT)

        // 获取坐标点击
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val centerX = (rect.left + rect.right) / 2
        val centerY = (rect.top + rect.bottom) / 2

        val path = Path().apply { moveTo(centerX.toFloat(), centerY.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 200))
            .build()
        val result = CompletableDeferred<Boolean>()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "点击完成")
                result.complete(true)  // 手势成功完成
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.d(TAG, "点击被取消")
                result.complete(false)  // 手势成功完成
            }
        }, null)

        // 等待结果（带超时）
        return withTimeoutOrNull(500) {
            result.await()
        } ?: false  // 超时返回 false
    }

    /**
     * 通过坐标执行点击操作
     * @param x 点击的 X 坐标
     * @param y 点击的 Y 坐标
     * @param isLongClick 是否长按，默认为 false
     * @return 点击是否成功
     */
    public suspend fun performClickAt(
        x: Float,
        y: Float,
        isLongClick: Boolean = false
    ): Boolean {
        Log.d(TAG, "performClickAt: x=$x, y=$y, isLongClick=$isLongClick")
        
        val duration = if (isLongClick) 600L else 200L
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        
        val result = CompletableDeferred<Boolean>()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "坐标点击完成: ($x, $y)")
                result.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.d(TAG, "坐标点击被取消: ($x, $y)")
                result.complete(false)
            }
        }, null)

        // 等待结果（带超时）
        return withTimeoutOrNull(500) {
            result.await()
        } ?: false
    }

    public suspend fun performLongClick(node: AccessibilityNodeInfo): Boolean {
        Log.d(TAG, "performLongClick: ${node}")

        // 如果节点可点击并成功点击，直接返回
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
            return true
        }

        // 向上查找父节点并尝试点击
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                return true
            }
            parent = parent.parent
        }
        return false
    }

    fun pressHomeButton() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun pressBackButton() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        rootInActiveWindow?.let {
            val swipe = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        Path().apply {
                            moveTo(startX, startY)
                            lineTo(endX, endY)
                        }, 0L, 200L
                    )
                ).build()

            dispatchGesture(swipe, null, null)
        }
    }


}