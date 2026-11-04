# Kelavs VPN ProGuard Rules

# Keep WireGuard classes
-keep class com.wireguard.android.** { *; }
-keep class com.wireguard.config.** { *; }
-keep class com.wireguard.crypto.** { *; }
-keep class com.wireguard.util.** { *; }

# Keep Kelavs VPN classes
-keep class com.kelavs.vpn.** { *; }

# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.KSerializer *;
}

# Keep Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Material3
-keep class androidx.compose.material3.** { *; }

# Keep Navigation
-keep class androidx.navigation.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Biometric
-keep class androidx.biometric.** { *; }

# Keep ZXing
-keep class com.journeyapps.** { *; }
-keep class com.google.zxing.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep JSON
-keep class kotlinx.serialization.json.** { *; }

# Keep Security Crypto
-keep class androidx.security.crypto.** { *; }

# Prevent obfuscation of native library names
-keep class com.wireguard.android.util.SharedLibraryLoader { *; }

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Retain line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Don't optimize away null checks
-optimizations !code/allocation/variable
# ---------------------------------------------------------------------------
# Kelavs additions for a reliable release build
# ---------------------------------------------------------------------------

# Keep source information so release crash reports stay readable.
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod,SourceFile,LineNumberTable

# Optional dependencies referenced (but not required) by OkHttp / Kotlin libs.
# Without these the R8 step can abort with "Missing classes detected".
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.codehaus.mojo.**
-dontwarn org.jetbrains.annotations.**
-dontwarn android.support.**

# kotlinx.serialization: keep generated serializers ($serializer / Companion).
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.coroutines.internal.MainDispatcherFactory provider(...);
}

# Room: keep entity constructors used by reflection-based DAO adapter fallbacks.
-keepclassmembers class com.kelavs.vpn.** {
    <init>(...);
}
