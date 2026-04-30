package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

/**
 * Shizuku 状态监控与管理核心
 * 用于检测和管理 Shizuku 运行状态及权限申请
 */
object ShizukuManager {
    
    private const val TAG = "ShizukuManager"
    
    // Shizuku 下载地址
    const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
    
    // Shizuku 权限请求码
    const val REQUEST_CODE_SHIZUKU_PERMISSION = 1001
    
    // Shizuku 状态监听器列表
    private val binderReceivedListeners = mutableListOf<() -> Unit>()
    private val binderDeadListeners = mutableListOf<() -> Unit>()
    private val permissionResultListeners = mutableListOf<(Int, Int) -> Unit>()
    
    // 权限授权状态
    private var _permissionGranted = false
    val permissionGranted: Boolean get() = _permissionGranted
    
    /**
     * 检查 Shizuku 运行时是否正常
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查是否为 Sui (Magisk 模块)
     */
    fun isSui(): Boolean = FeApplication.isSui
    
    /**
     * 获取运行时类型描述
     */
    fun getRuntimeType(): String {
        return when {
            isSui() -> "Sui (Magisk)"
            isShizukuRunning() -> when (getUid()) {
                0 -> "Shizuku (Root)"
                2000 -> "Shizuku (ADB)"
                else -> "Shizuku (UID: ${getUid()})"
            }
            else -> "Not Available"
        }
    }
    
    /**
     * 获取 Shizuku/Sui 的 UID
     * 0 = Root, 2000 = ADB Shell
     */
    fun getUid(): Int = FeApplication.shizukuUid
    
    /**
     * 获取 Shizuku 版本号
     */
    fun getVersion(): Int = FeApplication.shizukuVersion
    
    /**
     * 检查 Shizuku 版本是否低于 11
     */
    fun isPreV11(): Boolean {
        return try {
            Shizuku.isPreV11()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查自身权限
     */
    fun checkSelfPermission(): Int {
        return try {
            Shizuku.checkSelfPermission()
        } catch (e: Exception) {
            Log.e(TAG, "检查自身权限失败", e)
            PackageManager.PERMISSION_DENIED
        }
    }
    
    /**
     * 检查是否需要显示权限申请说明
     */
    fun shouldShowRequestPermissionRationale(): Boolean {
        return try {
            Shizuku.shouldShowRequestPermissionRationale()
        } catch (e: Exception) {
            Log.e(TAG, "检查权限申请说明失败", e)
            false
        }
    }
    
    /**
     * 请求 Shizuku 权限
     */
    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "请求权限失败", e)
        }
    }
    
    /**
     * 添加 Binder 连接监听器
     */
    fun addBinderReceivedListener(listener: () -> Unit) {
        binderReceivedListeners.add(listener)
        Shizuku.addBinderReceivedListenerSticky {
            listener()
        }
    }
    
    /**
     * 添加 Binder 断开监听器
     */
    fun addBinderDeadListener(listener: () -> Unit) {
        binderDeadListeners.add(listener)
        Shizuku.addBinderDeadListener {
            listener()
        }
    }
    
    /**
     * 添加权限申请结果监听器
     */
    fun addPermissionResultListener(listener: (Int, Int) -> Unit) {
        permissionResultListeners.add(listener)
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            _permissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
            listener(requestCode, grantResult)
        }
    }
    
    /**
     * 移除所有监听器
     */
    fun removeAllListeners() {
        binderReceivedListeners.forEach { listener ->
            Shizuku.removeBinderReceivedListener(listener)
        }
        binderReceivedListeners.clear()
        
        binderDeadListeners.forEach { listener ->
            Shizuku.removeBinderDeadListener(listener)
        }
        binderDeadListeners.clear()
        
        permissionResultListeners.forEach { listener ->
            Shizuku.removeRequestPermissionResultListener(listener)
        }
        permissionResultListeners.clear()
    }
    
    /**
     * 打开 Shizuku 下载页面
     */
    fun openShizukuDownloadPage(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD_URL))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * 打开 Shizuku 应用（如果已安装），否则打开下载页面
     */
    fun openShizukuApp(context: Context) {
        try {
            val packageName = "moe.shizuku.privileged.api"
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                // 未找到应用，打开下载页面
                openShizukuDownloadPage(context)
            }
        } catch (e: Exception) {
            openShizukuDownloadPage(context)
        }
    }
    
    /**
     * 使用 Shizuku API 执行命令
     * 宝贝这是修复后的方法，使用反射调用 Shizuku 的 private newProcess API 执行命令喵！
     * @param command 要执行的命令
     * @return 命令输出结果，失败时返回 null
     */
    fun execCommand(command: String): String? {
        return try {
            // 检查 Shizuku 是否可用
            if (!isShizukuRunning()) {
                Log.w(TAG, "execCommand: Shizuku is not running")
                return null
            }
            
            // 检查权限
            if (checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "execCommand: Shizuku permission not granted")
                return null
            }
            
            // 宝贝使用反射调用 Shizuku 的 private newProcess 方法喵~
            // newProcess 签名: public static ShizukuRemoteProcess newProcess(String[] cmd, String[] env, String dir)
            val newProcessMethod: Method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            
            @Suppress("UNCHECKED_CAST")
            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null  // dir 参数为 null
            ) as ShizukuRemoteProcess
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (output.isNotEmpty()) output.append("\n")
                output.append(line)
            }
            
            // 等待进程结束并检查错误
            val exitCode = process.waitFor()
            
            // 读取错误输出
            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                if (errorOutput.isNotEmpty()) errorOutput.append("\n")
                errorOutput.append(line)
            }
            
            reader.close()
            errorReader.close()
            process.destroy()
            
            if (exitCode != 0 && errorOutput.isNotEmpty()) {
                Log.w(TAG, "execCommand stderr: $errorOutput")
            }
            
            val result = output.toString()
            Log.d(TAG, "execCommand: '$command' -> exitCode=$exitCode, outputLength=${result.length}")
            result.ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "execCommand failed: ${e.message}", e)
            // 打印完整的异常堆栈帮助调试
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 使用 Shizuku 执行多条命令
     * 宝贝这里支持一次性执行多条命令喵~
     */
    fun execMultipleCommands(commands: List<String>): List<String?> {
        return commands.map { execCommand(it) }
    }
    
    /**
     * 获取当前激活模式
     */
    fun getCurrentActivationMode(): ActivationMode {
        return when {
            // 优先检测 Sui (Magisk 模块)
            isSui() -> ActivationMode.SHIZUKU
            // 检测 Shizuku 运行时
            isShizukuRunning() -> ActivationMode.SHIZUKU
            // 检测 Root 运行时（传统 RootManager 方式）
            RootManager.isRootAvailable() -> ActivationMode.ROOT
            // 默认无特殊权限
            else -> ActivationMode.DEFAULT
        }
    }
    
    /**
     * 检测 Root 运行时是否可用（传统方式）
     * @deprecated 请使用 RootManager.isRootAvailable()
     */
    @Deprecated("请使用 RootManager.isRootAvailable()")
    private fun isRootAvailable(): Boolean {
        return RootManager.isRootAvailable()
    }
}

/**
 * Composable 状态管理 - 监听 Shizuku 状态变化
 */
@Composable
fun rememberShizukuState(): ShizukuState {
    var isRunning by remember { mutableStateOf(ShizukuManager.isShizukuRunning()) }
    var isSui by remember { mutableStateOf(ShizukuManager.isSui()) }
    var permissionGranted by remember { mutableStateOf(ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) }
    var uid by remember { mutableStateOf(ShizukuManager.getUid()) }
    var version by remember { mutableStateOf(ShizukuManager.getVersion()) }
    
    DisposableEffect(Unit) {
        val onBinderReceived = {
            isRunning = true
            isSui = ShizukuManager.isSui()
            uid = ShizukuManager.getUid()
            version = ShizukuManager.getVersion()
        }
        
        val onBinderDead = {
            isRunning = false
            uid = -1
        }
        
        val onPermissionResult: (Int, Int) -> Unit = { _, grantResult ->
            permissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
        }
        
        ShizukuManager.addBinderReceivedListener(onBinderReceived)
        ShizukuManager.addBinderDeadListener(onBinderDead)
        ShizukuManager.addPermissionResultListener(onPermissionResult)
        
        onDispose {
            // 注意：此处不清理监听器，因为 ShizukuManager 本身会管理生命周期
            // 当应用销毁时，ShizukuManager 会自动清理所有注册的监听器
        }
    }
    
    return ShizukuState(
        isRunning = isRunning,
        isSui = isSui,
        permissionGranted = permissionGranted,
        uid = uid,
        version = version
    )
}

/**
 * Shizuku 状态数据类
 */
data class ShizukuState(
    val isRunning: Boolean,
    val isSui: Boolean,
    val permissionGranted: Boolean,
    val uid: Int,
    val version: Int
) {
    /**
     * 获取运行时类型名称
     */
    fun getRuntimeTypeName(): String {
        return when {
            isSui -> "Sui (Magisk)"
            isRunning -> when (uid) {
                0 -> "Shizuku (Root)"
                2000 -> "Shizuku (ADB)"
                else -> "Shizuku (UID: $uid)"
            }
            else -> "未连接"
        }
    }
    
    /**
     * 获取权限状态描述
     */
    fun getPermissionStatus(): String {
        return when {
            !isRunning -> "Shizuku 未运行"
            permissionGranted -> "已授权"
            else -> "未授权"
        }
    }
}
