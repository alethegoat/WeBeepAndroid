# ============================================================
# ProGuard / R8 rules per WeBeepAndroid
# ============================================================

-keepattributes Signature
-keepattributes *Annotation*

# Keep DTO data classes used as Moodle models
-keep class it.polimi.webeep.core.network.dto.** { *; }

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keep class it.polimi.webeep.core.database.entity.** { *; }
-keep class it.polimi.webeep.core.database.dao.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ---- Hilt / Dagger (Hilt generates its own keep rules) ----
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ---- General ----
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- kotlinx.serialization (Moodle DTOs via Ktor) ----
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# ---- Strip verbose logging in release ----
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
