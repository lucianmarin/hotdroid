# Keep kotlinx.serialization descriptors
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.lucianmarin.news.model.**$$serializer { *; }
-keepclassmembers class com.lucianmarin.news.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.lucianmarin.news.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
