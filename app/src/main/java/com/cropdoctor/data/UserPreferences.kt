package com.cropdoctor.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("crop_doctor_prefs", Context.MODE_PRIVATE)

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var selectedRegion: String
        get() = prefs.getString(KEY_REGION, "Telangana") ?: "Telangana"
        set(value) = prefs.edit().putString(KEY_REGION, value).apply()

    companion object {
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_REGION = "selected_region"
    }
}
