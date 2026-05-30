# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Whisper JNI
-keep class com.videocontextbot.processor.whisper.** { *; }
-keepclassmembers class com.videocontextbot.processor.whisper.** {
    @androidx.annotation.Keep *;
}

# Modelos y datos
-keep class com.videocontextbot.domain.model.** { *; }
-keep class com.videocontextbot.data.remote.dto.** { *; }
