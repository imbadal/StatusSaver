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

}