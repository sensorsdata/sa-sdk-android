package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Created by 王灼洲 on 2016/11/16
 */

@Aspect
public class FragmentAspectj {
    private final static String TAG = FragmentAspectj.class.getCanonicalName();

    //@After("execution(* android.support.v4.app.Fragment.onCreateView(..))")
    public void fragmentOnCreateViewMethod(JoinPoint joinPoint) throws Throwable {
        track(joinPoint);
    }

    //@After("execution(* android.app.Fragment.onCreateView(..))")
    public void fragmentOnCreateViewMethod2(JoinPoint joinPoint) throws Throwable {
        track(joinPoint);
    }

    private void track(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            //Fragment名称
            String fragmentName = targetMethod.getDeclaringClass().getName();

            //获取所在的Context
            LayoutInflater inflater = (LayoutInflater) joinPoint.getArgs()[0];
            Context context = inflater.getContext();

            //将Context转成Activity
            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }

            JSONObject properties = new JSONObject();
            properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            properties.put("fragmentName", fragmentName);

            SensorsDataAPI.sharedInstance().track("$AppViewScreen", properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
