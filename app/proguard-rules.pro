# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.lumenui.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.lumenui.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
