/*
 * Created by dengshiwei on 2020/05/11.
 * Copyright 2015－2021 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.util;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ThreadNameConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class AppInfoUtils {
    private static String mAppVersionName;
    private static Bundle mConfigBundle;
    /**
     * 获取应用名称
     *
     * @param context Context
     * @return 应用名称
     */
    public static CharSequence getAppName(Context context) {
        if (context == null) return "";
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            return appInfo.loadLabel(packageManager);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 获取 App 的 ApplicationId
     *
     * @param context Context
     * @return ApplicationId
     */
    public static String getProcessName(Context context) {
        if (context == null) return "";
        try {
            return context.getApplicationInfo().processName;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    /**
     * 获取 App 版本号
     *
     * @param context Context
     * @return App 的版本号
     */
    public static String getAppVersionName(Context context) {
        if (context == null) return "";
        if (!TextUtils.isEmpty(mAppVersionName)) {
            return mAppVersionName;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            mAppVersionName = packageInfo.versionName;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return mAppVersionName;
    }

    /**
     * 获取主进程的名称
     *
     * @param context Context
     * @return 主进程名称
     */
    public static String getMainProcessName(Context context) {
        if (context == null) {
            return "";
        }
        try {
            return context.getApplicationContext().getApplicationInfo().processName;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    /**
     * 判断当前进程名称是否为主进程
     *
     * @param context Context
     * @param bundle Bundle
     * @return 是否主进程
     */
    public static boolean isMainProcess(Context context, Bundle bundle) {
        if (context == null) {
            return false;
        }

        String mainProcessName = AppInfoUtils.getMainProcessName(context);
        if (TextUtils.isEmpty(mainProcessName) && bundle != null) {
            mainProcessName = bundle.getString("com.sensorsdata.analytics.android.MainProcessName");
        }

        if (TextUtils.isEmpty(mainProcessName)) {
            return true;
        }

        String currentProcess = getCurrentProcessName();
        return TextUtils.isEmpty(currentProcess) || mainProcessName.equals(currentProcess);
    }

    /**
     * 判断线程是否埋点执行线程
     * @return true，埋点执行线程；false，非埋点执行线程
     */
    public static boolean isTaskExecuteThread() {
        return TextUtils.equals(ThreadNameConstants.THREAD_TASK_EXECUTE, Thread.currentThread().getName());
    }

    /**
     * 获取 Application 标签的 Bundle 对象
     * @param context Context
     * @return Bundle
     */
    public static Bundle getAppInfoBundle(Context context) {
        if (mConfigBundle == null) {
            try {
                final ApplicationInfo appInfo = context.getApplicationContext().getPackageManager()
                        .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                mConfigBundle = appInfo.metaData;
            } catch (final PackageManager.NameNotFoundException e) {
                SALog.printStackTrace(e);
            }
        }

        if (mConfigBundle == null) {
            return new Bundle();
        }
        return mConfigBundle;
    }

    /**
     * 获得当前进程的名字
     *
     * @return 进程名称
     */
    private static String getCurrentProcessName() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return Application.getProcessName();
            }

            String currentProcess = getCurrentProcessNameByCmd();
            if (TextUtils.isEmpty(currentProcess)) {
                currentProcess = getCurrentProcessNameByAT();
            }
            return currentProcess;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private static String getCurrentProcessNameByAT() {
        String processName = null;
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread", false, Application.class.getClassLoader());
            Method declaredMethod = activityThread.getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
            declaredMethod.setAccessible(true);
            Object processInvoke = declaredMethod.invoke(null);
            if (processInvoke instanceof String) {
                processName = (String) processInvoke;
            }
        } catch (Throwable e) {
            //ignore
        }
        return processName;
    }

    private static String getCurrentProcessNameByCmd() {
        FileInputStream in = null;
        try {
            String fn = "/proc/self/cmdline";
            in = new FileInputStream(fn);
            byte[] buffer = new byte[256];
            int len = 0;
            int b;
            while ((b = in.read()) > 0 && len < buffer.length) {
                buffer[len++] = (byte) b;
            }
            if (len > 0) {
                return new String(buffer, 0, len, "UTF-8");
            }
        } catch (Throwable e) {
            // ignore
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    SALog.printStackTrace(e);
                }
            }
        }
        return null;
    }
}
