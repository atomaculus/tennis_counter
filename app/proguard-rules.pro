# Playce Wear OS release rules

# Keep Wearable Data Layer API classes used via reflection
-keep class com.google.android.gms.wearable.** { *; }

# Health Services uses protobuf reflection internally; obfuscating generated
# proto field names breaks exercise startup on release builds.
-keep class androidx.health.services.client.proto.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    *;
}

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
