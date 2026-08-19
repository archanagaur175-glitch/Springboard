# Keep Room entity + DAO generated code for minified release builds.
-keep class com.springboard.launcher.data.db.** { *; }
-keepclassmembers class com.springboard.launcher.data.db.** { *; }

# Keep notification listener bound by the system.
-keep class com.springboard.launcher.systemui.SpringboardNotificationListener { *; }

# Keep serialized DataStore models.
-keepclassmembers class com.springboard.launcher.data.prefs.** { *; }