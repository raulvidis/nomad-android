# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# Hilt/Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# llama.cpp JNI bridge — keep the class + native method declarations so R8
# doesn't rename symbols the .so binds to by name.
-keep class com.nomad.android.data.ai.LlamaBridge { *; }

# MapLibre
-keep class org.maplibre.gl.** { *; }
-keep class com.mapbox.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson / Kotlinx Serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.nomad.android.data.** { *; }
