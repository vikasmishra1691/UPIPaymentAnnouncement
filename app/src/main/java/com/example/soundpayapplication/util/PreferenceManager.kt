package com.example.soundpayapplication.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("soundpay_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ANNOUNCEMENT_ENABLED = "announcement_enabled"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_VOLUME_BOOST = "volume_boost"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_VOICE_GENDER = "voice_gender"
    }

    fun isAnnouncementEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ANNOUNCEMENT_ENABLED, true)
    }

    fun setAnnouncementEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ANNOUNCEMENT_ENABLED, enabled).apply()
    }

    fun getLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, "english") ?: "english"
    }

    fun setLanguage(language: String) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun isVolumeBoostEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_VOLUME_BOOST, true)
    }

    fun setVolumeBoostEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_VOLUME_BOOST, enabled).apply()
    }

    fun isAutoStartEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_START, true)
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun getVoiceGender(): String {
        return sharedPreferences.getString(KEY_VOICE_GENDER, "female") ?: "female"
    }

    fun setVoiceGender(gender: String) {
        sharedPreferences.edit().putString(KEY_VOICE_GENDER, gender).apply()
    }
}

