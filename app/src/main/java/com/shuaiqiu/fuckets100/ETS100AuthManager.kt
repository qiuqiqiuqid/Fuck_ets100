package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * ETS100 认证管理器
 * 负责登录状态和 Token 的存储管理喵~
 * 
 * 使用 SharedPreferences 保存：
 * - device_code: 机器码（首次随机生成，后续固定）
 * - phone: 登录的手机号
 * - token: 登录返回的 token
 * - parent_account_id: 父账户 ID
 */
object ETS100AuthManager {

    private const val TAG = "ETS100AuthManager"
    private const val PREFS_NAME = "ets100_auth"

    // ============================================================================
    // 认证信息 Key
    // ============================================================================
    
    private object Keys {
        const val DEVICE_CODE = "device_code"
        const val PHONE = "phone"
        const val TOKEN = "token"
        const val PARENT_ACCOUNT_ID = "parent_account_id"
        const val IS_LOGGED_IN = "is_logged_in"
    }

    // ============================================================================
    // SharedPreferences 实例（延迟初始化）
    // ============================================================================
    
    @Volatile
    private var prefsInstance: SharedPreferences? = null
    
    private fun getPrefs(context: Context): SharedPreferences {
        return prefsInstance ?: synchronized(this) {
            prefsInstance ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also {
                prefsInstance = it
            }
        }
    }

    // ============================================================================
    // 机器码管理
    // ============================================================================

    /**
     * 获取机器码
     * 如果不存在则首次随机生成，后续使用保存的值喵~
     * 
     * 机器码格式: data_md5|mac_md5 (各16字符，共33字符)
     */
    fun getDeviceCode(context: Context): String {
        val prefs = getPrefs(context)
        val savedCode = prefs.getString(Keys.DEVICE_CODE, null)
        
        if (savedCode != null) {
            Log.d(TAG, "使用已保存的机器码: ${savedCode.take(8)}...")
            return savedCode
        }
        
        // 首次生成：随机生成设备信息部分
        val deviceInfo = generateRandomHex(16)  // 16字符的随机hex
        val macAddress = generateRandomHex(16)  // 16字符的随机hex
        
        // 计算 MD5 并取特定范围
        val dataMd5 = md5Substring(deviceInfo, 8, 24)
        val macMd5 = md5Substring(macAddress, 8, 24)
        val newCode = "$dataMd5|$macMd5"
        
        // 保存
        prefs.edit().putString(Keys.DEVICE_CODE, newCode).apply()
        Log.d(TAG, "首次生成机器码: ${newCode.take(8)}...")
        
        return newCode
    }

    /**
     * 生成随机十六进制字符串
     */
    private fun generateRandomHex(length: Int): String {
        val chars = "0123456789ABCDEF"
        return (1..length).map { chars.random() }.joinToString("")
    }

    /**
     * MD5 并截取子串
     */
    private fun md5Substring(input: String, start: Int, end: Int): String {
        val md5Hash = md5(input)
        return md5Hash.substring(start, end)
    }

    /**
     * 计算 MD5
     */
    private fun md5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ============================================================================
    // 登录信息管理
    // ============================================================================

    /**
     * 保存登录信息
     * 喵~ 登录成功后调用喵！
     */
    fun saveLoginInfo(context: Context, phone: String, token: String, parentAccountId: String) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putString(Keys.PHONE, phone)
            putString(Keys.TOKEN, token)
            putString(Keys.PARENT_ACCOUNT_ID, parentAccountId)
            putBoolean(Keys.IS_LOGGED_IN, true)
            apply()
        }
        Log.d(TAG, "保存登录信息成功: phone=$phone")
    }

    /**
     * 获取 Token
     * 喵~ 没有 Token 返回 null 喵！
     */
    fun getToken(context: Context): String? {
        val prefs = getPrefs(context)
        return prefs.getString(Keys.TOKEN, null)
    }

    /**
     * 获取父账户 ID
     */
    fun getParentAccountId(context: Context): String? {
        val prefs = getPrefs(context)
        return prefs.getString(Keys.PARENT_ACCOUNT_ID, null)
    }

    /**
     * 获取登录手机号
     */
    fun getPhone(context: Context): String? {
        val prefs = getPrefs(context)
        return prefs.getString(Keys.PHONE, null)
    }

    /**
     * 检查是否已登录
     * 喵~ 需要同时有 Token 和 ParentAccountId 才算已登录喵！
     */
    fun isLoggedIn(context: Context): Boolean {
        val prefs = getPrefs(context)
        val token = prefs.getString(Keys.TOKEN, null)
        val parentId = prefs.getString(Keys.PARENT_ACCOUNT_ID, null)
        return !token.isNullOrEmpty() && !parentId.isNullOrEmpty()
    }

    /**
     * 登出
     * 喵~ 清除所有登录信息，但保留机器码喵！
     */
    fun logout(context: Context) {
        val prefs = getPrefs(context)
        val deviceCode = prefs.getString(Keys.DEVICE_CODE, null)  // 保留机器码
        
        prefs.edit().clear().apply()
        
        // 恢复机器码
        if (deviceCode != null) {
            prefs.edit().putString(Keys.DEVICE_CODE, deviceCode).apply()
        }
        
        Log.d(TAG, "已登出，机器码已保留")
    }

    /**
     * 清除所有数据
     * 喵~ 包括机器码也清除，用于完全重置喵！
     */
    fun clearAll(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()
        Log.d(TAG, "已清除所有认证数据")
    }
}