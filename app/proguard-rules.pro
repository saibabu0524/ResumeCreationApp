# ─── Retrofit ───
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ─── Kotlin Serialization ───
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.softsuave.resumecreationapp.**$$serializer { *; }
-keepclassmembers class com.softsuave.resumecreationapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.softsuave.resumecreationapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── DTO / Entity classes ───
-keep class com.softsuave.resumecreationapp.core.network.dto.** { *; }
-keep class com.softsuave.resumecreationapp.core.database.entity.** { *; }

# ─── Coroutines ───
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ─── Hilt ───
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ─── OkHttp ───
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase {}
-dontwarn androidx.room.paging.**

# ─── Compose ───
# Keep Parcelable implementations for process death / state restoration
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
# Stability annotations for Compose compiler
-keep,allowobfuscation @interface androidx.compose.runtime.Stable
-keep,allowobfuscation @interface androidx.compose.runtime.Immutable

# ─── Navigation (type-safe @Serializable routes) ───
-keep class com.softsuave.resumecreationapp.feature.**.navigation.* { *; }
-keep class com.softsuave.resumecreationapp.navigation.* { *; }

# ─── Firebase ───
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-dontwarn com.google.firebase.**

# ─── WorkManager ───
-keep class * extends androidx.work.Worker {}
-keep class * extends androidx.work.CoroutineWorker {}
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
