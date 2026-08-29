# ProGuard rules for dwPlayer
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.jetbrains.annotations.Nullable *;
    @org.jetbrains.annotations.NotNull *;
}

# Ktor & Netty
-keep class io.ktor.** { *; }
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# SMBJ
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
