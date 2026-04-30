package com.shuaiqiu.fuckets100

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.sui.Sui

/**
 * Fe 应用 Application 类
 * 负责初始化 Sui (Magisk 模块) 和 Shizuku
 */
class FeApplication : Application() {
    
    // 应用范围的协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "FeApplication"
        
        // 应用是否是 Sui (Magisk 模块) - 初始化为null在onCreate时设置
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
        
        // 应用全局上下文
        lateinit var appCtx: Context
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 设置应用全局上下文
        appCtx = this.applicationContext
        
        // 初始化设置管理器
        SettingsManager.init(this)
        
        // 初始化SAF管理器
        SAFManager.init(this)
        
        // 初始化Sui (Magisk 模块)
        isSui = Sui.init(BuildConfig.APPLICATION_ID)
        Log.d(TAG, "Sui initialized: $isSui")
        
        // 设置Shizuku监听器
        setupShizukuListeners()
        
        // 检查远程配置
        checkRemoteConfig()
        
        Log.d(TAG, "FeApplication onCreate | isSui: $isSui")
    }
    
    /**
     * 检查远程配置
     * 包含KillSwitch检查和提示消息显示,控制应用是否显示"程序异常"提示
     * 宝贝如果网络失败或超时会闪退喵~
     */
    private fun checkRemoteConfig() {
        applicationScope.launch {
            try {
                val (isKillSwitch, showNotice, noticeMessage) = RemoteConfigManager.checkStatus()
                
                when {
                    isKillSwitch -> {
                        // KillSwitch 激活,显示"程序异常"提示
                        Log.w(TAG, "KillSwitch activated: 程序异常")
                        Toast.makeText(this@FeApplication, "程序异常", Toast.LENGTH_LONG).show()
                        
                        // 延迟关闭应用
                        android.os.Handler(mainLooper).postDelayed({
                            // 关闭应用
                            android.os.Process.killProcess(android.os.Process.myPid())
                            System.exit(0)
                        }, 2000) // 2秒后关闭
                    }
                    showNotice && noticeMessage.isNotEmpty() -> {
                        // 显示通知消息
                        Log.i(TAG, "显示通知: $noticeMessage")
                        Toast.makeText(this@FeApplication, noticeMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: RemoteConfigManager.NetworkException) {
                // 网络失败或超时，直接闪退喵~
                Log.e(TAG, "❌ 远程配置获取失败: ${e.message}")
                Toast.makeText(this@FeApplication, "网络异常", Toast.LENGTH_SHORT).show()
                
                android.os.Handler(mainLooper).postDelayed({
                    // 关闭应用
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(0)
                }, 1000) // 1秒后关闭
            }
        }
    }
    
    private fun setupShizukuListeners() {
        // 监听Binder接收事件 - 收到binder后调用onShizukuBinderReceived
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
     * 检查Shizuku是否可用
     */
    fun staticCheckShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取Shizuku运行时类型
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