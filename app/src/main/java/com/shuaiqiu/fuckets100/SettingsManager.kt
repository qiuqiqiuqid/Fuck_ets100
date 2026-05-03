package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 设置管理器
 * 负责保存和读取应用的各种设置选项喵~
 */
object SettingsManager {
    
    private const val PREFS_NAME = "fe_settings"
    private const val KEY_ACTIVATION_MODE = "activation_mode"
    private const val KEY_DEBUG_MODE = "debug_mode"  // 调试模式开关
    private const val KEY_FORCE_READ_MODE = "force_read_mode"  // 强执读取模式
    private const val KEY_HIDE_DEBUG_BUTTON = "hide_debug_button"  // 隐藏调试按钮
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 初始化设置管理器，在 Application 启动时调用
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存激活模式设置
     */
    fun saveActivationMode(mode: ActivationMode) {
        prefs.edit {
            putString(KEY_ACTIVATION_MODE, mode.name)
        }
    }
    
    /**
     * 读取保存的激活模式设置
     * 返回 null 表示用户尚未选择激活模式
     */
    fun getSavedActivationMode(): ActivationMode? {
        val modeName = prefs.getString(KEY_ACTIVATION_MODE, null) ?: return null
        return try {
            ActivationMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    /**
     * 检查用户是否已经选择了激活模式
     */
    fun hasUserSelectedMode(): Boolean {
        return prefs.contains(KEY_ACTIVATION_MODE)
    }
    
    /**
     * 清除保存的激活模式设置
     */
    fun clearActivationMode() {
        prefs.edit {
            remove(KEY_ACTIVATION_MODE)
        }
    }
    
    /**
     * 喵~ 保存调试模式开关（默认开启）
     */
    fun saveDebugMode(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_DEBUG_MODE, enabled)
        }
    }
    
    /**
     * 喵~ 获取调试模式开关（默认 true）
     */
    fun getDebugMode(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_MODE, true)
    }
    
    /**
     * 喵~ 保存强执读取模式开关（默认关闭）
     */
    fun saveForceReadMode(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_FORCE_READ_MODE, enabled)
        }
    }
    
    /**
     * 喵~ 获取强执读取模式开关（默认 false）
     */
    fun getForceReadMode(): Boolean {
        return prefs.getBoolean(KEY_FORCE_READ_MODE, false)
    }
    
    /**
     * 喵~ 保存隐藏调试按钮开关（默认 false - 默认显示调试按钮）
     */
    fun saveHideDebugButton(hide: Boolean) {
        prefs.edit {
            putBoolean(KEY_HIDE_DEBUG_BUTTON, hide)
        }
    }
    
    /**
     * 喵~ 获取隐藏调试按钮开关（默认 false - 调试按钮默认显示）
     */
    fun getHideDebugButton(): Boolean {
        return prefs.getBoolean(KEY_HIDE_DEBUG_BUTTON, false)
    }
}