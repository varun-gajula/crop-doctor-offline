# Proguard rules for Crop Doctor Offline
-keep class com.cropdoctor.** { *; }
-keep class org.pytorch.executorch.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature

# Keep kotlinx serialization
-keepclassmembers class * {
    companion object *;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}
