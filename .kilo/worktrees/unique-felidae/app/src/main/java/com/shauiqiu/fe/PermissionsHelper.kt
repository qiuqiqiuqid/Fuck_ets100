package com.shauiqiu.fe

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

    /** 检查是否拥有「所有文件访问」权限 */
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // 旧版本默认通过
        }
    }

    /** 获取「所有文件访问」权限 (Android 11+) */
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
                Toast.makeText(context, "喵~ 已经拥有全文件访问权限啦！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 检查是否拥有「悬浮窗」权限 */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /** 获取「悬浮窗」权限 */
    fun requestOverlayPermission(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "呜~ 悬浮窗权限已经开启咯！", Toast.LENGTH_SHORT).show()
        }
    }

    /** 检查是否拥有应用列表权限 (QUERY_ALL_PACKAGES)
     * 通过实际调用 PackageManager.getInstalledPackages 来验证权限是否真正有效
     * 支持国产定制系统
     */
    fun hasAppListPermission(): Boolean {
        return try {
            val pm = FeApplication.appCtx.packageManager
            // 使用 getInstalledPackages 进行验证（可获取完整应用列表）
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            
            // 多维度验证权限是否有效
            val packageCount = packages.size
            // 检查是否包含系统应用（验证是否获取到完整列表）
            val hasSystemApps = packages.any { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg.packageName, 0)
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (e: Exception) {
                    false
                }
            }
            
            // 判断标准：至少10个应用 + 必须包含系统应用
            val isValid = packageCount >= 10 && hasSystemApps
            
            if (!isValid) {
                Log.w(TAG, "应用列表权限验证失败: packageCount=$packageCount, hasSystemApps=$hasSystemApps")
            }
            
            isValid
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: 缺少 QUERY_ALL_PACKAGES 权限", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "获取应用列表失败: ${e.message}", e)
            false
        }
    }

    /** 获取「应用列表」权限
     * 跳转到应用详情页面，让用户手动开启 QUERY_ALL_PACKAGES 权限
     * 该权限位于「安装未知应用」或「特殊权限」设置中
     */
    fun requestAppListPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "无法打开应用详情页面: ${e.message}", e)
            Toast.makeText(context, "无法打开应用详情页面喵...", Toast.LENGTH_SHORT).show()
        }
    }
}
