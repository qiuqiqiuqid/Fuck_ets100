package com.shauiqiu.fe

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Shizuku 状态管理器
 * 提供响应式的 Shizuku 连接状态和操作
 */
object ShizukuManager {
    
    private const val TAG = "ShizukuManager"
    
    // Shizuku 下载页面
    const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
    
    // 权限请求码
    const val REQUEST_CODE_SHIZUKU_PERMISSION = 1001
    
    // Shizuku 监听器
    private val binderReceivedListeners = mutableListOf<() -> Unit>()
    private val binderDeadListeners = mutableListOf<() -> Unit>()
    private val permissionResultListeners = mutableListOf<(Int, Int) -> Unit>()
    
    // 权限是否已授予
    private var _permissionGranted = false
    val permissionGranted: Boolean get() = _permissionGranted
    
    /**
     * 检查 Shizuku 是否正在运行
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查是否是 Sui (Magisk 模块)
     */
    fun isSui(): Boolean = FeApplication.isSui
    
    /**
     * 获取当前运行时类型描述
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
     * 获取 Shizuku 版本
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
            Log.e(TAG, "Error checking self permission", e)
            PackageManager.PERMISSION_DENIED
        }
    }
    
    /**
     * 检查是否应该显示权限请求理由
     */
    fun shouldShowRequestPermissionRationale(): Boolean {
        return try {
            Shizuku.shouldShowRequestPermissionRationale()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permission rationale", e)
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
            Log.e(TAG, "Error requesting permission", e)
        }
    }
    
    /**
     * 添加 Binder 接收监听器
     */
    fun addBinderReceivedListener(listener: () -> Unit) {
        binderReceivedListeners.add(listener)
        Shizuku.addBinderReceivedListenerSticky {
            listener()
        }
    }
    
    /**
     * 添加 Binder 死亡监听器
     */
    fun addBinderDeadListener(listener: () -> Unit) {
        binderDeadListeners.add(listener)
        Shizuku.addBinderDeadListener {
            listener()
        }
    }
    
    /**
     * 添加权限请求结果监听器
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
     * 跳转到 Shizuku 下载页面
     */
    fun openShizukuDownloadPage(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD_URL))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * 跳转到 Shizuku 应用页面（如果已安装）
     */
    fun openShizukuApp(context: Context) {
        try {
            val packageName = "moe.shizuku.privileged.api"
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                // 如果没有安装，跳转到下载页面
                openShizukuDownloadPage(context)
            }
        } catch (e: Exception) {
            openShizukuDownloadPage(context)
        }
    }
    
    /**
     * 判断当前激活模式
     */
    fun getCurrentActivationMode(): ActivationMode {
        return when {
            // 首先检查 Sui (Magisk 模块)
            isSui() -> ActivationMode.SHIZUKU
            // 检查 Shizuku 是否连接
            isShizukuRunning() -> ActivationMode.SHIZUKU
            // 检查是否有 Root (使用 RootManager 进行真实检测)
            RootManager.isRootAvailable() -> ActivationMode.ROOT
            // 默认未激活
            else -> ActivationMode.DEFAULT
        }
    }
    
    /**
     * 检查 Root 是否可用 (使用 RootManager 进行真实检测)
     * @deprecated 请使用 RootManager.isRootAvailable() 代替
     */
    @Deprecated("使用 RootManager.isRootAvailable() 代替")
    private fun isRootAvailable(): Boolean {
        return RootManager.isRootAvailable()
    }
}

/**
 * Composable 状态收集器 - 收集 Shizuku 连接状态
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
            // 注意：由于 ShizukuManager 使用单例，监听器不会被移除
            // 如果需要正确的生命周期管理，需要重构 ShizukuManager
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
     * 获取运行时类型描述
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
