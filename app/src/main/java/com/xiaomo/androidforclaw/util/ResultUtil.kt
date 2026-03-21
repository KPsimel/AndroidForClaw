/**
 * OpenClaw Source Reference:
 * - 无 OpenClaw 对应 (Android 平台独有)
 */
package com.xiaomo.androidforclaw.util

import com.xiaomo.androidforclaw.data.model.ResultBean

object ResultUtil {

    fun getResults(): List<ResultBean> {
        // 这里可以从本地存储或数据库获取结果数据
        return emptyList()
    }

    fun saveResult(result: ResultBean) {
        // 保存结果到本地存储或数据库
    }

    fun clearResults() {
        // 清除所有结果
    }
}
