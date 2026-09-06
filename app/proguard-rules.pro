# ProGuard & R8 Optimization Rules for PDF Saathi (Google Play Store Release)

# PDFBox Android Optimization
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-keep class org.apache.pdfbox.** { *; }
-dontwarn org.apache.pdfbox.**

# Hilt & Dependency Injection
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclassmembers class * {
    @javax.inject.Inject *;
}

# Room Database
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Jetpack Compose
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNodeElement { *; }

# General Android Shrinking & Optimization
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-repackageclasses ''
-allowaccessmodification
