# Keep kotlinx.serialization metadata for R8
-keepattributes *Annotation*,InnerClasses

# Keep generated serializers
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }
