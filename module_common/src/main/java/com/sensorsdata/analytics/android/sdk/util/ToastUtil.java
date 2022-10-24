package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.Field;

public class ToastUtil {

    private static final String TAG = ToastUtil.class.getSimpleName();

    private static final Handler mToastMainHandler = new Handler(Looper.getMainLooper());

    public static void showShort(Context context, String message) {
        if (context == null) {
            SALog.i(TAG, "context is null");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }
        showToastToMain(context.getApplicationContext(), message, Toast.LENGTH_SHORT);
    }

    public static void showLong(Context context, String message) {
        if (context == null) {
            SALog.i(TAG, "context is null");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }
        showToastToMain(context.getApplicationContext(), message, Toast.LENGTH_LONG);
    }

    private static void showToastToMain(final Context context, final String message, final int lengthLong) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showToast(context, message, lengthLong);
        } else {
            mToastMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    showToast(context, message, lengthLong);
                }
            });
        }
    }

    private static void showToast(Context context, String message, int lengthLong) {
        final Toast toast = Toast.makeText(context, message, lengthLong);
        hookToast(toast);
        toast.show();
    }

    /**
     * 8.0 以下版本 和 华为 8.0 的系统 代理 Toast 的 mHandler 捕获异常
     *
     * @param toast Toast 实例
     */
    private static void hookToast(Toast toast) {
        if (Build.VERSION_CODES.O > Build.VERSION.SDK_INT ||
                (isHuaWei() && (Build.VERSION_CODES.O == Build.VERSION.SDK_INT))) {
            try {
                Object mTn = ReflectUtil.findFieldRecur(toast, "mTN");
                if (mTn != null) {
                    Field mHandler = ReflectUtil.findFieldObj(mTn.getClass(), "mHandler");
                    if (mHandler != null) {
                        mHandler.setAccessible(true);
                        mHandler.set(mTn, new HandlerProxy((Handler) mHandler.get(mTn)));
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    private static boolean isHuaWei() {
        String manufacturer = DeviceUtils.getManufacturer();
        if (manufacturer == null) {
            return false;
        }
        return manufacturer.equalsIgnoreCase("honor") || manufacturer.equalsIgnoreCase("huawei");
    }

    private static class HandlerProxy extends Handler {

        private Handler mHandler;

        public HandlerProxy(Handler handler) {
            this.mHandler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                mHandler.handleMessage(msg);
            } catch (Exception e) {
                //ignore
            }
        }
    }

}
