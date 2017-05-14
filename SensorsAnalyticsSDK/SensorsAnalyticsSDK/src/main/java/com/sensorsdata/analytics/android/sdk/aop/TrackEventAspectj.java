package com.sensorsdata.analytics.android.sdk.aop;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEvent;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Created by 王灼洲 on 2017/1/5
 */

@Aspect
public class TrackEventAspectj {
    private final static String TAG = TrackEventAspectj.class.getCanonicalName();

    @Pointcut("execution(@com.sensorsdata.analytics.android.sdk.SensorsDataTrackEvent * *(..))")
    public void methodAnnotatedWithTrackEvent() {
    }

    @After("methodAnnotatedWithTrackEvent()")
    public void trackEventAOP(final JoinPoint joinPoint) throws Throwable {
        AopThreadPool.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

                    Method method = methodSignature.getMethod();
                    SensorsDataTrackEvent trackEvent = method.getAnnotation(SensorsDataTrackEvent.class);
                    String eventName = trackEvent.eventName();
                    if (TextUtils.isEmpty(eventName)) {
                        return;
                    }

                    String pString = trackEvent.properties();
                    JSONObject properties = new JSONObject();
                    if (!TextUtils.isEmpty(pString)) {
                        properties = new JSONObject(pString);
                    }

                    SensorsDataAPI.sharedInstance().track(eventName, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    SALog.i(TAG, "trackEventAOP error: " + e.getMessage());
                }
            }
        });

    }
}
