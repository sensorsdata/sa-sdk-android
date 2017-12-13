package com.sensorsdata.analytics.android.sdk;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.sensorsdata.analytics.android.sdk.aop.AdapterViewOnItemClickListenerAppClick;
import com.sensorsdata.analytics.android.sdk.aop.CheckBoxOnCheckedChangedAppClick;
import com.sensorsdata.analytics.android.sdk.aop.DialogOnClickAppClick;
import com.sensorsdata.analytics.android.sdk.aop.ExpandableListViewItemChildAppClick;
import com.sensorsdata.analytics.android.sdk.aop.MenuItemAppClick;
import com.sensorsdata.analytics.android.sdk.aop.RadioGroupOnCheckedAppClick;
import com.sensorsdata.analytics.android.sdk.aop.RatingBarOnRatingChangedAppClick;
import com.sensorsdata.analytics.android.sdk.aop.ReactNativeViewAppClick;
import com.sensorsdata.analytics.android.sdk.aop.SeekBarOnSeekBarChangeAppClick;
import com.sensorsdata.analytics.android.sdk.aop.SpinnerOnItemSelectedAppClick;
import com.sensorsdata.analytics.android.sdk.aop.TabHostOnTabChangedAppClick;
import com.sensorsdata.analytics.android.sdk.aop.TrackViewOnAppClick;
import com.sensorsdata.analytics.android.sdk.aop.ViewOnClickAppClick;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Created by 王灼洲 on 2017/8/26
 */

public class SensorsDataRuntimeBridge {
    private final static String TAG = "SensorsDataRuntimeBridge";

    //FragmentAspectj
    public static void onFragmentOnResumeMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            SensorsDataIgnoreTrackAppViewScreen trackEvent = method.getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            String methodDeclaringClass = targetMethod.getDeclaringClass().getName();

            if (targetMethod.getDeclaringClass().getAnnotation(SensorsDataTrackFragmentAppViewScreen.class) == null) {
                return;
            }

            if (!"android.support.v4.app.Fragment".equals(methodDeclaringClass)) {
                if (!targetFragment.isHidden() && targetFragment.getUserVisibleHint()) {
                    trackFragmentViewScreen(targetFragment, fragmentName, activity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //FragmentAspectj
    public static void onFragmentSetUserVisibleHintMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            //Fragment名称
            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            SensorsDataIgnoreTrackAppViewScreen trackEvent = method.getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            if (targetMethod.getDeclaringClass().getAnnotation(SensorsDataTrackFragmentAppViewScreen.class) == null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            //获取所在的Context
            boolean isVisibleHint = (boolean) joinPoint.getArgs()[0];

            if (isVisibleHint) {
                if (targetFragment.isResumed()) {
                    if (!targetFragment.isHidden()) {
                        trackFragmentViewScreen(targetFragment, fragmentName, activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //FragmentAspectj
    public static void onFragmentHiddenChangedMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            //Fragment名称
            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            SensorsDataIgnoreTrackAppViewScreen trackEvent = method.getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            if (targetMethod.getDeclaringClass().getAnnotation(SensorsDataTrackFragmentAppViewScreen.class) == null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            //获取所在的Context
            boolean hidden = (boolean) joinPoint.getArgs()[0];

            if (!hidden) {
                if (targetFragment.isResumed()) {
                    if (targetFragment.getUserVisibleHint()) {
                        trackFragmentViewScreen(targetFragment, fragmentName, activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //FragmentAspectj
    public static void trackFragmentView(JoinPoint joinPoint, Object result) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            if (targetMethod == null) {
                return;
            }

            //Fragment名称
            String fragmentName = joinPoint.getTarget().getClass().getName();

            if (result instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) result);
            } else if (result instanceof View) {
                View view = (View) result;
                view.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void trackFragmentViewScreen(android.support.v4.app.Fragment targetFragment, String fragmentName, Activity activity) {
        try {
            if (targetFragment == null) {
                return;
            }

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

            if (targetFragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) targetFragment;

                String screenUrl = screenAutoTracker.getScreenUrl();
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    SensorsDataUtils.mergeJSONObject(otherProperties, properties);
                }

                SensorsDataAPI.sharedInstance().trackViewScreen(screenUrl, properties);
            } else {
                SensorsDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = targetFragment.getClass().getAnnotation(SensorsDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    String screenUrl = autoTrackAppViewScreenUrl.url();
                    if (TextUtils.isEmpty(screenUrl)) {
                        screenUrl = fragmentName;
                    }
                    SensorsDataAPI.sharedInstance().trackViewScreen(screenUrl, properties);
                } else {
                    SensorsDataAPI.sharedInstance().track("$AppViewScreen", properties);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
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

    //AdapterViewOnItemClickListenerAspectj
    public static void onAdapterViewItemClick(JoinPoint joinPoint) {
        AdapterViewOnItemClickListenerAppClick.onAppClick(joinPoint);
    }

    //CheckBoxOnCheckedChangedAspectj
    public static void onCheckBoxCheckedChanged(JoinPoint joinPoint) {
        CheckBoxOnCheckedChangedAppClick.onAppClick(joinPoint);
    }

    //DialogOnClickAspectj
    public static void onMultiChoiceClick(JoinPoint joinPoint) {
        DialogOnClickAppClick.onMultiChoiceAppClick(joinPoint);
    }

    //DialogOnClickAspectj
    public static void onDialogClick(JoinPoint joinPoint) {
        DialogOnClickAppClick.onAppClick(joinPoint);
    }

    //ExpandableListViewItemOnClickAspectj
    public static void onExpandableListViewItemGroupClick(JoinPoint joinPoint) {
        ExpandableListViewItemChildAppClick.onItemGroupClick(joinPoint);
    }

    //ExpandableListViewItemOnClickAspectj
    public static void onExpandableListViewItemChildClick(JoinPoint joinPoint) {
        ExpandableListViewItemChildAppClick.onItemChildClick(joinPoint);
    }

    //MenuItemSelectedAspectj
    public static void onMenuClick(JoinPoint joinPoint, int menuItemIndex) {
        MenuItemAppClick.onAppClick(joinPoint, menuItemIndex);
    }

    //RadioGroupOnCheckedChangeAspectj
    public static void onRadioGroupCheckedChanged(JoinPoint joinPoint) {
        RadioGroupOnCheckedAppClick.onAppClick(joinPoint);
    }

    //RatingBarOnRatingChangedAspectj
    public static void onRatingBarChanged(JoinPoint joinPoint) {
        RatingBarOnRatingChangedAppClick.onAppClick(joinPoint);
    }

    //ReactNativeAspectj
    public static void onReactNativeViewAppClick(JoinPoint joinPoint) {
        ReactNativeViewAppClick.onAppClick(joinPoint);
    }

    //SeekBarOnSeekBarChangeListenerAspectj
    public static void onSeekBarChange(JoinPoint joinPoint) {
        SeekBarOnSeekBarChangeAppClick.onAppClick(joinPoint);
    }

    //SpinnerOnItemSelectedAspectj
    public static void onSpinnerItemSelected(JoinPoint joinPoint) {
        SpinnerOnItemSelectedAppClick.onAppClick(joinPoint);
    }

    //TabHostOnTabChangedAspectj
    public static void onTabHostChanged(JoinPoint joinPoint) {
        TabHostOnTabChangedAppClick.onAppClick(joinPoint);
    }

    //TrackEventAspectj
    public static void trackEventAOP(JoinPoint joinPoint) {
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

    //TrackViewOnClickAspectj
    public static void trackViewOnClick(JoinPoint joinPoint) {
        TrackViewOnAppClick.onAppClick(joinPoint);
    }

    //ViewOnClickListenerAspectj
    public static void onButterknifeClick(JoinPoint joinPoint) {
        try {
            if (SensorsDataAPI.sharedInstance().isButterknifeOnClickEnabled()) {
                onViewOnClick(joinPoint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //ViewOnClickListenerAspectj
    public static void onViewOnClick(JoinPoint joinPoint) {
        ViewOnClickAppClick.onAppClick(joinPoint);
    }
}
