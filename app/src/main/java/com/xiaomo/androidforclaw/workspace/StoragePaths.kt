package com.xiaomo.androidforclaw.workspace

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Unified storage path resolver.
 * Uses external storage (/sdcard/.androidforclaw) when available,
 * falls back to internal storage (context.filesDir/.androidforclaw) otherwise.
 */
object StoragePaths {

    private const val ROOT_NAME = ".androidforclaw"

    @Volatile
    private var resolvedRoot: File? = null

    /**
     * Initialize with application context. Must be called once at app startup.
     */
    fun init(context: Context) {
        if (resolvedRoot != null) return
        val externalRoot = File(Environment.getExternalStorageDirectory(), ROOT_NAME)
        resolvedRoot = if (isExternalStorageWritable(externalRoot)) {
            externalRoot
        } else {
            File(context.filesDir, ROOT_NAME).also { it.mkdirs() }
        }
    }

    /**
     * Root directory: /sdcard/.androidforclaw or <filesDir>/.androidforclaw
     */
    val root: File
        get() = resolvedRoot ?: File("/sdcard/$ROOT_NAME")

    val config: File get() = File(root, "config")
    val workspace: File get() = File(root, "workspace")
    val logs: File get() = File(root, "logs")
    val skills: File get() = File(root, "skills")

    /** openclaw.json lives directly under root */
    val openclawConfig: File get() = File(root, "openclaw.json")

    /**
     * Re-check storage after permission is granted.
     * If external storage becomes available, migrate to it.
     */
    fun recheckAfterPermission(context: Context) {
        val externalRoot = File(Environment.getExternalStorageDirectory(), ROOT_NAME)
        if (isExternalStorageWritable(externalRoot)) {
            resolvedRoot = externalRoot
        }
    }

    private fun isExternalStorageWritable(dir: File): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager() && (dir.exists() || dir.mkdirs())
            } else {
                dir.exists() || dir.mkdirs()
            }
        } catch (_: Exception) {
            false
        }
    }
}
