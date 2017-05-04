# Sensors Analytics

This is the official Android SDK for Sensors Analytics.

## Easy Installation

 __Gradle 编译环境（Android Studio）__

第一步：在 **project** 级别的 build.gradle 文件中添加 Sensors Analytics android-gradle-plugin 依赖：

```android
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.2.3'
        //添加 Sensors Analytics android-gradle-plugin 依赖
        classpath 'com.sensorsdata.analytics.android:android-gradle-plugin:1.0.6'
    }
}

allprojects {
    repositories {
        jcenter()
    }
}
```

如下示例图：
![](https://github.com/sensorsdata/sa-sdk-android/blob/master/SensorsAnalyticsSDK/screenshots/android_sdk_autotrack_1.png)

第二步：在 **主 module** 的 build.gradle 文件中添加 com.sensorsdata.analytics.android 插件、Sensors Analytics SDK 依赖：

```android
apply plugin: 'com.android.application'
//添加 com.sensorsdata.analytics.android 插件
apply plugin: 'com.sensorsdata.analytics.android'

dependencies {
   compile 'com.android.support:appcompat-v7:25.1.1'
   //添加 Sensors Analytics SDK 依赖
   compile 'com.sensorsdata.analytics.android:SensorsAnalyticsSDK:1.7.1'
}
```
SensorsAnalyticsSDK 的最新版本号请参考 [github 更新日志](https://github.com/sensorsdata/sa-sdk-android/releases)。

如下示例图：
![](https://github.com/sensorsdata/sa-sdk-android/blob/master/SensorsAnalyticsSDK/screenshots/android_sdk_autotrack_2.png)

第三步：添加 Sensors Analytics SDK 需要的权限

修改 `app/src/main/AndroidManifest.xml`，添加 Sensors Analytics SDK 需要的权限:

```xml
	<!-- 同步数据需要网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- 获取网络状态 -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <!-- 获取运营商信息 -->
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

第四步：在 project 级别的 gradle.properties 中添加如下配置：

```android
android.enableBuildCache=false
```

如下示例图：
![](https://github.com/sensorsdata/sa-sdk-android/blob/master/SensorsAnalyticsSDK/screenshots/android_sdk_autotrack_5.png)

如果开启 [buildCache](https://developer.android.com/studio/build/build-cache.html)，Android Studio 会把依赖的 jar 或 arr 缓存到本地，并且把模块名称设置为 hash 值，导致 includeJarFilter 配置失效。

第五步：目前全埋点不支持 Android Studio 的 instant run 特性，使用全埋点需要关闭该特性。

如下示例图：
![](https://github.com/sensorsdata/sa-sdk-android/blob/master/SensorsAnalyticsSDK/screenshots/android_sdk_autotrack_4.png)

第六步：由于 SDK 会依赖 appcompat-v7 处理下面几个控件：

* android.support.v7.widget.SwitchCompat
* android.support.v7.app.AlertDialog

需要添加下面依赖( 如果项目中已引入了 v7包，可以不添加 )：

```android
compile 'com.android.support:appcompat-v7:25.1.1'
```

Android SDK 要求最低系统版本为 API 11（Android 3.0），特别地，AutoTrack功能要求系统最低版本为 API 14 （Android 4.0），可视化埋点功能要求最低系统版本为 API 16（Android 4.1）。目前，Android SDK (aar格式) 大小约为 230 KB。

**注：** 由于 SDK 中有一部分类或方法是被 javascript 调用的，再加上 SDK 的源码本身就是开源的，所以建议不要混淆 Android SDK。

如果开启了混淆，请在proguard文件中添加下边代码：

```
-dontwarn com.sensorsdata.analytics.android.sdk.**
-keep class com.sensorsdata.analytics.android.sdk.** {
*;
}
# 使用可视化埋点需添加
-keep class **.R$* {
    <fields>;
}

-keep class com.sensorsdata.analytics.android.sdk.** {
 *;
}
-dontwarn com.sensorsdata.analytics.android.sdk.**
-keepnames class * extends android.view.View

```

## To Learn More

See our [full manual](http://www.sensorsdata.cn/manual/android_sdk.html)

## For Java Developers

We also maintains a SDK for Java at 

    https://github.com/sensorsdata/sa-sdk-java

