package com.shauiqiu.fe

import android.app.Application
import android.content.Context
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.sui.Sui

/**
 * Fe 应用 Application 类
 * 用于初始化 Sui (Magisk 模块) 和 Shizuku
 */
class FeApplication : Application() {

    companion object {
        private const val TAG = "FeApplication"
        
        // 是否使用 Sui (Magisk 模块) - 初始为 null，在 onCreate 中初始化
        var isSui: Boolean = false
            private set
        
        // Shizuku 是否已连接
        var isShizukuBinderReceived = false
            private set
        
        // Shizuku/Sui 的 UID (0 = Root, 2000 = ADB Shell)
        var shizukuUid = -1
            private set
        
        // Shizuku 版本
        var shizukuVersion = -1
            private set
        
        // 应用上下文引用
        lateinit var appCtx: Context
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 保存应用上下文引用
        appCtx = this.applicationContext
        
        // 初始化设置管理器
        SettingsManager.init(this)
        
        // 初始化 SAF 管理器
        SAFManager.init(this)
        
        // 初始化 Sui (Magisk 模块)
        isSui = Sui.init(BuildConfig.APPLICATION_ID)
        Log.d(TAG, "Sui initialized: $isSui")
        
        // 设置 Shizuku 状态回调
        setupShizukuListeners()
        
        Log.d(TAG, "FeApplication onCreate | isSui: $isSui")
    }
    
    private fun setupShizukuListeners() {
        // 添加粘性监听器 - 如果 binder 已经存在会立即回调
        Shizuku.addBinderReceivedListenerSticky {
            onShizukuBinderReceived()
        }
        
        Shizuku.addBinderDeadListener {
            onShizukuBinderDead()
        }
    }
    
    private fun onShizukuBinderReceived() {
        isShizukuBinderReceived = true
        
        try {
            shizukuVersion = Shizuku.getVersion()
            shizukuUid = Shizuku.getUid()
            
            Log.d(TAG, "Shizuku connected! Version: $shizukuVersion, UID: $shizukuUid")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Shizuku info", e)
        }
    }
    
    private fun onShizukuBinderDead() {
        isShizukuBinderReceived = false
        shizukuUid = -1
        Log.d(TAG, "Shizuku disconnected")
    }
    
    /**
     * 检查 Shizuku 是否可用
     */
    fun staticCheckShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取 Shizuku 运行时类型描述
     */
    fun staticGetRuntimeType(): String {
        return when {
            isSui -> "Sui (Magisk)"
            isShizukuBinderReceived -> when (shizukuUid) {
                0 -> "Shizuku (Root)"
                2000 -> "Shizuku (ADB)"
                else -> "Shizuku (UID: $shizukuUid)"
            }
            else -> "Not Available"
        }
    }
}
