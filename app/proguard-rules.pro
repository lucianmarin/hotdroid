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

# Prevent R8 from inlining AndroidViewModel.<init> into subclasses,
# which emits an illegal invoke-direct to grandparent ViewModel.<init>()
-keep class androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
