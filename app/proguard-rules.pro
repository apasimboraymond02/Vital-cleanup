# Keep Room Database generated code & entities
-keep class androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Keep Data Models & Domain Models
-keep class com.teraxes.vital.data.model.** { *; }
-keepclassmembers class com.teraxes.vital.data.model.** { *; }
-keep class com.teraxes.vital.domain.** { *; }
-keepclassmembers class com.teraxes.vital.domain.** { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
