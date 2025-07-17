# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep source file and line number information for debugging
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# ===========================================
# DATA CLASSES - Preserve for serialization
# ===========================================

# Keep all data classes and their properties
-keepclassmembers class com.inningsstudio.statussaver.data.model.StatusModel {
    <init>(...);
    <fields>;
}

-keepclassmembers class com.inningsstudio.statussaver.data.model.SavedStatusModel {
    <init>(...);
    <fields>;
}

-keepclassmembers class com.inningsstudio.statussaver.domain.entity.StatusEntity {
    <init>(...);
    <fields>;
}

# Keep UI state classes
-keepclassmembers class com.inningsstudio.statussaver.presentation.state.StatusUiState {
    <init>(...);
    <fields>;
}

-keepclassmembers class com.inningsstudio.statussaver.presentation.state.OnboardingUiState {
    <init>(...);
    <fields>;
}

# Keep all sealed class implementations
-keep class com.inningsstudio.statussaver.presentation.state.StatusUiState$* {
    <init>(...);
    <fields>;
}

-keep class com.inningsstudio.statussaver.presentation.state.OnboardingUiState$* {
    <init>(...);
    <fields>;
}

# ===========================================
# GSON - JSON Serialization
# ===========================================

# Keep GSON classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep classes that are serialized/deserialized with GSON
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===========================================
# COMPOSE - Jetpack Compose
# ===========================================

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Compose UI components
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }

# Keep Compose navigation
-keep class androidx.navigation.compose.** { *; }

# ===========================================
# FIREBASE
# ===========================================

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.config.** { *; }

# ===========================================
# COIL - Image Loading
# ===========================================

# Keep Coil classes
-keep class coil.** { *; }
-keep interface coil.** { *; }

# Keep ImageRequest and related classes
-keep class coil.request.** { *; }
-keep class coil.transform.** { *; }

# ===========================================
# EXOPLAYER - Video Player
# ===========================================

# Keep ExoPlayer classes
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }

# ===========================================
# ACCOMPANIST
# ===========================================

# Keep Accompanist classes
-keep class com.google.accompanist.** { *; }

# ===========================================
# SHIMMER
# ===========================================

# Keep Shimmer classes
-keep class com.facebook.shimmer.** { *; }

# ===========================================
# ANDROIDX & MATERIAL DESIGN
# ===========================================

# Keep AndroidX classes
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Keep Material Design classes
-keep class com.google.android.material.** { *; }

# ===========================================
# KOTLIN
# ===========================================

# Keep Kotlin metadata
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===========================================
# GENERAL RULES
# ===========================================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep R classes
-keep class **.R$* {
    public static <fields>;
}

# Keep custom application class
-keep class com.inningsstudio.statussaver.StatusSaverApplication { *; }

# Keep Firebase service
-keep class com.inningsstudio.statussaver.core.service.StatusSaverFirebaseMessagingService { *; }

# Keep constants
-keep class com.inningsstudio.statussaver.core.constants.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep Repository implementations
-keep class * implements com.inningsstudio.statussaver.domain.repository.StatusRepository {
    <init>(...);
}

# Keep UseCase classes
-keep class com.inningsstudio.statussaver.domain.usecase.** { *; }

# Keep utility classes
-keep class com.inningsstudio.statussaver.core.utils.** { *; }

# ===========================================
# OPTIMIZATION RULES
# ===========================================

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize string operations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# ===========================================
# WEBVIEW (if needed in future)
# ===========================================

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}