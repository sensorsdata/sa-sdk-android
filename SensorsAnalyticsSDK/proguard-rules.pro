# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/zouyuhan/Documents/android/sdks/android-sdk-macosx/tools/proguard/proguard-android.txt
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

-dontwarn com.sensorsdata.analytics.android.**
-keep class com.sensorsdata.analytics.android.** {
*;
}
-keep class **.R$* {
    <fields>;
}
-keep public class * extends android.content.ContentProvider
-keepnames class * extends android.view.View

-dontwarn org.json.**
-keep class org.json.**{*;}


# AlertDialog
-keep class android.app.AlertDialog {*;}
-keep class android.support.v7.app.AlertDialog {*;}
-keep class androidx.appcompat.app.AlertDialog {*;}
-keep class * extends android.support.v7.app.AlertDialog {*;}
-keep class * extends androidx.appcompat.app.AlertDialog {*;}
-keep class * extends android.app.AlertDialog {*;}

# Fragment
-keep class android.app.Fragment {*;}
-keep class android.support.v4.app.Fragment {*;}
-keep class androidx.fragment.app.Fragment {*;}
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public void setUserVisibleHint(boolean);
    public void onViewCreated(android.view.View, android.os.Bundle);
    public void onHiddenChanged(boolean);
    public void onResume();
}
-keepclassmembers class * extends android.app.Fragment {
    public void setUserVisibleHint(boolean);
    public void onViewCreated(android.view.View, android.os.Bundle);
    public void onHiddenChanged(boolean);
    public void onResume();
}
-keepclassmembers class * extends android.support.v4.app.Fragment {
    public void setUserVisibleHint(boolean);
    public void onViewCreated(android.view.View, android.os.Bundle);
    public void onHiddenChanged(boolean);
    public void onResume();
}


# TabLayout
-keep class android.support.design.widget.TabLayout$Tab {*;}
-keep class com.google.android.material.tabs.TabLayout$Tab {*;}
-keep class * extends android.support.design.widget.TabLayout$Tab {*;}
-keep class * extends com.google.android.material.tabs.TabLayout$Tab {*;}


# SwitchCompat
-keep class android.support.v7.widget.SwitchCompat {*;}
-keep class androidx.appcompat.widget.SwitchCompat {*;}
-keep class * extends android.support.v7.widget.SwitchCompat {*;}
-keep class * extends androidx.appcompat.widget.SwitchCompat {*;}

# ContextCompat
-keep class android.support.v4.content.ContextCompat {*;}
-keep class androidx.core.content.ContextCompat {*;}
-keep class * extends android.support.v4.content.ContextCompat {*;}
-keep class * extends androidx.core.content.ContextCompat {*;}

# AppCompatActivity
-keep class android.support.v7.app.AppCompatActivity {
    public android.support.v7.app.ActionBar getSupportActionBar();
}
-keep class androidx.appcompat.app.AppCompatActivity {
    public androidx.appcompat.app.ActionBar getSupportActionBar();
}
-keep class * extends android.support.v7.app.AppCompatActivity {
    public android.support.v7.app.ActionBar getSupportActionBar();
}
-keep class * extends androidx.appcompat.app.AppCompatActivity {
    public androidx.appcompat.app.ActionBar getSupportActionBar();
}

#ActionBar
-keep class android.support.v7.app.ActionBar {*;}
-keep class androidx.appcompat.app.ActionBar {*;}
-keep class * extends android.support.v7.app.ActionBar {*;}
-keep class * extends androidx.appcompat.app.ActionBar {*;}