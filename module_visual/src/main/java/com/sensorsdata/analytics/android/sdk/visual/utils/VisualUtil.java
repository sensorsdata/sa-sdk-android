/*
 * Created by dengshiwei on 2022/07/04.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.visual.utils;

import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.visual.core.ViewTreeStatusObservable;
import com.sensorsdata.analytics.android.sdk.visual.model.SnapInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;

import org.json.JSONException;
import org.json.JSONObject;

public class VisualUtil {

    public static int getVisibility(View view) {
        if (view instanceof Spinner) {
            return View.GONE;
        }
        if (!SAViewUtils.isViewSelfVisible(view)) {
            return View.GONE;
        }
        if (!view.isShown()) {
            return View.GONE;
        }
        return View.VISIBLE;
    }

    public static boolean isSupportElementContent(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return !(view instanceof SeekBar || view instanceof RatingBar || view instanceof Switch);
        }
        return false;
    }

    public static boolean isForbiddenClick(View v) {
        if (ViewUtil.instanceOfWebView(v) || v instanceof AdapterView) {
            return true;
        }
        if (v instanceof TextView) {
            TextView textView = (TextView) v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                return textView.isTextSelectable() && !textView.hasOnClickListeners();
            }
        }
        return false;
    }

    public static boolean isSupportClick(View v) {
        ViewParent parent = v.getParent();
        if (parent instanceof AdapterView || ViewUtil.instanceOfRecyclerView(parent)) {
            return true;
        }
        return v instanceof RatingBar || v instanceof SeekBar;
    }

    /**
     * 取控件响应链的 screen_name
     *
     * @param view ViewTree 中的 控件
     * @param info 可视化临时缓存对象
     * @return 含 $screen_name 和 $title 的 json
     */
    public static JSONObject getScreenNameAndTitle(View view, SnapInfo info) {
        if (view == null) {
            return null;
        }
        JSONObject object = null;
        Activity activity = SAViewUtils.getActivityOfView(view.getContext(), view);
        if (activity == null) {
            activity = AppStateTools.getInstance().getForegroundActivity();
        }
        if (activity != null && activity.getWindow() != null && activity.getWindow().isActive()) {
            Object fragment = SAFragmentUtils.getFragmentFromView(view, activity);
            if (fragment != null) {
                object = SAModuleManager.getInstance().invokeAutoTrackFunction("getFragmentPageInfo", activity, fragment);
                if (info != null && !info.hasFragment) {
                    info.hasFragment = true;
                }
            } else {
                object = SAModuleManager.getInstance().invokeAutoTrackFunction("getActivityPageInfo", activity);
                JSONObject rnJson = SAModuleManager.getInstance().invokeAutoTrackFunction("getRNPageInfo");
                if (object == null) {
                    object = new JSONObject();
                }
                JSONUtils.mergeJSONObject(rnJson, object);
            }
        }
        return object;
    }

    /**
     * append $element_position、$element_selector
     */
    public static ViewNode addViewPathProperties(Activity activity, View view, JSONObject properties) {
        try {
            if (view == null || activity == null) {
                return null;
            }
            if (properties == null) {
                properties = new JSONObject();
            }
            ViewNode viewNode = ViewTreeStatusObservable.getInstance().getViewNode(view);
            if ((SensorsDataAPI.sharedInstance().isVisualizedAutoTrackEnabled() && SensorsDataAPI.sharedInstance().isVisualizedAutoTrackActivity(activity.getClass()))
                    || (SensorsDataAPI.sharedInstance().isHeatMapEnabled() && SensorsDataAPI.sharedInstance().isHeatMapActivity(activity.getClass()))) {
                String elementSelector = SAViewUtils.getElementSelector(view);
                if (!TextUtils.isEmpty(elementSelector)) {
                    properties.put(AopConstants.ELEMENT_SELECTOR, elementSelector);
                }
                if (viewNode != null && !TextUtils.isEmpty(viewNode.getViewPath())) {
                    properties.put(AopConstants.ELEMENT_PATH, viewNode.getViewPath());
                }
            }

            if (viewNode != null && !TextUtils.isEmpty(viewNode.getViewPosition())) {
                properties.put(AopConstants.ELEMENT_POSITION, viewNode.getViewPosition());
            }
            return viewNode;
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}
