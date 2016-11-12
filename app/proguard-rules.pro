# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in G:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Disable debug info output
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String,int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void throwUninitializedPropertyAccessException(java.lang.String);
}

-dontwarn org.jetbrains.anko.internals.**
-keep class org.jetbrains.anko.internals.** { *;}

-dontwarn rx.internal.util.unsafe.**
-keep class rx.internal.util.unsafe.** { *;}

-keepattributes *Annotation*

-keep class com.ztc1997.fingerprint2sleep.xposed.FPQAModule
-keep class com.ztc1997.fingerprint2sleep.util.XposedProbe {
    public static int activatedModuleVersion();
}