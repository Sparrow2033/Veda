# Veda release baseline rules for internal publishing.

# Keep runtime-visible annotations (Room, Json adapters, etc.).
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve line numbers for readable crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room entities / DAO / database metadata.
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# Keep JavaScript bridge methods used from WebView JS.
-keepclassmembers class com.veda.app.ui.graph.GraphActivity$GraphBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# GraphViewModel relies on reflection to resolve repository/database entry points by
# exact class/member names (Class.forName + method/field name heuristics).
# Preserve these names in release builds so minification cannot break graph loading.
-keepnames class com.veda.app.data.repo.VedaRepository
-keep class com.veda.app.data.repo.VedaRepository { *; }
-keep class com.veda.app.data.db.AppDatabase { *; }
-keep class com.veda.app.data.dao.** { *; }

# Keep Parcelable creators if introduced by future MVP patches.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep enum values used by name in serialization / state restoration.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove noisy logs in release.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
