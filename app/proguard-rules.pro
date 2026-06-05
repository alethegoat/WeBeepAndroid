# ============================================================
# ProGuard / R8 rules per WeBeepAndroid
# ============================================================

# ---- Retrofit ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ---- Gson ----
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Mantieni i data class usati come DTO (modelli Moodle)
-keep class it.polimi.webeep.core.network.dto.** { *; }

# ---- OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ---- Hilt / Dagger ----
-dontwarn dagger.**
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ---- Coroutines ----
-dontwarn kotlinx.coroutines.**

# ---- Compose ----
-dontwarn androidx.compose.**

# ---- Regole generali ----
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
