package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object PermissionsHelper {
    private const val TAG = "PermissionsHelper"

    /**
     * 检查是否拥有所有文件访问权限
     */
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // 低于Android 11默认有权限
        }
    }

    /**
     * 请求所有文件访问权限(Android 11+)
     */
    fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            } else {
                Toast.makeText(context, "很好~ 文件访问权限已经授权啦", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 检查是否拥有悬浮窗权限
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestOverlayPermission(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "太棒了~ 悬浮窗权限已经授权了呢", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 检查是否拥有应用列表权限(QUERY_ALL_PACKAGES)
     * 这个权限用于获取已安装应用列表来判断是否为系统环境
     */
    fun hasAppListPermission(): Boolean {
        return try {
            val pm = FeApplication.appCtx.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            
            val packageCount = packages.size
            val hasSystemApps = packages.any { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg.packageName, 0)
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (e: Exception) {
                    false
                }
            }
            
            val isValid = packageCount >= 10 && hasSystemApps
            
            if (!isValid) {
                Log.w(TAG, "应用列表权限异常: packageCount=$packageCount, hasSystemApps=$hasSystemApps")
            }
            
            isValid
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: 需要 QUERY_ALL_PACKAGES 权限", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "获取应用列表权限失败: ${e.message}", e)
            false
        }
    }

    /**
     * 请求应用列表权限
     * 这个权限比较特殊,需要使用特殊方法请求
     */
    fun requestAppListPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "无法打开应用详情页面: ${e.message}", e)
            Toast.makeText(context, "无法打开应用详情页面了..", Toast.LENGTH_SHORT).show()
        }
    }
}