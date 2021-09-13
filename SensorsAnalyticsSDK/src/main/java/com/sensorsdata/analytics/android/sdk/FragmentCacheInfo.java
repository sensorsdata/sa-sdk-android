package com.sensorsdata.analytics.android.sdk;

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * author : Handy
 * e-mail : huxq17@gmail.com
 * time : 2021/09/07
 * desc :
 */
public class FragmentCacheInfo {
    private WeakReference<Activity> activityWeakReference;
    private final Class<?> fragmentClazz;
    private final JSONObject trackProperties;

    public FragmentCacheInfo(Activity activity, Object fragment) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.fragmentClazz = fragment.getClass();
        JSONObject trackProperties = null;
        if (fragment instanceof ScreenAutoTracker) {
            ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
            try {
                trackProperties = screenAutoTracker.getTrackProperties();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        this.trackProperties = trackProperties;
    }

    public Activity getActivity() {
        return activityWeakReference == null ? null : activityWeakReference.get();
    }

    public void setActivity(Activity activity) {
        this.activityWeakReference = new WeakReference<>(activity);
    }

    public Class<?> getFragmentClazz() {
        return fragmentClazz;
    }

    public JSONObject getTrackProperties() {
        return trackProperties;
    }
}
