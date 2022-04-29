package com.sensorsdata.analytics.android.sdk.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.ref.WeakReference;

public class FragmentCacheUtil {

    @SuppressLint("NewApi")
    private static LruCache<String, WeakReference<Object>> sFragmentLruCache = new LruCache<>(Integer.MAX_VALUE);

    public static void setFragmentToCache(String fragmentName, Object object) {
        if (!TextUtils.isEmpty(fragmentName) && null != object && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            sFragmentLruCache.put(fragmentName, new WeakReference<>(object));
        }
    }

    public static Object getFragmentFromCache(String fragmentName) {
        try {
            if (!TextUtils.isEmpty(fragmentName)) {
                WeakReference<Object> weakReference = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
                    weakReference = sFragmentLruCache.get(fragmentName);
                }
                Object object;
                if (null != weakReference) {
                    object = weakReference.get();
                    if (null != object) {
                        return object;
                    }
                }
                object = Class.forName(fragmentName).newInstance();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    sFragmentLruCache.put(fragmentName, new WeakReference<>(object));
                }
                return object;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}
