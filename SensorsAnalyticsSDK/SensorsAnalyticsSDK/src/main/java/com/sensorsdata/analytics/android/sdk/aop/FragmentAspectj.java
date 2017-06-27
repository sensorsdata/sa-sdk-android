package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppViewScreen;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackOnClick;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Created by 王灼洲 on 2016/11/16
 */

@Aspect
public class FragmentAspectj {
    private final static String TAG = FragmentAspectj.class.getCanonicalName();

    @Around("execution(* android.support.v4.app.Fragment.onCreateView(..))")
    public Object fragmentOnCreateViewMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackFragmentView(joinPoint);
    }

    @Around("execution(* android.app.Fragment.onCreateView(..))")
    public Object fragmentOnCreateViewMethod2(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackFragmentView(joinPoint);
    }

    private Object trackFragmentView(final ProceedingJoinPoint joinPoint) throws Throwable {
        // 被注解的方法在这一行代码被执行
        Object result = joinPoint.proceed();

        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            //Fragment名称
            String fragmentName = targetMethod.getDeclaringClass().getName();

            if (result instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) result);
            } else if (result instanceof View) {
                View view = (View) result;
                view.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName)) {
                return;
            }

            if (root == null) {
                return;
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup) {
                    child.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
                } else if (child instanceof ViewGroup) {
                    traverseView(fragmentName, (ViewGroup) child);
                } else {
                    child.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@After("execution(* android.support.v4.app.Fragment.onHiddenChanged(boolean))")
    public void onHiddenChangedMethod(JoinPoint joinPoint) throws Throwable {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            //Fragment名称
            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            SensorsDataIgnoreTrackOnClick trackEvent = method.getAnnotation(SensorsDataIgnoreTrackOnClick.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            //获取所在的Context
            boolean hidden = (boolean) joinPoint.getArgs()[0];

            if (!hidden) {
                if (targetFragment.isResumed()) {
                    if (targetFragment.getUserVisibleHint()) {
                        trackFragmentViewScreen(fragmentName, activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@After("execution(* android.support.v4.app.Fragment.setUserVisibleHint(boolean))")
    public void setUserVisibleHintMethod(JoinPoint joinPoint) throws Throwable {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            //Fragment名称
            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            SensorsDataIgnoreTrackOnClick trackEvent = method.getAnnotation(SensorsDataIgnoreTrackOnClick.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            //获取所在的Context
            boolean isVisibleHint = (boolean) joinPoint.getArgs()[0];

            if (isVisibleHint) {
                if (targetFragment.isResumed()) {
                    if (!targetFragment.isHidden()) {
                        trackFragmentViewScreen(fragmentName, activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@After("execution(* android.support.v4.app.Fragment.onResume())")
    public void onResumeMethod(JoinPoint joinPoint) throws Throwable {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            SensorsDataIgnoreTrackOnClick trackEvent = method.getAnnotation(SensorsDataIgnoreTrackOnClick.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            String methodDeclaringClass = targetMethod.getDeclaringClass().getName();
            if ("android.support.v4.app.Fragment".equals(methodDeclaringClass)) {
                if (!targetFragment.isHidden() && targetFragment.getUserVisibleHint()) {
                    trackFragmentViewScreen(fragmentName, activity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void trackFragmentViewScreen(String fragmentName, Activity activity) {
        try {
            if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragmentName)) {
                return;
            }

            JSONObject properties = new JSONObject();
            if (activity != null) {
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
                properties.put(AopConstants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName));
            } else {
                properties.put(AopConstants.SCREEN_NAME, fragmentName);
            }

            SensorsDataAPI.sharedInstance().track("$AppViewScreen", properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
