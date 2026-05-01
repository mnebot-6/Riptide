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

# ── Ktor Client ──────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keepnames class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── OkHttp ───────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── DTOs (Sync API + Auth) ───────────────────────────────────────────────────
-keep class com.mnebot.riptide.data.remote.dto.** { *; }
-keep class com.mnebot.riptide.data.remote.** { *; }

# ── Google Play Services Auth ────────────────────────────────────────────────
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── Kotlin ───────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }

# ── General ──────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
