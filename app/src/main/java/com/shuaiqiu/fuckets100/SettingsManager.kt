package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 猫庐戮莽陆庐莽庐隆莽聬聠氓聶? * 莽聰篓盲潞聨忙聦聛盲鹿聟氓聦聳盲驴聺氓颅聵莽聰篓忙聢路氓聛聫氓楼陆猫庐戮莽陆? */
object SettingsManager {
    
    private const val PREFS_NAME = "fe_settings"
    private const val KEY_ACTIVATION_MODE = "activation_mode"
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 氓聢聺氓搂聥氓聦聳茂录聦氓聹?Application 盲赂颅猫掳聝莽聰?     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 盲驴聺氓颅聵忙驴聙忙麓禄忙篓隆氓录?     */
    fun saveActivationMode(mode: ActivationMode) {
        prefs.edit {
            putString(KEY_ACTIVATION_MODE, mode.name)
        }
    }
    
    /**
     * 猫聨路氓聫聳盲驴聺氓颅聵莽職聞忙驴聙忙麓禄忙篓隆氓录?     * 氓娄聜忙聻聹忙虏隆忙聹聣盲驴聺氓颅聵猫驴聡茂录聦猫驴聰氓聸聻 null
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
     * 忙拢聙忙聼楼莽聰篓忙聢路忙聵炉氓聬娄忙聣聥氓聤篓茅聙聣忙聥漏猫驴聡忙驴聙忙麓禄忙篓隆氓录?     */
    fun hasUserSelectedMode(): Boolean {
        return prefs.contains(KEY_ACTIVATION_MODE)
    }
    
    /**
     * 忙赂聟茅聶陇盲驴聺氓颅聵莽職聞忙驴聙忙麓禄忙篓隆氓录?     */
    fun clearActivationMode() {
        prefs.edit {
            remove(KEY_ACTIVATION_MODE)
        }
    }
}
