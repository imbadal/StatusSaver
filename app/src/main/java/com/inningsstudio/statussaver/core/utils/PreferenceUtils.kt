package com.inningsstudio.statussaver.core.utils

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.inningsstudio.statussaver.core.constants.Const
import androidx.activity.ComponentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inningsstudio.statussaver.data.model.SavedStatusModel

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

    fun isPrivacyPolicyAccepted(): Boolean {
        return application.getSharedPreferences(Const.APP_PREFERENCE, ComponentActivity.MODE_PRIVATE)
            .getBoolean("privacy_policy_accepted", false)
    }

    fun setPrivacyPolicyAccepted(accepted: Boolean) {
        application.getSharedPreferences(Const.APP_PREFERENCE, ComponentActivity.MODE_PRIVATE)
            .edit()
            .putBoolean("privacy_policy_accepted", accepted)
            .apply()
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