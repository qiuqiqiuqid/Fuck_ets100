package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class AnswerDisplayMode(val label: String) {
    SHORTEST("显示最短答案"),
    ALL("显示全部答案")
}

object SettingsManager {
    
    private const val PREFS_NAME = "fe_settings"
    private const val KEY_ACTIVATION_MODE = "activation_mode"
    private const val KEY_ANSWER_DISPLAY_MODE = "answer_display_mode"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveActivationMode(mode: ActivationMode) {
        prefs.edit {
            putString(KEY_ACTIVATION_MODE, mode.name)
        }
    }
    
    fun getSavedActivationMode(): ActivationMode? {
        val modeName = prefs.getString(KEY_ACTIVATION_MODE, null) ?: return null
        return try {
            ActivationMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    fun hasUserSelectedMode(): Boolean {
        return prefs.contains(KEY_ACTIVATION_MODE)
    }
    
    fun clearActivationMode() {
        prefs.edit {
            remove(KEY_ACTIVATION_MODE)
        }
    }
    
    fun saveAnswerDisplayMode(mode: AnswerDisplayMode) {
        prefs.edit {
            putString(KEY_ANSWER_DISPLAY_MODE, mode.name)
        }
    }
    
    fun getAnswerDisplayMode(): AnswerDisplayMode {
        val modeName = prefs.getString(KEY_ANSWER_DISPLAY_MODE, null) ?: return AnswerDisplayMode.SHORTEST
        return try {
            AnswerDisplayMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            AnswerDisplayMode.SHORTEST
        }
    }
}
