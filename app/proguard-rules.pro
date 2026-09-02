# ============================================================
# ProGuard / R8 rules per WeBeepAndroid
# ============================================================

-keepattributes Signature
-keepattributes *Annotation*

# Keep DTO data classes used as Moodle models
-keep class it.polimi.webeep.core.network.dto.** { *; }

# ---- OkHttp / Okio (warnings only; consumer rules handle keep) ----
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keep class it.polimi.webeep.core.database.entity.** { *; }
-keep class it.polimi.webeep.core.database.dao.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ---- Hilt / Dagger (warnings only; Hilt generates its own keep rules) ----
-dontwarn dagger.**
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ---- Coroutines ----
-dontwarn kotlinx.coroutines.**

# ---- Compose ----
-dontwarn androidx.compose.**

# ---- General ----
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
