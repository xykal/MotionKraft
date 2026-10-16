# Keep core runtime
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# FFmpeg Kit - native libs (maintained fork)
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }
-dontwarn com.arthenica.**
-dontwarn dev.ffmpegkit-maintained.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Data models used via Gson
-keepclassmembers,allowobfuscation class xyz.xyverse.motionkraft.data.model.** {
    <init>(...);
    <fields>;
}
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# Coil
-dontwarn coil.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-repackageclasses 'xyz.xyverse.motionkraft'
