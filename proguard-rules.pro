# R8 / ProGuard rules for PDF Saathi

# Keep Room entities & DAOs
-keep class com.pdfsaathi.app.data.local.entity.** { *; }
-keep class com.pdfsaathi.app.data.local.dao.** { *; }

# Keep Hilt generated classes
-keep class com.pdfsaathi.app.di.** { *; }

# Keep Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
