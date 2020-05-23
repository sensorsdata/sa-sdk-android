/*
 * Created by zhangxiangwei on 2020/03/05.
 * Copyright 2015－2020 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.visual.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.visual.snap.Pathfinder;

public class VisualUtil {
    public static int getVisibility(View view) {
        if (view instanceof Spinner) {
            return View.GONE;
        }
        return ViewUtil.isViewSelfVisible(view) ? View.VISIBLE : View.GONE;
    }

    @SuppressLint("NewApi")
    public static boolean isSupportElementContent(View view) {
        return !(view instanceof SeekBar || view instanceof RatingBar || view instanceof Switch);
    }

    public static boolean isForbiddenClick(View v) {
        if (v instanceof WebView || ViewUtil.instanceOfX5WebView(v) || v instanceof AdapterView) {
            return true;
        }
        if (v instanceof TextView) {
            TextView textView = (TextView) v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (textView.isTextSelectable() && !textView.hasOnClickListeners()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSupportClick(View v) {
        ViewParent parent = v.getParent();
        if (parent instanceof AdapterView || ViewUtil.instanceOfRecyclerView(parent)) {
            return true;
        }
        if (v instanceof RatingBar || v instanceof SeekBar) {
            return true;
        }
        return false;
    }

    public static int getChildIndex(ViewParent parent, View child) {
        try {
            if (!(parent instanceof ViewGroup)) {
                return -1;
            }
            ViewGroup viewParent = (ViewGroup) parent;
            final String childIdName = AopUtil.getViewId(child);
            String childClassName = child.getClass().getCanonicalName();
            int index = 0;
            for (int i = 0; i < viewParent.getChildCount(); i++) {
                View brother = viewParent.getChildAt(i);
                if (!Pathfinder.hasClassName(brother, childClassName)) {
                    continue;
                }
                String brotherIdName = AopUtil.getViewId(brother);
                if (null != childIdName && !childIdName.equals(brotherIdName)) {
                    index++;
                    continue;
                }
                if (brother == child) {
                    return index;
                }
                index++;
            }
            return -1;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return -1;
        }
    }
}
