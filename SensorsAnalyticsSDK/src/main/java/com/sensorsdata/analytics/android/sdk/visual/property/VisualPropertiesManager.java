/*
 * Created by zhangxiangwei on 2021/01/28.
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

package com.sensorsdata.analytics.android.sdk.visual.property;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.AppStateManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.model.VisualConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可视化全埋点自定义属性统一管理类
 */
public class VisualPropertiesManager {

    private static final String TAG = "SA.VP.VisualPropertiesManager";
    private static final String PROPERTY_TYPE_NUMBER = "NUMBER";
    // 当属性数大于该值，优先遍历 ViewTree 整体耗时更小；否则优先遍历配置。
    private static final int MAX_PROPERTY_NUMBER = 5;
    private static VisualPropertiesManager sInstance;
    private VisualConfig mVisualConfig;
    private VisualPropertiesCache mConfigCache;
    private VisualConfigRequestHelper mRequestHelper;
    private CollectLogListener mCollectLogListener;

    private VisualPropertiesManager() {
        mConfigCache = new VisualPropertiesCache();
        mVisualConfig = mConfigCache.getVisualConfig();
        mRequestHelper = new VisualConfigRequestHelper();
    }

    public static VisualPropertiesManager getInstance() {
        if (sInstance == null) {
            synchronized (VisualPropertiesManager.class) {
                if (sInstance == null) {
                    sInstance = new VisualPropertiesManager();
                }
            }
        }
        return sInstance;
    }

    public void requestVisualConfig(Context context) {
        mRequestHelper.requestVisualConfig(context, getVisualConfigVersion(), new VisualConfigRequestHelper.IApiCallback() {
            @Override
            public void onSuccess(String message) {
                save2Cache(message);
            }
        });
    }

    public void requestVisualConfig() {
        Context context = SensorsDataAPI.sharedInstance().getContext();
        if (context != null) {
            requestVisualConfig(context);
        }
    }

    public VisualConfig getVisualConfig() {
        return mVisualConfig;
    }

    public void save2Cache(String message) {
        mConfigCache.save2Cache(message);
        mVisualConfig = mConfigCache.getVisualConfig();
    }

    public String getVisualConfigVersion() {
        if (mVisualConfig != null) {
            return mVisualConfig.version;
        }
        return null;
    }

    interface CollectLogListener {
        void onStart(String eventType, String screenName, ViewNode viewNode);

        void onSwitchClose();

        void onCheckVisualConfigFailure(String message);

        void onCheckEventConfigFailure();

        void onFindPropertyElementFailure(String propertyName, String propertyElementPath, String propertyElementPosition);

        void onParsePropertyContentFailure(String propertyName, String propertyType, String elementContent, String regular);

        void onOtherError(String message);
    }

    public void registerCollectLogListener(CollectLogListener listener) {
        this.mCollectLogListener = listener;
    }

    public void unRegisterCollectLogListener() {
        this.mCollectLogListener = null;
    }

    public enum VisualEventType {
        APP_CLICK("appclick", "$AppClick");
        private String visualEventType;
        private String trackEventType;

        VisualEventType(String visualEventType, String trackEventType) {
            this.visualEventType = visualEventType;
            this.trackEventType = trackEventType;
        }

        public String getVisualEventType() {
            return visualEventType;
        }

        public static VisualEventType getVisualEventType(String trackEventType) {
            for (VisualEventType visualEventType : VisualEventType.values()) {
                if (TextUtils.equals(visualEventType.trackEventType, trackEventType)) {
                    return visualEventType;
                }
            }
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void mergeVisualProperties(VisualEventType eventType, JSONObject srcObject, ViewNode viewNode) {
        try {
            String screenName = srcObject.optString(AopConstants.SCREEN_NAME);
            if (mCollectLogListener != null) {
                mCollectLogListener.onStart(eventType.visualEventType, screenName, viewNode);
            }

            SALog.i(TAG, String.format("mergeVisualProperties eventType: %s, screenName:%s ", eventType.getVisualEventType(), screenName));
            if (TextUtils.isEmpty(screenName)) {
                SALog.i(TAG, "screenName is empty and return");
                return;
            }

            // 校验开关是否打开
            if (!SensorsDataAPI.sharedInstance().isVisualizedAutoTrackEnabled()) {
                SALog.i(TAG, "you should call 'enableVisualizedAutoTrack(true)' first");
                if (mCollectLogListener != null) {
                    mCollectLogListener.onSwitchClose();
                }
                return;
            }

            Activity activity = null;
            if (viewNode != null) {
                WeakReference<View> view = viewNode.getView();
                if (view != null && view.get() != null) {
                    activity = AopUtil.getActivityFromContext(view.get().getContext(), view.get());
                }
            }
            if (activity == null) {
                activity = AppStateManager.getInstance().getForegroundActivity();
            }
            if (!(activity != null && SensorsDataAPI.sharedInstance().isVisualizedAutoTrackActivity(activity.getClass()))) {
                SALog.i(TAG, "activity is null or not in white list and return");
                if (mCollectLogListener != null) {
                    mCollectLogListener.onOtherError("activity is null or not in white list and return");
                }
                return;
            }

            // 缓存中不存在配置
            if (mVisualConfig == null) {
                SALog.i(TAG, "visual properties is empty and return");
                if (mCollectLogListener != null) {
                    mCollectLogListener.onCheckVisualConfigFailure("本地缓存无自定义属性配置");
                }
                return;
            }

            if (!checkAppIdAndProject()) {
                if (mCollectLogListener != null) {
                    mCollectLogListener.onCheckVisualConfigFailure("本地缓存的 AppId 或 Project 与当前项目不一致");
                }
                return;
            }
            // 校验配置是否为空
            List<VisualConfig.VisualPropertiesConfig> propertiesConfigs = mVisualConfig.events;
            if (propertiesConfigs == null || propertiesConfigs.size() == 0) {
                SALog.i(TAG, "propertiesConfigs is empty");
                if (mCollectLogListener != null) {
                    mCollectLogListener.onOtherError("propertiesConfigs is empty");
                }
                return;
            }

            String elementPath = null;
            String elementPosition = null;
            String elementContent = null;
            if (viewNode != null) {
                elementPath = viewNode.getViewPath();
                elementPosition = viewNode.getViewPosition();
                elementContent = viewNode.getViewContent();
            }
            List<VisualConfig.VisualPropertiesConfig> eventConfigList = getMatchEventConfigList(propertiesConfigs, eventType, screenName, elementPath, elementPosition, elementContent);
            if (eventConfigList.size() == 0) {
                SALog.i(TAG, "event config is empty and return");
                if (mCollectLogListener != null) {
                    mCollectLogListener.onCheckEventConfigFailure();
                }
                return;
            }

            for (VisualConfig.VisualPropertiesConfig visualPropertiesConfig : eventConfigList) {
                // 走到这里，事件控件便命中了，开始获取属性控件
                VisualConfig.VisualEvent event = visualPropertiesConfig.event;
                List<VisualConfig.VisualProperty> properties = visualPropertiesConfig.properties;
                if (properties == null || properties.size() == 0) {
                    SALog.i(TAG, "properties is empty ");
                    return;
                }

                boolean isByViewTree = properties.size() >= MAX_PROPERTY_NUMBER;
                HashMap<String, ViewNode> viewTreeHashMap = new HashMap<>();
                View rootView = getRootView(eventType, viewNode != null ? viewNode.getView() : null);
                if (isByViewTree) {
                    findTargetView(rootView, viewTreeHashMap);
                }
                mergeVisualProperty(rootView, properties, event, srcObject, viewNode, isByViewTree, viewTreeHashMap);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public List<VisualConfig.VisualPropertiesConfig> getMatchEventConfigList(List<VisualConfig.VisualPropertiesConfig> propertiesConfigs, VisualEventType eventType, String screenName, String elementPath, String elementPosition, String elementContent) {
        // 遍历事件控件
        List<VisualConfig.VisualPropertiesConfig> list = new ArrayList<>();
        try {
            for (VisualConfig.VisualPropertiesConfig visualPropertiesConfig : propertiesConfigs) {
                // 校验事件类型一致
                if (!TextUtils.equals(visualPropertiesConfig.eventType, eventType.getVisualEventType())) {
                    continue;
                }

                // 校验事件控件 screen_name 是否存在
                VisualConfig.VisualEvent event = visualPropertiesConfig.event;
                if (!TextUtils.equals(event.screenName, screenName)) {
                    continue;
                }

                if (eventType == VisualEventType.APP_CLICK) {
                    // 校验事件控件 $element_path 是否一致
                    if (!TextUtils.equals(event.elementPath, elementPath)) {
                        SALog.i(TAG, String.format("event element_path is not match: current element_path is %s, config element_path is %s ", elementPath, event.elementPath));
                        continue;
                    }

                    // 如果限定了位置，则必须校验 elementPosition
                    if (event.limitElementPosition && !TextUtils.equals(event.elementPosition, elementPosition)) {
                        SALog.i(TAG, String.format("event element_position is not match: current element_position is %s, config element_position is %s ", elementPosition, event.elementPosition));
                        continue;
                    }

                    // 如果限定了内容，则必须校验 elementContent
                    if (event.limitElementContent && !TextUtils.equals(event.elementContent, elementContent)) {
                        SALog.i(TAG, String.format("event element_content is not match: current element_content is %s, config element_content is %s ", elementContent, event.elementContent));
                        continue;
                    }
                }
                list.add(visualPropertiesConfig);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return list;
    }

    public boolean checkAppIdAndProject() {
        // 校验当前数据接收地址
        String serverUrl = SensorsDataAPI.sharedInstance().getServerUrl();
        if (TextUtils.isEmpty(serverUrl)) {
            SALog.i(TAG, "serverUrl is empty and return");
            return false;
        }
        // 校验当前 project 和 appId
        Uri uri = Uri.parse(serverUrl);
        String project = uri.getQueryParameter("project");
        Context context = SensorsDataAPI.sharedInstance().getContext();
        String appId = AppInfoUtils.getProcessName(context);
        if (TextUtils.isEmpty(project) || TextUtils.isEmpty(appId)) {
            SALog.i(TAG, "project or app_id is empty and return");
            return false;
        }

        // 校验配置中的 app_id 和 当前 app_id 是否一致
        if (!TextUtils.equals(appId, mVisualConfig.appId)) {
            SALog.i(TAG, String.format("app_id is not equals: current app_id is %s, config app_id is %s ", appId, mVisualConfig.appId));
            return false;
        }

        // 校验配置中的 project 和 当前 project 是否一致
        if (!TextUtils.equals(project, mVisualConfig.project)) {
            SALog.i(TAG, String.format("project is not equals: current project is %s, config project is %s ", project, mVisualConfig.project));
            return false;
        }
        return true;
    }

    @TargetApi(17)
    private View getRootView(VisualEventType eventType, WeakReference<View> view) {
        View rootView = null;
        if (view != null && view.get() != null) {
            rootView = view.get().getRootView();
        }
        if (rootView == null) {
            Activity activity = AppStateManager.getInstance().getForegroundActivity();
            if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
                SALog.i(TAG, "findPropertyTargetView activity == null and return");
                return null;
            }
            SALog.i(TAG, "activity class name: " + activity.getClass().getCanonicalName());
            final Window window = activity.getWindow();
            rootView = window.getDecorView().getRootView();
        }
        if (rootView == null) {
            SALog.i(TAG, "don't find any root view");
            return null;
        }
        return rootView;
    }

    private String findTargetView(final View view, String elementPath, String elementPosition) {
        ViewNode viewNode = ViewUtil.getViewPathAndPosition(view, true);
        if (viewNode != null && !TextUtils.isEmpty(viewNode.getViewContent()) && TextUtils.equals(elementPath, viewNode.getViewPath()) && (TextUtils.isEmpty(elementPosition) | TextUtils.equals(elementPosition, viewNode.getViewPosition()))) {
            return viewNode.getViewContent();
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = group.getChildAt(i);
                if (child != null) {
                    String elementContent = findTargetView(child, elementPath, elementPosition);
                    if (!TextUtils.isEmpty(elementContent)) {
                        return elementContent;
                    }
                }
            }
        }
        return null;
    }

    private void mergeVisualProperty(final View view, List<VisualConfig.VisualProperty> properties, VisualConfig.VisualEvent event, JSONObject srcObject, ViewNode clickViewNode, boolean isByViewTree, HashMap<String, ViewNode> viewTreeHashMap) {
        try {
            for (VisualConfig.VisualProperty visualProperty : properties) {
                // 属性名非法校验
                if (TextUtils.isEmpty(visualProperty.name)) {
                    SALog.i(TAG, "config visual property name is empty");
                    continue;
                }

                // 属性 element_path 非法校验
                if (TextUtils.isEmpty(visualProperty.elementPath)) {
                    SALog.i(TAG, "config visual property elementPath is empty");
                    continue;
                }

                // 当满足以下几个条件，需要替换属性控件的 elementPosition,暂不支持列表嵌套列表
                // 1. 事件控件和属性控件在同一个列表里 2. 事件控件支持限定位置，且选择「不限定位置」
                if (clickViewNode != null && !TextUtils.isEmpty(clickViewNode.getViewPosition()) && !TextUtils.isEmpty(event.elementPosition) && !event.limitElementPosition && !TextUtils.isEmpty(visualProperty.elementPosition)) {
                    if (TextUtils.equals(visualProperty.elementPath.split("-")[0], event.elementPath.split("-")[0])) {
                        visualProperty.elementPosition = clickViewNode.getViewPosition();
                        SALog.i(TAG, "visualProperty elementPosition replace: " + clickViewNode.getViewPosition());
                    }
                }

                String propertyElementContent = null;
                if (isByViewTree) {
                    String key = generateKey(visualProperty.elementPath, visualProperty.elementPosition);
                    if (!viewTreeHashMap.containsKey(key)) {
                        continue;
                    }
                    ViewNode viewTreeNode = viewTreeHashMap.get(key);
                    if (viewTreeNode != null && TextUtils.equals(visualProperty.elementPath, viewTreeNode.getViewPath()) && (TextUtils.isEmpty(visualProperty.elementPosition) | TextUtils.equals(visualProperty.elementPosition, viewTreeNode.getViewPosition()))) {
                        propertyElementContent = viewTreeNode.getViewContent();
                    }
                } else {
                    propertyElementContent = findTargetView(view, visualProperty.elementPath, visualProperty.elementPosition);
                }

                if (propertyElementContent == null || TextUtils.isEmpty(propertyElementContent)) {
                    if (mCollectLogListener != null) {
                        mCollectLogListener.onFindPropertyElementFailure(visualProperty.name, visualProperty.elementPath, visualProperty.elementPosition);
                    }
                    continue;
                }

                SALog.i(TAG, String.format("find property target view success, property element_path: %s,element_position: %s,element_content: %s", visualProperty.elementPath, visualProperty.elementPosition, propertyElementContent));
                // 开始正则处理
                String result = null;
                if (!TextUtils.isEmpty(visualProperty.regular)) {
                    Pattern pattern = null;
                    try {
                        pattern = Pattern.compile(visualProperty.regular, Pattern.DOTALL | Pattern.MULTILINE);
                        Matcher matcher = pattern.matcher(propertyElementContent);
                        if (matcher.find()) {
                            result = matcher.group();
                            SALog.i(TAG, String.format("propertyValue is: %s", result));
                        } else {
                            SALog.i(TAG, "matcher not find continue");
                            if (mCollectLogListener != null) {
                                mCollectLogListener.onParsePropertyContentFailure(visualProperty.name, visualProperty.type, propertyElementContent, visualProperty.regular);
                            }
                            continue;
                        }
                    } catch (Exception e) {
                        if (mCollectLogListener != null) {
                            mCollectLogListener.onParsePropertyContentFailure(visualProperty.name, visualProperty.type, propertyElementContent, visualProperty.regular);
                        }
                        SALog.printStackTrace(e);
                        continue;
                    }
                }

                // merge jsonObject、可视化属性优先级最高
                if (!TextUtils.isEmpty(result)) {
                    if (TextUtils.equals(PROPERTY_TYPE_NUMBER, visualProperty.type)) {
                        try {
                            if (result != null) {
                                srcObject.put(visualProperty.name, NumberFormat.getInstance().parse(result));
                            }
                        } catch (Exception e) {
                            if (mCollectLogListener != null) {
                                mCollectLogListener.onOtherError(e.getMessage());
                            }
                        }
                    } else {
                        try {
                            srcObject.put(visualProperty.name, result);
                        } catch (JSONException e) {
                            if (mCollectLogListener != null) {
                                mCollectLogListener.onOtherError(e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void findTargetView(final View view, HashMap<String, ViewNode> hashMap) {
        ViewNode viewNode = ViewUtil.getViewPathAndPosition(view, true);
        if (viewNode != null && !TextUtils.isEmpty(viewNode.getViewContent()) && !TextUtils.isEmpty(viewNode.getViewPath())) {
            hashMap.put(generateKey(viewNode.getViewPath(), viewNode.getViewPosition()), viewNode);
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = group.getChildAt(i);
                if (child != null) {
                    findTargetView(child, hashMap);
                }
            }
        }
    }

    private String generateKey(String elementPath, String elementPosition) {
        StringBuilder key = new StringBuilder();
        key.append(elementPath);
        if (!TextUtils.isEmpty(elementPosition)) {
            key.append(elementPosition);
        }
        return key.toString();
    }
}
