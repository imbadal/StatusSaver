package com.inningsstudio.statussaver

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.inningsstudio.statussaver.core.utils.FirebaseUtils

class StatusSaverApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase Analytics
        FirebaseAnalytics.getInstance(this)
        
        // Initialize Firebase Crashlytics
        FirebaseCrashlytics.getInstance()
        
        // Initialize Firebase Messaging
        FirebaseMessaging.getInstance()
        
        // Enable Crashlytics collection
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // Initialize Firebase Utils
        FirebaseUtils.initializeAnalytics(this)
    }
} 