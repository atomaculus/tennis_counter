# Playce Wear OS release rules

# Keep Wearable Data Layer API classes used via reflection
-keep class com.google.android.gms.wearable.** { *; }

# Keep DataStore generated classes
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose: keep @Composable metadata
-dontwarn androidx.compose.**
