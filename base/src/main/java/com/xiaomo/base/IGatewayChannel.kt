package com.xiaomo.base

/**
 * 本地进程内 gateway 通信接口。
 *
 * 替代 WebSocket 实现，允许同进程的 ChatController 直接与 GatewayController 交互，
 * 无需经过本地 localhost:8765 WebSocket 中转。
 *
 * - 远程 gateway 连接仍使用 GatewaySession（WebSocket），它实现本接口。
 * - 本地同进程连接使用 LocalGatewayChannel（app 模块），同样实现本接口。
 */
interface IGatewayChannel {
    /**
     * 发起 RPC 请求，返回 JSON 字符串响应。
     */
    suspend fun request(method: String, paramsJson: String?, timeoutMs: Long = 15_000L): String

    /**
     * 发送节点事件（chat.subscribe 等）。
     * 本地实现可直接忽略或处理。
     */
    suspend fun sendNodeEvent(event: String, payloadJson: String?): Boolean

    /**
     * 注册事件监听器，用于接收来自 gateway 的推送事件（agent 进度、chat 状态等）。
     *
     * - LocalGatewayChannel：将监听器注册到 GatewayController，事件直接投递，不走 WebSocket。
     * - GatewaySession：默认 no-op，事件已通过构造时传入的 onEvent 回调路由。
     */
    fun setEventListener(listener: ((event: String, payloadJson: String) -> Unit)?) {}
}
