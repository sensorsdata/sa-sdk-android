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

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.AppStateManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.visual.ViewTreeStatusObservable;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.model.VisualConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可视化全埋点自定义属性统一管理类
 */
public class VisualPropertiesManager {

    private static final String TAG = "SA.VP.VisualPropertiesManager";
    private static final String PROPERTY_TYPE_NUMBER = "NUMBER";
    private VisualConfig mVisualConfig;
    private VisualPropertiesCache mConfigCache;
    private VisualConfigRequestHelper mRequestHelper;
    private CollectLogListener mCollectLogListener;
    private VisualPropertiesH5Helper mVisualPropertiesH5Helper;

    private VisualPropertiesManager() {
        mConfigCache = new VisualPropertiesCache();
        mVisualConfig = mConfigCache.getVisualConfig();
        mRequestHelper = new VisualConfigRequestHelper();
        mVisualPropertiesH5Helper = new VisualPropertiesH5Helper();
    }

    public static VisualPropertiesManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static VisualPropertiesManager INSTANCE = new VisualPropertiesManager();
    }

    public void requestVisualConfig(Context context, SensorsDataAPI sensorsDataAPI) {
        try {
            if (sensorsDataAPI == null || !sensorsDataAPI.isNetworkRequestEnable()) {
                SALog.i(TAG, "Close network request");
                return;
            }
            mRequestHelper.requestVisualConfig(context, getVisualConfigVersion(), new VisualConfigRequestHelper.IApiCallback() {
                @Override
                public void onSuccess(String message) {
                    save2Cache(message);
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void requestVisualConfig() {
        try {
            Context context = SensorsDataAPI.sharedInstance().getContext();
            if (context != null) {
                requestVisualConfig(context, SensorsDataAPI.sharedInstance());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public VisualPropertiesH5Helper getVisualPropertiesH5Helper() {
        return mVisualPropertiesH5Helper;
    }

    public VisualPropertiesCache getVisualPropertiesCache() {
        return mConfigCache;
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
        APP_CLICK("appclick", "$AppClick"),
        WEB_CLICK("appclick", "$WebClick");
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
                // 事件元素为 H5，此时由 JS 处理
                if (event != null && event.isH5) {
                    continue;
                }
                List<VisualConfig.VisualProperty> properties = visualPropertiesConfig.properties;
                if (properties == null || properties.size() == 0) {
                    SALog.i(TAG, "properties is empty ");
                    continue;
                }
                mergeVisualProperty(properties, event, srcObject, viewNode, visualPropertiesConfig.eventName);
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
                if (!TextUtils.isEmpty(screenName) && !TextUtils.equals(event.screenName, screenName)) {
                    continue;
                }

                if (eventType == VisualEventType.APP_CLICK || eventType == VisualEventType.WEB_CLICK) {
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

        if (mVisualConfig == null) {
            SALog.i(TAG, "VisualConfig is null and return");
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

    private void mergeVisualProperty(List<VisualConfig.VisualProperty> properties, VisualConfig.VisualEvent event, JSONObject srcObject, ViewNode clickViewNode, String eventName) {
        try {
            // 用来对 webView_element_path 进行分组
            HashSet<String> h5HashSet = new HashSet<>();
            for (VisualConfig.VisualProperty visualProperty : properties) {
                if (visualProperty.isH5 && !TextUtils.isEmpty(visualProperty.webViewElementPath)) {
                    h5HashSet.add(visualProperty.webViewElementPath + visualProperty.screenName);
                } else {
                    mergeAppVisualProperty(visualProperty, event, srcObject, clickViewNode);
                }
            }
            // 处理 App 内嵌 H5 属性采集
            if (h5HashSet.size() > 0) {
                mVisualPropertiesH5Helper.mergeJSVisualProperties(srcObject, h5HashSet, eventName);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void mergeAppVisualProperty(VisualConfig.VisualProperty visualProperty, VisualConfig.VisualEvent event, JSONObject srcObject, ViewNode clickViewNode) {
        try {
            // 属性名非法校验
            if (TextUtils.isEmpty(visualProperty.name)) {
                SALog.i(TAG, "config visual property name is empty");
                return;
            }

            // 属性 element_path 非法校验
            if (TextUtils.isEmpty(visualProperty.elementPath)) {
                SALog.i(TAG, "config visual property elementPath is empty");
                return;
            }

            // 当满足以下几个条件，需要替换属性控件的 elementPosition,暂不支持列表嵌套列表
            // 1. 事件控件和属性控件在同一个列表里 2. 事件控件支持限定位置，且选择「不限定位置」
            if (clickViewNode != null && !TextUtils.isEmpty(clickViewNode.getViewPosition()) && event != null && !TextUtils.isEmpty(event.elementPosition) && !event.limitElementPosition && !TextUtils.isEmpty(visualProperty.elementPosition)) {
                if (TextUtils.equals(visualProperty.elementPath.split("-")[0], event.elementPath.split("-")[0])) {
                    visualProperty.elementPosition = clickViewNode.getViewPosition();
                    SALog.i(TAG, "visualProperty elementPosition replace: " + clickViewNode.getViewPosition());
                }
            }

            String propertyElementContent = null;
            try {
                ViewNode viewTreeNode = ViewTreeStatusObservable.getInstance().getViewNode(clickViewNode != null ? clickViewNode.getView() : null, visualProperty.elementPath, visualProperty.elementPosition, visualProperty.screenName);
                if (viewTreeNode != null && TextUtils.equals(visualProperty.elementPath, viewTreeNode.getViewPath()) && (TextUtils.isEmpty(visualProperty.elementPosition) | TextUtils.equals(visualProperty.elementPosition, viewTreeNode.getViewPosition()))) {
                    // 默认是已缓存的 viewNode content，优先从 view 引用中再次获取 element_content，保持数据最新
                    propertyElementContent = viewTreeNode.getViewContent();
                    WeakReference<View> targetView = null;
                    if (viewTreeNode.getView() != null) {
                        targetView = viewTreeNode.getView();
                    }
                    if (targetView != null && targetView.get() != null) {
                        // 为保证获取到的 element_content 是最新的，这里从 view 引用再次获取
                        propertyElementContent = ViewUtil.getViewContentAndType(targetView.get(), true).getViewContent();
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            if (propertyElementContent == null || TextUtils.isEmpty(propertyElementContent)) {
                if (mCollectLogListener != null) {
                    mCollectLogListener.onFindPropertyElementFailure(visualProperty.name, visualProperty.elementPath, visualProperty.elementPosition);
                }
                return;
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
                        return;
                    }
                } catch (Exception e) {
                    if (mCollectLogListener != null) {
                        mCollectLogListener.onParsePropertyContentFailure(visualProperty.name, visualProperty.type, propertyElementContent, visualProperty.regular);
                    }
                    SALog.printStackTrace(e);
                    return;
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
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
