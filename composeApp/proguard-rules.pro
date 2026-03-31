# ── Riptide ProGuard/R8 Rules ─────────────────────────────────────────────────

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class com.mnebot.riptide.data.local.db.** { *; }
-keep class com.mnebot.riptide.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ── kotlinx-serialization ────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mnebot.riptide.**$$serializer { *; }
-keepclassmembers class com.mnebot.riptide.** {
    *** Companion;
}
-keepclasseswithmembers class com.mnebot.riptide.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Compose ──────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Glance (Widget) ──────────────────────────────────────────────────────────
-keep class com.mnebot.riptide.widget.** { *; }
-keep class androidx.glance.** { *; }

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── WallpaperService ─────────────────────────────────────────────────────────
-keep class com.mnebot.riptide.wallpaper.** { *; }

# ── Domain models (used with Room + serialization) ───────────────────────────
-keep class com.mnebot.riptide.domain.model.** { *; }

# ── Kotlin ───────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }

# ── General ──────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
