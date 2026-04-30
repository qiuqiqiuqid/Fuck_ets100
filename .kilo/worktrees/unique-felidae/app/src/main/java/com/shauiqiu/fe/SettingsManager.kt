package com.shauiqiu.fe

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 设置管理器
 * 用于持久化保存用户偏好设置
 */
object SettingsManager {
    
    private const val PREFS_NAME = "fe_settings"
    private const val KEY_ACTIVATION_MODE = "activation_mode"
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 初始化，在 Application 中调用
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存激活模式
     */
    fun saveActivationMode(mode: ActivationMode) {
        prefs.edit {
            putString(KEY_ACTIVATION_MODE, mode.name)
        }
    }
    
    /**
     * 获取保存的激活模式
     * 如果没有保存过，返回 null
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
     * 检查用户是否手动选择过激活模式
     */
    fun hasUserSelectedMode(): Boolean {
        return prefs.contains(KEY_ACTIVATION_MODE)
    }
    
    /**
     * 清除保存的激活模式
     */
    fun clearActivationMode() {
        prefs.edit {
            remove(KEY_ACTIVATION_MODE)
        }
    }
}
