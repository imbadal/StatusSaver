package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseUtils {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initializeAnalytics(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logEvent(eventName: String, parameters: Bundle? = null) {
        firebaseAnalytics?.logEvent(eventName, parameters)
    }

    fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logUserAction(action: String, parameters: Map<String, String>? = null) {
        val bundle = Bundle().apply {
            putString("action", action)
            parameters?.forEach { (key, value) ->
                putString(key, value)
            }
        }
        logEvent("user_action", bundle)
    }

    fun logError(exception: Throwable, customMessage: String? = null) {
        FirebaseCrashlytics.getInstance().apply {
            recordException(exception)
            customMessage?.let { setCustomKey("error_message", it) }
        }
    }

    fun logNonFatalError(message: String, parameters: Map<String, String>? = null) {
        FirebaseCrashlytics.getInstance().apply {
            log(message)
            parameters?.forEach { (key, value) ->
                setCustomKey(key, value)
            }
        }
    }

    fun setUserProperty(key: String, value: String) {
        firebaseAnalytics?.setUserProperty(key, value)
    }

    fun setUserId(userId: String) {
        firebaseAnalytics?.setUserId(userId)
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    fun getFCMToken(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { token ->
                        onSuccess(token)
                    } ?: onFailure(Exception("FCM token is null"))
                } else {
                    onFailure(task.exception ?: Exception("Failed to get FCM token"))
                }
            }
    }

    fun subscribeToTopic(topic: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Failed to subscribe to topic"))
                }
            }
    }

    fun unsubscribeFromTopic(topic: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Failed to unsubscribe from topic"))
                }
            }
    }
} 