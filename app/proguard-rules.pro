# Keep line numbers in crash reports readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Theme and settings are written as JSON by kotlinx.serialization. Keep their serializers.
-keepclassmembers class app.furibudget.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.furibudget.**$$serializer { *; }
