package com.shuaiqiu.fuckets100

import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * 零宽字符漏洞工具类
 * 利用零宽字符绕过 Android 对 /sdcard/Android/data 的访问限制
 * 
 * 支持的零宽字符：
 * - \u200b 零宽空格 (Zero Width Space)
 * - \u200c 零宽非连接符 (Zero Width Non-Joiner)
 * - \u200d 零宽连接符 (Zero Width Joiner)
 * - \uFEFF 字节顺序标记 (Byte Order Mark)
 */
object ZWCHelper {
    private const val TAG = "ZWCHelper"
    
    // 零宽字符列表（按优先级排序）
    private val ZWC_CHARS = listOf(
        "\u200b",  // 零宽空格
        "\u200c",  // 零宽非连接符
        "\u200d",  // 零宽连接符
        "\uFEFF"   // 字节顺序标记
    )
    
    // 常规路径
    private const val NORMAL_PATH = "/sdcard/Android/data"
    
    // 目标应用路径
    private const val TARGET_APP_PATH = "com.ets100.secondary/files/Download/ETS_secondary"
    
    /**
     * 生成带零宽字符的绕过路径
     * 格式: /sdcard/Android/[ZWC]data/com.ets100.secondary/files/Download/ETS_secondary
     */
    fun generateZWCPath(zwcChar: String): String {
        return "/sdcard/Android/$zwcChar" + "data/$TARGET_APP_PATH"
    }
    
    /**
     * 获取所有可能的绕过路径
     */
    fun getAllBypassPaths(): List<String> {
        return ZWC_CHARS.map { generateZWCPath(it) }
    }
    
    /**
     * 检测零宽字符漏洞是否可用
     * 通过实际尝试访问绕过路径来检测
     */
    fun isVulnerabilityAvailable(): Boolean {
        return try {
            // Android 10 及以下不需要漏洞
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Log.d(TAG, "Android 10 及以下，不需要漏洞")
                return true
            }
            
            // 尝试每种零宽字符
            for (zwcChar in ZWC_CHARS) {
                val bypassPath = generateZWCPath(zwcChar)
                Log.d(TAG, "尝试零宽字符路径: $bypassPath")
                
                if (testPathAccess(bypassPath)) {
                    Log.d(TAG, "零宽字符 $zwcChar (U+${
                        Integer.toHexString(zwcChar.codePointAt(0)).uppercase()
                    }) 漏洞可用")
                    return true
                }
            }
            
            Log.d(TAG, "所有零宽字符路径都不可访问")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "检测零宽字符漏洞失败", e)
            false
        }
    }
    
    /**
     * 测试路径是否可访问
     */
    private fun testPathAccess(path: String): Boolean {
        return try {
            val file = File(path)
            val canRead = file.canRead()
            val isDir = file.isDirectory
            
            Log.d(TAG, "路径: $path")
            Log.d(TAG, "  canRead: $canRead")
            Log.d(TAG, "  isDirectory: $isDir")
            
            if (canRead && isDir) {
                // 尝试列出目录内容
                val files = file.list()
                Log.d(TAG, "  listFiles: ${files?.size ?: 0} 个文件")
                return true
            }
            false
        } catch (e: Exception) {
            Log.d(TAG, "  访问失败: ${e.message}")
            false
        }
    }
    
    /**
     * 获取第一个可用的绕过路径
     */
    fun getWorkingBypassPath(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "/sdcard/Android/data/$TARGET_APP_PATH"
        }
        
        for (zwcChar in ZWC_CHARS) {
            val bypassPath = generateZWCPath(zwcChar)
            if (testPathAccess(bypassPath)) {
                return bypassPath
            }
        }
        return null
    }
    
    /**
     * 检测直读模式是否可用（普通方式或零宽字符漏洞）
     */
    fun isDirectReadAvailable(): Boolean {
        return try {
            // 首先检查是否有全文件访问权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    // 没有全文件访问权限，尝试零宽字符漏洞
                    val available = isVulnerabilityAvailable()
                    Log.d(TAG, "全文件访问权限检查: false, 零宽字符漏洞: $available")
                    return available
                }
            }
            
            // 有权限或有漏洞，直接检查常规路径
            val normalFile = File("/sdcard/Android/data/$TARGET_APP_PATH")
            if (normalFile.canRead() && normalFile.isDirectory) {
                Log.d(TAG, "常规路径可访问")
                return true
            }
            
            // 常规路径不可读，尝试零宽字符漏洞
            isVulnerabilityAvailable()
        } catch (e: Exception) {
            Log.e(TAG, "检测直读可用性失败", e)
            false
        }
    }
    
    /**
     * 获取 data 目录的实际可用路径
     * 会尝试多种方式获取可访问的路径
     */
    fun getDataDir(): String {
        // Android 10 及以下直接返回常规路径
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "/sdcard/Android/data/$TARGET_APP_PATH"
        }
        
        // 尝试获取工作路径
        val workingPath = getWorkingBypassPath()
        if (workingPath != null) {
            return workingPath
        }
        
        // 如果零宽字符都不行，尝试常规路径
        val normalPath = "/sdcard/Android/data/$TARGET_APP_PATH"
        if (File(normalPath).canRead()) {
            return normalPath
        }
        
        // 返回常规路径作为最后手段
        Log.w(TAG, "无法找到可用的绕过路径，使用常规路径")
        return normalPath
    }
    
    /**
     * 获取状态描述文本
     */
    fun getStatusDescription(): String {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> "Android 10 及以下，无需特殊处理"
            isVulnerabilityAvailable() -> "零宽字符漏洞可用，可访问 Android/data"
            Environment.isExternalStorageManager() -> "全文件访问权限已获取"
            else -> "暂不支持直读，请授权全文件访问或使用其他模式"
        }
    }
    
    /**
     * 获取详细的调试信息
     */
    fun getDebugInfo(): String {
        val sb = StringBuilder()
        sb.appendLine("=== ZWCHelper 调试信息 ===")
        sb.appendLine("Android 版本: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        sb.appendLine("设备型号: ${Build.MODEL}")
        sb.appendLine("厂商: ${Build.MANUFACTURER}")
        sb.appendLine()
        
        // 检测每种零宽字符
        sb.appendLine("零宽字符检测结果:")
        for (zwcChar in ZWC_CHARS) {
            val path = generateZWCPath(zwcChar)
            val accessible = testPathAccess(path)
            val unicode = "U+%04X".format(zwcChar.codePointAt(0))
            sb.appendLine("  \\$unicode ($zwcChar): ${if (accessible) "可用" else "不可用"}")
        }
        sb.appendLine()
        
        sb.appendLine("漏洞状态: ${if (isVulnerabilityAvailable()) "可用" else "不可用"}")
        sb.appendLine("全文件权限: ${Environment.isExternalStorageManager()}")
        sb.appendLine("可用路径: ${getWorkingBypassPath() ?: "无"}")
        
        return sb.toString()
    }
}
