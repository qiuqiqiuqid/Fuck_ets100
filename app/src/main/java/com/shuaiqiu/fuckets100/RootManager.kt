package com.shuaiqiu.fuckets100

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Root 权限管理器
 * 提供 Root 权限检测、执行命令等功能
 */
object RootManager {
    
    private const val TAG = "RootManager"
    
    // 缓存 Root 运行时状态，避免重复检测
    private var cachedRootAvailable: Boolean? = null
    
    /**
     * 检测 Root 运行时是否可用
     * 优先使用 `id` 命令检测当前用户是否为 Root 用户
     * @return true 表示拥有 Root 权限的设备已获取 Root 权限
     */
    fun isRootAvailable(): Boolean {
        // 使用缓存避免重复检测
        cachedRootAvailable?.let { return it }
        
        val result = execCommand("id")
        val isRoot = result?.contains("uid=0") == true
        
        cachedRootAvailable = isRoot
        
        if (isRoot) {
            Log.i(TAG, "Root 权限检测成功")
        } else {
            Log.w(TAG, "Root 权限未开启 result=$result")
        }
        
        return isRoot
    }
    
    /**
     * 清除 Root 缓存状态
     * 下次检测时会重新执行 Root 权限检测
     */
    fun clearCache() {
        cachedRootAvailable = null
    }
    
    /**
     * 检测 su binary 是否存在
     * @return true 如果 su 二进制文件存在于常见路径
     */
    fun isSuBinaryExists(): Boolean {
        val paths = arrayOf(
            "/system/xbin/su",
            "/system/bin/su",
            "/vendor/bin/su",
            "/sbin/su"
        )
        
        return paths.any { path ->
            try {
                java.io.File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 使用 Root 权限执行命令
     * @param command 要执行的命令
     * @return 命令输出结果，失败时返回 null
     */
    fun execAsRoot(command: String): String? {
        return try {
            execCommand(command)
        } catch (e: Exception) {
            Log.e(TAG, "执行 Root 命令失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 使用 Root 权限执行多条命令
     * @param commands 要执行的命令列表
     * @return 每条命令的执行结果列表
     */
    fun execMultipleAsRoot(commands: List<String>): List<String?> {
        return commands.map { execAsRoot(it) }
    }
    
    /**
     * 执行命令核心方法
     */
    private fun execCommand(command: String): String? {
        var process: Process? = null
        var outputStream: DataOutputStream? = null
        var inputReader: BufferedReader? = null
        var errorReader: BufferedReader? = null
        
        try {
            // 启动 su 进程
            process = Runtime.getRuntime().exec("su")
            outputStream = DataOutputStream(process.outputStream)
            inputReader = BufferedReader(InputStreamReader(process.inputStream))
            errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // 执行命令
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            // 读取命令输出
            val output = StringBuilder()
            var line: String?
            while (inputReader.readLine().also { line = it } != null) {
                if (output.isNotEmpty()) output.append("\n")
                output.append(line)
            }
            
            // 等待命令执行完成
            process.waitFor()
            
            val result = output.toString().ifEmpty { null }
            
            // 检查 stderr 是否有错误输出
            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                if (errorOutput.isNotEmpty()) errorOutput.append("\n")
                errorOutput.append(line)
            }
            
            if (errorOutput.isNotEmpty()) {
                Log.w(TAG, "命令 stderr: $errorOutput")
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "execCommand 异常: ${e.message}", e)
            return null
        } finally {
            try {
                outputStream?.close()
                inputReader?.close()
                errorReader?.close()
                process?.destroy()
            } catch (e: Exception) {
                // 忽略清理异常
            }
        }
    }
    
    /**
     * 验证 Root 权限是否真正可用
     * @return true 表示可以正常执行 Root 命令
     */
    fun verifyRootAccess(): Boolean {
        val testCommands = listOf(
            "id",
            "whoami",
            "ls /data"
        )
        
        var successCount = 0
        for (command in testCommands) {
            val result = execAsRoot(command)
            if (result != null) {
                successCount++
            }
        }
        
        // 多次成功执行命令才认为 Root 权限真正可用
        return successCount > 0
    }
}
