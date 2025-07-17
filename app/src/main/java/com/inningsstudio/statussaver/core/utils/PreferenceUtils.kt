package com.inningsstudio.statussaver.core.utils

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.inningsstudio.statussaver.core.constants.Const
import androidx.activity.ComponentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inningsstudio.statussaver.data.model.SavedStatusModel

class PreferenceUtils(private val application: Application) {
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences(Const.APP_PREFERENCE, ComponentActivity.MODE_PRIVATE)
    }

    // In-memory cache
    private var cachedUri: String? = null
    private var cachedOnboardingCompleted: Boolean? = null
    private var cachedPrivacyPolicyAccepted: Boolean? = null

    fun getUriFromPreference(): String? {
        if (cachedUri != null) return cachedUri
        val start = System.currentTimeMillis()
        val value = prefs.getString(Const.STATUS_URI, "")
        val end = System.currentTimeMillis()
        Log.d("PreferenceUtils", "getUriFromPreference took ${end - start} ms")
        cachedUri = value
        return value
    }

    fun setUriToPreference(statusUri: String) {
        val start = System.currentTimeMillis()
        prefs.edit().putString(Const.STATUS_URI, statusUri).apply()
        val end = System.currentTimeMillis()
        Log.d("PreferenceUtils", "setUriToPreference took ${end - start} ms")
        cachedUri = statusUri
    }

    fun isOnboardingCompleted(): Boolean {
        if (cachedOnboardingCompleted != null) return cachedOnboardingCompleted!!
        val start = System.currentTimeMillis()
        val value = prefs.getBoolean("onboarding_completed", false)
        val end = System.currentTimeMillis()
        Log.d("PreferenceUtils", "isOnboardingCompleted took ${end - start} ms")
        cachedOnboardingCompleted = value
        return value
    }

    fun setOnboardingCompleted(completed: Boolean) {
        val start = System.currentTimeMillis()
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        val end = System.currentTimeMillis()
        Log.d("PreferenceUtils", "setOnboardingCompleted took ${end - start} ms")
        cachedOnboardingCompleted = completed
    }

    fun getPermissionAttempts(): Int {
        return prefs.getInt("permission_attempts", 0)
    }

    fun setPermissionAttempts(attempts: Int) {
        prefs.edit().putInt("permission_attempts", attempts).apply()
    }

    fun isPrivacyPolicyAccepted(): Boolean {
        if (cachedPrivacyPolicyAccepted != null) return cachedPrivacyPolicyAccepted!!
        val start = System.currentTimeMillis()
        val value = prefs.getBoolean("privacy_policy_accepted", false)
        val end = System.currentTimeMillis()
        Log.d("PreferenceUtils", "isPrivacyPolicyAccepted took ${end - start} ms")
        cachedPrivacyPolicyAccepted = value
        return value
    }

    fun setPrivacyPolicyAccepted(accepted: Boolean) {
        val start = System.currentTimeMillis()
        prefs.edit().putBoolean("privacy_policy_accepted", accepted).apply()
        val end = System.currentTimeMillis()
        Log.d("PreferenceUtils", "setPrivacyPolicyAccepted took ${end - start} ms")
        cachedPrivacyPolicyAccepted = accepted
    }

    companion object {
        private const val PREF_NAME = Const.PREF_NAME
        private const val KEY_FIRST_TIME = Const.KEY_FIRST_TIME
        private const val KEY_STATUS_URI = Const.KEY_STATUS_URI

        fun isFirstTime(context: Context): Boolean {
            val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getBoolean(KEY_FIRST_TIME, true)
        }

        fun setFirstTime(context: Context, isFirstTime: Boolean) {
            val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean(KEY_FIRST_TIME, isFirstTime)
            editor.apply()
        }

        fun getStatusUri(context: Context): String {
            val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString(KEY_STATUS_URI, "") ?: ""
        }

        fun setStatusUri(context: Context, statusUri: String) {
            val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(KEY_STATUS_URI, statusUri)
            editor.apply()
        }
    }
}