package com.shauiqiu.fe

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Root 权限管理器
 * 提供真实的 Root 权限检测和命令执行能力
 */
object RootManager {
    
    private const val TAG = "RootManager"
    
    // 缓存的 Root 可用状态，避免重复检测
    private var cachedRootAvailable: Boolean? = null
    
    /**
     * 检查 Root 权限是否可用
     * 通过执行 `su -c id` 命令来验证真实的 Root 权限
     * @return true 如果设备已 Root 且应用已授权 Root 权限
     */
    fun isRootAvailable(): Boolean {
        // 返回缓存结果
        cachedRootAvailable?.let { return it }
        
        val result = execCommand("id")
        val isRoot = result?.contains("uid=0") == true
        
        cachedRootAvailable = isRoot
        
        if (isRoot) {
            Log.i(TAG, "Root 权限检测成功")
        } else {
            Log.w(TAG, "Root 权限不可用: result=$result")
        }
        
        return isRoot
    }
    
    /**
     * 清除 Root 状态缓存
     * 适用于需要重新检测 Root 状态的场景
     */
    fun clearCache() {
        cachedRootAvailable = null
    }
    
    /**
     * 检查 su binary 是否存在（初步检测）
     * 这个方法只检查 su 文件是否存在，不验证实际权限
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
     * 以 Root 权限执行命令
     * @param command 要执行的命令
     * @return 命令输出结果，失败返回 null
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
     * 以 Root 权限执行多条命令
     * @param commands 要执行的命令列表
     * @return 所有命令的输出结果
     */
    fun execMultipleAsRoot(commands: List<String>): List<String?> {
        return commands.map { execAsRoot(it) }
    }
    
    /**
     * 执行命令的核心实现
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
            
            // 读取输出
            val output = StringBuilder()
            var line: String?
            while (inputReader.readLine().also { line = it } != null) {
                if (output.isNotEmpty()) output.append("\n")
                output.append(line)
            }
            
            // 等待命令执行完成
            process.waitFor()
            
            val result = output.toString().ifEmpty { null }
            
            // 检查 stderr 是否有错误
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
                // 忽略关闭异常
            }
        }
    }
    
    /**
     * 验证 Root 权限并获取 Root Shell
     * @return true 如果成功获取 Root Shell
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
        
        // 至少成功执行一个命令即可认为 Root 可用
        return successCount > 0
    }
}
