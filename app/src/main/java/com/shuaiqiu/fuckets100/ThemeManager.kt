package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

/**
 * 应用主题枚举类
 * 定义了莫奈系列和黑白系列两种主题配色
 */
enum class AppTheme(
    val displayName: String,
    val primary: Color,
    val primaryContainer: Color,
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onPrimary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val error: Color,
    val errorContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val isDark: Boolean
) {
    // ============ 莫奈系列（彩色版） ============
    MONET_PURPLE("梦幻紫", 
        Color(0xFFD0BCFF), Color(0xFF4E3D76), 
        Color(0xFF1E1E1E), Color(0xFF2A2640), Color(0xFF1E1E2E), Color(0xFF252540), Color(0xFF303055), 
        Color(0xFF37265E), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_SKY("天空蓝", 
        Color(0xFFA8C8FF), Color(0xFF3D5A80), 
        Color(0xFF1E1E1E), Color(0xFF1E2530), Color(0xFF1E2A3F), Color(0xFF253650), Color(0xFF2D4560), 
        Color(0xFF2A3F55), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_MINT("薄荷绿", 
        Color(0xFFA8E6CF), Color(0xFF3D6B5A), 
        Color(0xFF1E1E1E), Color(0xFF1E2A25), Color(0xFF1E3530), Color(0xFF254040), Color(0xFF2D4D4D), 
        Color(0xFF2A4D3D), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_CORAL("珊瑚橙", 
        Color(0xFFFFB4A2), Color(0xFF805040), 
        Color(0xFF1E1E1E), Color(0xFF2A2520), Color(0xFF352F28), Color(0xFF403530), Color(0xFF4D3D35), 
        Color(0xFF553320), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_SAKURA("樱花粉", 
        Color(0xFFFFB7C5), Color(0xFF804A58), 
        Color(0xFF1E1E1E), Color(0xFF2A2528), Color(0xFF352530), Color(0xFF402D38), Color(0xFF4D3542), 
        Color(0xFF553040), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_SUNFLOWER("向日葵", 
        Color(0xFFFFD93D), Color(0xFF807030), 
        Color(0xFF1E1E1E), Color(0xFF2A2820), Color(0xFF353025), Color(0xFF403828), Color(0xFF4D4530), 
        Color(0xFF554D20), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_LAVENDER("薰衣草", 
        Color(0xFFC9B1FF), Color(0xFF5A4D80), 
        Color(0xFF1E1E1E), Color(0xFF252230), Color(0xFF2D2840), Color(0xFF353050), Color(0xFF404060), 
        Color(0xFF3D3555), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_TEAL("青碧色", 
        Color(0xFF80CBC4), Color(0xFF3D6360), 
        Color(0xFF1E1E1E), Color(0xFF1E2826), Color(0xFF253330), Color(0xFF2A3E3C), Color(0xFF334B49), 
        Color(0xFF2A4442), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_AMBER("琥珀金", 
        Color(0xFFFFCC80), Color(0xFF806040), 
        Color(0xFF1E1E1E), Color(0xFF2A2520), Color(0xFF352D28), Color(0xFF403530), Color(0xFF4D3D38), 
        Color(0xFF55402A), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),
    MONET_ROSE("玫瑰红", 
        Color(0xFFE57373), Color(0xFF803D3D), 
        Color(0xFF1E1E1E), Color(0xFF282020), Color(0xFF302528), Color(0xFF382D30), Color(0xFF423538), 
        Color(0xFF552A2A), Color(0xFFE5E1E7), Color(0xFFCBC4D2), Color(0xFF494551), 
        Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), true),

    // ============ 黑白系列（灰度版本） ============
    MONO_SILVER_LIGHT("银灰亮", 
        Color(0xFF424242), Color(0xFFE0E0E0), 
        Color(0xFFFAFAFA), Color(0xFFF5F5F5), Color(0xFFEFEFEF), Color(0xFFE8E8E8), Color(0xFFDCDCDC), 
        Color(0xFF212121), Color(0xFFE0E0E0), Color(0xFF616161), Color(0xFFBDBDBD), 
        Color(0xFFD32F2F), Color(0xFFFFCDD2), Color(0xFF424242), Color(0xFFE0E0E0), false),
    MONO_CHARCOAL_DARK("墨灰深", 
        Color(0xFFBDBDBD), Color(0xFF424242), 
        Color(0xFF121212), Color(0xFF1A1A1A), Color(0xFF222222), Color(0xFF2A2A2A), Color(0xFF333333), 
        Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFF616161), Color(0xFFFFB4AB), 
        Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), Color(0xFF424242), true),
    MONO_CHARCOAL_LIGHT("炭灰亮", 
        Color(0xFF424242), Color(0xFF2D2D2D), 
        Color(0xFFF5F5F5), Color(0xFFECECEC), Color(0xFFE2E2E2), Color(0xFFD8D8D8), Color(0xFFCCCCCC), 
        Color(0xFF212121), Color(0xFF757575), Color(0xFFBDBDBD), Color(0xFFD32F2F), 
        Color(0xFFFFCDD2), Color(0xFF424242), Color(0xFFE0E0E0), Color(0xFF424242), false),
    MONO_PURE_DARK("纯黑", 
        Color(0xFF757575), Color(0xFF1A1A1A), 
        Color(0xFF0A0A0A), Color(0xFF121212), Color(0xFF1A1A1A), Color(0xFF222222), Color(0xFF2A2A2A), 
        Color(0xFFE0E0E0), Color(0xFF757575), Color(0xFF424242), Color(0xFFFFB4AB), 
        Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), Color(0xFF2A2A2A), true),
    MONO_SILVER_DARK("银灰深", 
        Color(0xFFE0E0E0), Color(0xFF424242), 
        Color(0xFF1A1A1A), Color(0xFF202020), Color(0xFF282828), Color(0xFF303030), Color(0xFF383838), 
        Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFF616161), Color(0xFFFFB4AB), 
        Color(0xFF93000A), Color(0xFFFFB4AB), Color(0xFF6F3A34), Color(0xFF424242), true)
}

/**
 * 主题管理器
 */
object ThemeManager {
    private const val PREFS_NAME = "fe_settings"
    private const val KEY_THEME = "app_theme"
    
    private var prefs: SharedPreferences? = null
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getSavedTheme(): AppTheme {
        val themeName = prefs?.getString(KEY_THEME, AppTheme.MONET_PURPLE.name) ?: AppTheme.MONET_PURPLE.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.MONET_PURPLE
        }
    }
    
    fun saveTheme(theme: AppTheme) {
        prefs?.edit()?.putString(KEY_THEME, theme.name)?.apply()
    }
    
    // 获取莫奈系列主题
    fun getMonetThemes(): List<AppTheme> = AppTheme.values().filter { it.name.startsWith("MONET_") }
    
    // 获取黑白系列主题
    fun getMonoThemes(): List<AppTheme> = AppTheme.values().filter { it.name.startsWith("MONO_") }
}