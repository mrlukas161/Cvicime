# Minifikacia je v release builde zatial vypnuta (viz app/build.gradle.kts).
# Az sa zapne, tieto pravidla su vychodzi zaklad.

-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**
