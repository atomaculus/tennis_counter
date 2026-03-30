# Playce Mobile release rules

# Room Database - keep entities and DAOs
-keep class com.example.tenniscounter.mobile.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Wearable Data Layer API classes
-keep class com.google.android.gms.wearable.** { *; }

# Google Play Billing
-keep class com.android.vending.billing.** { *; }

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

# DataStore generated classes
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
