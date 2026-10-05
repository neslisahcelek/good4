# Keep kotlinx.serialization metadata for R8
-keepattributes *Annotation*,InnerClasses

# Keep generated serializers
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }

# Credential Manager & Google Sign-In
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

