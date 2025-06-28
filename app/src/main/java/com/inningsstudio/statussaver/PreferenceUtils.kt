package com.inningsstudio.statussaver

import android.app.Application
import androidx.activity.ComponentActivity

class PreferenceUtils(private val application: Application) {

    fun getUriFromPreference(): String? {
        return application.getSharedPreferences(
            Const.APP_PREFERENCE,
            ComponentActivity.MODE_PRIVATE
        )?.getString(
            Const.STATUS_URI, ""
        )
    }

    fun setUriToPreference(statusUri: String) {
        application.getSharedPreferences(Const.APP_PREFERENCE, ComponentActivity.MODE_PRIVATE)
            .edit()
            .putString(Const.STATUS_URI, statusUri)
            .apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return application.getSharedPreferences(Const.APP_PREFERENCE, ComponentActivity.MODE_PRIVATE)
            .getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        application.getSharedPreferences(Const.APP_PREFERENCE, ComponentActivity.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", completed)
            .apply()
    }

    fun getPermissionAttempts(): Int {
        return application.getSharedPreferences(Const.APP_PREFERENCE, ComponentActivity.MODE_PRIVATE)
            .getInt("permission_attempts", 0)
    }

    fun setPermissionAttempts(attempts: Int) {
        application.getSharedPreferences(Const.APP_PREFERENCE, ComponentActivity.MODE_PRIVATE)
            .edit()
            .putInt("permission_attempts", attempts)
            .apply()
    }

}