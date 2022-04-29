/*
 * Created by wangzhuozhou on 2015/08/01.
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
package com.sensorsdata.analytics.android.sdk.visual;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.AppStateManager;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAutoTrackHelper;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.visual.model.SnapInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.model.WebNode;
import com.sensorsdata.analytics.android.sdk.visual.model.WebNodeInfo;
import com.sensorsdata.analytics.android.sdk.visual.snap.PropertyDescription;
import com.sensorsdata.analytics.android.sdk.visual.snap.ResourceIds;
import com.sensorsdata.analytics.android.sdk.visual.snap.SnapCache;
import com.sensorsdata.analytics.android.sdk.visual.snap.SoftWareCanvas;
import com.sensorsdata.analytics.android.sdk.visual.util.Dispatcher;
import com.sensorsdata.analytics.android.sdk.visual.util.VisualUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ViewSnapshot {

    private static final int MAX_CLASS_NAME_CACHE_SIZE = 255;
    private static final int JS_NOT_INTEGRATED_ALERT_TIME_OUT = 5000;
    private static final String TAG = "SA.ViewSnapshot";
    private final RootViewFinder mRootViewFinder;
    private final List<PropertyDescription> mProperties;
    private final ClassNameCache mClassnameCache;
    private final Handler mMainThreadHandler;
    private AlertRunnable mAlertRunnable;
    private final ResourceIds mResourceIds;
    private SnapInfo mSnapInfo = new SnapInfo();

    public ViewSnapshot(List<PropertyDescription> properties, ResourceIds resourceIds, Handler mainThreadHandler) {
        mProperties = properties;
        mResourceIds = resourceIds;
        mMainThreadHandler = mainThreadHandler;
        mRootViewFinder = new RootViewFinder();
        mClassnameCache = new ClassNameCache(MAX_CLASS_NAME_CACHE_SIZE);
    }

    public SnapInfo snapshots(OutputStream out, StringBuilder lastImageHash) throws IOException {
        final long startSnapshot = System.currentTimeMillis();
        final FutureTask<List<RootViewInfo>> infoFuture =
                new FutureTask<>(mRootViewFinder);
        mMainThreadHandler.post(infoFuture);

        final OutputStream writer = new BufferedOutputStream(out);
        List<RootViewInfo> infoList = Collections.emptyList();
        writer.write("[".getBytes());

        try {
            infoList = infoFuture.get(2, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            SALog.i(TAG, "Screenshot interrupted, no screenshot will be sent.", e);
        } catch (final TimeoutException e) {
            SALog.i(TAG, "Screenshot took more than 2 second to be scheduled and executed. No screenshot will be sent.", e);
        } catch (final ExecutionException e) {
            SALog.i(TAG, "Exception thrown during screenshot attempt", e);
        } catch (Throwable e) {
            SALog.i(TAG, "Throwable thrown during screenshot attempt", e);
        } finally {
            infoFuture.cancel(true);
            mMainThreadHandler.removeCallbacks(infoFuture);
        }

        String screenName = null, activityTitle = null;
        final int infoCount = infoList.size();
        SALog.i(TAG, "infoCount:" + infoCount + ",time:" + (System.currentTimeMillis() - startSnapshot));
        for (int i = 0; i < infoCount; i++) {
            final RootViewInfo info = infoList.get(i);
            if (i > 0) {
                writer.write(",".getBytes());
            }
            if (info != null && info.screenshot != null && (isSnapShotUpdated(info.screenshot.getImageHash(), lastImageHash) || i > 0)) {
                writer.write("{".getBytes());
                writer.write("\"activity\":".getBytes());
                screenName = info.screenName;
                activityTitle = info.activityTitle;
                writer.write(JSONObject.quote(info.screenName).getBytes());
                writer.write(",".getBytes());
                writer.write(("\"scale\":").getBytes());
                writer.write((String.format("%s", info.scale)).getBytes());
                writer.write(",".getBytes());
                writer.write(("\"serialized_objects\":").getBytes());

                try {
                    JSONObject jsonRootObject = new JSONObject();
                    jsonRootObject.put("rootObject", info.rootView.hashCode());
                    JSONArray jsonObjects = new JSONArray();
                    snapshotViewHierarchy(jsonObjects, info.rootView);
                    jsonRootObject.put("objects", jsonObjects);
                    writer.write(jsonRootObject.toString().getBytes());
                    SALog.i(TAG, "snapshotViewHierarchy:" + (System.currentTimeMillis() - startSnapshot));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }

                writer.write(",".getBytes());
                writer.write(("\"image_hash\":").getBytes());
                writer.write((JSONObject.quote(info.screenshot.getImageHash())).getBytes());
                writer.write(",".getBytes());
                writer.write(("\"screenshot\":").getBytes());
                writer.flush();
                info.screenshot.writeBitmapJSON(Bitmap.CompressFormat.PNG, 70, out);
                writer.write("}".getBytes());
            } else {
                writer.write("{}".getBytes());
            }
        }
        writer.write("]".getBytes());
        writer.flush();
        mSnapInfo.screenName = screenName;
        mSnapInfo.activityTitle = activityTitle;
        return mSnapInfo;
    }

    private void getVisibleRect(View view, Rect rect, boolean fullscreen) {
        if (fullscreen) {
            view.getGlobalVisibleRect(rect);
            return;
        }
        int[] offset = new int[2];
        view.getLocationOnScreen(offset);
        boolean visibleRect = view.getLocalVisibleRect(rect);
        SnapCache.getInstance().setLocalVisibleRect(view, visibleRect);
        rect.offset(offset[0], offset[1]);
    }

    private void snapshotViewHierarchy(JSONArray j, View rootView)
            throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            reset();
            snapshotView(j, rootView, 0);
            WebNodesManager.getInstance().setHasWebView(mSnapInfo.isWebView);
        }
    }

    private void reset() {
        mSnapInfo = new SnapInfo();
    }

    public static class AlertRunnable implements Runnable {

        private String url;

        AlertRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            WebNodeInfo webNodeInfo = WebNodesManager.getInstance().getWebNodes(url);
            if (webNodeInfo == null) {
                SALog.i(TAG, "H5 页面未集成 Web JS SDK");
                String msg = "{\"callType\":\"app_alert\",\"data\":[{\"title\":\"当前页面无法进行可视化全埋点\",\"message\":\"此页面未集成 Web JS SDK 或者 Web JS SDK 版本过低，请集成最新版 Web JS SDK\",\"link_text\":\"配置文档\",\"link_url\":\"https://manual.sensorsdata.cn/sa/latest/tech_sdk_client_web_use-7545346.html\"}]}";
                WebNodesManager.getInstance().handlerFailure(url, msg);
            }
        }
    }


    private void snapshotView(final JSONArray j, final View view, int viewIndex)
            throws Exception {
        // 处理内嵌 H5 页面
        if (ViewUtil.isViewSelfVisible(view)) {
            List<String> webNodeIds = null;
            // webView 自身 level 需要小于所有的 H5 元素
            int webViewElementLevel = mSnapInfo.elementLevel;
            if (ViewUtil.instanceOfWebView(view)) {
                mSnapInfo.isWebView = true;
                final CountDownLatch latch = new CountDownLatch(1);
                try {
                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            String url = ReflectUtil.callMethod(view, "getUrl");
                            if (!TextUtils.isEmpty(url)) {
                                mSnapInfo.webViewUrl = url;
                                Float scale = ReflectUtil.callMethod(view, "getScale");
                                if (scale != null) {
                                    mSnapInfo.webViewScale = scale;
                                }
                                latch.countDown();
                                // 2021/07/02 修复 Web JS SDK 部分场景下无法监听页面变化 bug
                                SensorsDataAutoTrackHelper.loadUrl(view, "javascript:window.sensorsdata_app_call_js('visualized')");
                            } else {
                                latch.countDown();
                            }
                        }
                    });
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
                try {
                    latch.await(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    SALog.printStackTrace(e);
                }
                SALog.i(TAG, "WebView url: " + mSnapInfo.webViewUrl);
                if (!TextUtils.isEmpty(mSnapInfo.webViewUrl)) {
                    WebNodeInfo webNodeInfo = WebNodesManager.getInstance().getWebNodes(mSnapInfo.webViewUrl);
                    if (webNodeInfo != null) {
                        if (webNodeInfo.getStatus() == WebNodeInfo.Status.SUCCESS) {
                            List<WebNode> webNodes = webNodeInfo.getWebNodes();
                            if (webNodes != null && webNodes.size() > 0) {
                                webNodeIds = new ArrayList<>();
                                for (WebNode webNode : webNodes) {
                                    mergeWebViewNodes(j, webNode, view, mSnapInfo.webViewScale);
                                    if (webNode.isRootView()) {
                                        webNodeIds.add(webNode.getId() + view.hashCode());
                                    }
                                }
                            }
                        } else if (webNodeInfo.getStatus() == WebNodeInfo.Status.FAILURE) {
                            mSnapInfo.alertInfos = webNodeInfo.getAlertInfos();
                        }
                    } else {
                        if (mAlertRunnable == null) {
                            mAlertRunnable = new AlertRunnable(mSnapInfo.webViewUrl);
                        }
                        Dispatcher.getInstance().postDelayed(mAlertRunnable, JS_NOT_INTEGRATED_ALERT_TIME_OUT);
                    }
                }
            }
            // 处理原生页面
            JSONObject jsonSnapObject = new JSONObject();
            jsonSnapObject.put("hashCode", view.hashCode());
            jsonSnapObject.put("id", view.getId());
            jsonSnapObject.put("index", VisualUtil.getChildIndex(view.getParent(), view));
            if (ViewUtil.instanceOfWebView(view)) {
                jsonSnapObject.put("element_level", webViewElementLevel);
            } else {
                jsonSnapObject.put("element_level", ++mSnapInfo.elementLevel);
            }
            jsonSnapObject.put("element_selector", ViewUtil.getElementSelector(view));

            JSONObject object = VisualUtil.getScreenNameAndTitle(view, mSnapInfo);
            if (object != null) {
                String screenName = object.optString(AopConstants.SCREEN_NAME);
                String title = object.optString(AopConstants.TITLE);
                if (!TextUtils.isEmpty(screenName)) {
                    jsonSnapObject.put("screen_name", screenName);
                }
                if (!TextUtils.isEmpty(title)) {
                    jsonSnapObject.put("title", title);
                }
            }

            ViewNode viewNode = ViewUtil.getViewNode(view, viewIndex, true);
            if (viewNode != null) {
                if (!TextUtils.isEmpty(viewNode.getViewPath())) {
                    jsonSnapObject.put("element_path", viewNode.getViewPath());
                }
                if (!TextUtils.isEmpty(viewNode.getViewPosition())) {
                    jsonSnapObject.put("element_position", viewNode.getViewPosition());
                }
                if (!TextUtils.isEmpty(viewNode.getViewContent()) && VisualUtil.isSupportElementContent(view)) {
                    jsonSnapObject.put("element_content", viewNode.getViewContent());
                }
                jsonSnapObject.put("is_list_view", viewNode.isListView());
            }

            jsonSnapObject.put("sa_id_name", getResName(view));
            try {
                String saId = (String) view.getTag(R.id.sensors_analytics_tag_view_id);
                if (!TextUtils.isEmpty(saId)) {
                    jsonSnapObject.put("sa_id_name", saId);
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            // 对于 Dialog、PopupWindow 类型的非全屏 View 由于 rootView 存在 getTop = 0 / getLeft = 0 的问题，所以先设置了一个全屏 RootView,再设置次级 View 的实际布局。
            if (!WindowHelper.isMainWindow(view.getRootView())) {
                if (WindowHelper.isDecorView(view.getClass())) {
                    final DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
                    int screenWidth = displayMetrics.widthPixels;
                    int screenHeight = displayMetrics.heightPixels;
                    jsonSnapObject.put("top", view.getTop());
                    jsonSnapObject.put("left", view.getLeft());
                    jsonSnapObject.put("width", screenWidth);
                    jsonSnapObject.put("height", screenHeight);
                } else {
                    ViewParent parent = view.getParent();
                    if (parent != null && WindowHelper.isDecorView(parent.getClass())) {
                        Rect rect = new Rect();
                        getVisibleRect(view, rect, false);
                        jsonSnapObject.put("top", rect.top);
                        jsonSnapObject.put("left", rect.left);
                        jsonSnapObject.put("width", rect.width());
                        jsonSnapObject.put("height", rect.height());
                    } else {
                        jsonSnapObject.put("top", view.getTop());
                        jsonSnapObject.put("left", view.getLeft());
                        jsonSnapObject.put("width", view.getWidth());
                        jsonSnapObject.put("height", view.getHeight());
                    }
                }
            } else {
                jsonSnapObject.put("top", view.getTop());
                jsonSnapObject.put("left", view.getLeft());
                jsonSnapObject.put("width", view.getWidth());
                jsonSnapObject.put("height", view.getHeight());
            }

            int scrollX = view.getScrollX();
            // 适配解决 textView 配置了 maxLines = 1 和 gravity = center|right 时 scrollX 属性异常问题
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    if (textView.getMaxLines() == 1) {
                        scrollX = 0;
                    }
                }
            }
            // x5WebView 无法直接获取到 scrollX、scrollY
            if (ViewUtil.instanceOfX5WebView(view)) {
                try {
                    jsonSnapObject.put("scrollX", (Integer) ReflectUtil.callMethod(view, "getWebScrollX"));
                    jsonSnapObject.put("scrollY", (Integer) ReflectUtil.callMethod(view, "getWebScrollY"));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            } else {
                jsonSnapObject.put("scrollX", scrollX);
                jsonSnapObject.put("scrollY", view.getScrollY());
            }
            jsonSnapObject.put("visibility", VisualUtil.getVisibility(view));
            float translationX = 0;
            float translationY = 0;
            if (Build.VERSION.SDK_INT >= 11) {
                translationX = view.getTranslationX();
                translationY = view.getTranslationY();
            }
            jsonSnapObject.put("translationX", translationX);
            jsonSnapObject.put("translationY", translationY);

            JSONArray classesArray = new JSONArray();
            Class<?> klass = view.getClass();
            do {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    classesArray.put(mClassnameCache.get(klass));
                }
                klass = klass.getSuperclass();
            } while (klass != Object.class && klass != null);
            jsonSnapObject.put("classes", classesArray);

            addProperties(jsonSnapObject, view);

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) layoutParams;
                int[] rules = relativeLayoutParams.getRules();
                JSONArray layoutArray = new JSONArray();
                for (int rule : rules) {
                    layoutArray.put(rule);
                }
                jsonSnapObject.put("layoutRules", layoutArray);
            }
            JSONArray subviewsArray = new JSONArray();
            // 添加 WebView 控件所有子元素
            if (webNodeIds != null && webNodeIds.size() > 0) {
                for (String id : webNodeIds) {
                    subviewsArray.put(id);
                }
            } else if (view instanceof ViewGroup) {
                final ViewGroup group = (ViewGroup) view;
                final int childCount = group.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final View child = group.getChildAt(i);
                    if (null != child) {
                        subviewsArray.put(child.hashCode());
                    }
                }
            }
            jsonSnapObject.put("subviews", subviewsArray);
            j.put(jsonSnapObject);
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = group.getChildAt(i);
                // child can be null when views are getting disposed.
                if (null != child) {
                    snapshotView(j, child, i);
                }
            }
        }
    }

    private void addProperties(JSONObject j, View v)
            throws Exception {
        j.put("importantForAccessibility", true);
        final Class<?> viewClass = v.getClass();
        for (final PropertyDescription desc : mProperties) {
            if (desc.targetClass.isAssignableFrom(viewClass) && null != desc.accessor) {
                final Object value = desc.accessor.applyMethod(v);
                if (null == value) {
                    // Don't produce anything in this case
                } else if (value instanceof Number) {
                    j.put(desc.name, (Number) value);
                } else if (value instanceof Boolean) {
                    boolean clickable = (boolean) value;
                    if ("clickable".equals(desc.name)) {
                        if (VisualUtil.isSupportClick(v)) {
                            clickable = true;
                        } else if (VisualUtil.isForbiddenClick(v)) {
                            clickable = false;
                        }
                    }
                    j.put(desc.name, clickable);
                } else if (value instanceof ColorStateList) {
                    j.put(desc.name, (Integer) ((ColorStateList) value).getDefaultColor());
                } else if (value instanceof Drawable) {
                    final Drawable drawable = (Drawable) value;
                    final Rect bounds = drawable.getBounds();
                    JSONObject json = new JSONObject();
                    JSONArray classesArray = new JSONArray();
                    Class klass = drawable.getClass();
                    while (klass != Object.class && klass != null) {
                        String canonicalName = SnapCache.getInstance().getCanonicalName(klass);
                        classesArray.put(canonicalName);
                        klass = klass.getSuperclass();
                    }
                    json.put("classes", classesArray);
                    JSONObject jsonDimensions = new JSONObject();
                    jsonDimensions.put("left", bounds.left);
                    jsonDimensions.put("right", bounds.right);
                    jsonDimensions.put("top", bounds.top);
                    jsonDimensions.put("bottom", bounds.bottom);
                    json.put("dimensions", jsonDimensions);
                    if (drawable instanceof ColorDrawable) {
                        final ColorDrawable colorDrawable = (ColorDrawable) drawable;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            json.put("color", colorDrawable.getColor());
                        }
                    }
                    j.put(desc.name, json);
                } else {
                    j.put(desc.name, value.toString());
                }
            }
        }
    }

    /**
     * 页面 ImageHash / H5 页面元素内容 发生变化 / H5 出现错误提示时需要更新页面信息
     *
     * @param newImageHash
     * @param lastImageHash
     * @return 是否上报页面信息
     */
    private boolean isSnapShotUpdated(String newImageHash, StringBuilder lastImageHash) {
        boolean isUpdated = false;

        if (newImageHash != null && lastImageHash != null) {
            isUpdated = newImageHash.equals(lastImageHash.toString());
        }

        isUpdated = !isUpdated || WebNodesManager.getInstance().hasH5AlertInfo();
        if (lastImageHash != null) {
            lastImageHash.delete(0, lastImageHash.length()).append(newImageHash);
        }
        return isUpdated;
    }

    private String getResName(View view) {
        final int viewId = view.getId();
        if (-1 == viewId) {
            return null;
        } else {
            return mResourceIds.nameForId(viewId);
        }
    }


    @SuppressLint("NewApi")
    private static class ClassNameCache extends LruCache<Class<?>, String> {
        public ClassNameCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected String create(Class<?> klass) {
            return klass.getCanonicalName();
        }
    }

    private static class RootViewFinder implements Callable<List<RootViewInfo>> {

        private final List<RootViewInfo> mRootViews;
        private final CachedBitmap mCachedBitmap;
        private final int mClientDensity = DisplayMetrics.DENSITY_DEFAULT;

        public RootViewFinder() {
            mRootViews = new ArrayList<RootViewInfo>();
            mCachedBitmap = new CachedBitmap();
        }

        @Override
        public List<RootViewInfo> call() throws Exception {
            mRootViews.clear();
            try {
                Activity activity = AppStateManager.getInstance().getForegroundActivity();
                if (activity != null) {
                    JSONObject object = AopUtil.buildTitleAndScreenName(activity);
                    VisualUtil.mergeRnScreenNameAndTitle(object);
                    String screenName = object.optString(AopConstants.SCREEN_NAME);
                    String activityTitle = object.optString(AopConstants.TITLE);
                    View rootView = null;
                    final Window window = activity.getWindow();
                    if (window != null && window.isActive()) {
                        rootView = window.getDecorView().getRootView();
                    }
                    if (rootView == null) {
                        return mRootViews;
                    }
                    final RootViewInfo info = new RootViewInfo(screenName, activityTitle, rootView);
                    final View[] views = WindowHelper.getSortedWindowViews();
                    Bitmap bitmap = null;
                    if (views != null && views.length > 0) {
                        bitmap = mergeViewLayers(views, info);
                        for (View view : views) {
                            if (view.getWindowVisibility() != View.VISIBLE || view.getVisibility() != View.VISIBLE
                                    || view.getWidth() == 0 || view.getHeight() == 0
                                    || TextUtils.equals(WindowHelper.getWindowPrefix(view), WindowHelper.getMainWindowPrefix()))
                                continue;
                            //解决自定义框：比如通过 Window.addView 加的悬浮框
                            if (!WindowHelper.isCustomWindow(view)) {
                                //自定义框图层只参与底图绘制 mergeViewLayers ，不参与页面数据信息处理
                                RootViewInfo subInfo = new RootViewInfo(screenName, activityTitle, view.getRootView());
                                scaleBitmap(subInfo, bitmap);
                                mRootViews.add(subInfo);
                            }
                        }
                    }
                    if (mRootViews.size() == 0) {
                        scaleBitmap(info, bitmap);
                        mRootViews.add(info);
                    }
                }
            } catch (Throwable e) {
                SALog.d(TAG, "" + e);
            }
            return mRootViews;
        }

        Bitmap mergeViewLayers(View[] views, RootViewInfo info) {
            int width = info.rootView.getWidth();
            int height = info.rootView.getHeight();
            if (width == 0 || height == 0) {
                int[] screenSize = DeviceUtils.getDeviceSize(SensorsDataAPI.sharedInstance().getContext());
                width = screenSize[0];
                height = screenSize[1];
                if (width == 0 || height == 0) return null;
            }
            Bitmap fullScreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            SoftWareCanvas canvas = new SoftWareCanvas(fullScreenBitmap);
            int[] windowOffset = new int[2];
            boolean skipOther, isDrawBackground = false;
            if (ViewUtil.getMainWindowCount(views) > 1) {
                skipOther = true;
            } else {
                skipOther = false;
            }
            WindowHelper.init();
            ViewUtil.invalidateLayerTypeView(views);
            for (View view : views) {
                if (!(view.getVisibility() != View.VISIBLE || view.getWidth() == 0 || view.getHeight() == 0 || !ViewUtil.isWindowNeedTraverse(view, WindowHelper.getWindowPrefix(view), skipOther))) {
                    canvas.save();
                    if (!WindowHelper.isMainWindow(view)) {
                        view.getLocationOnScreen(windowOffset);
                        canvas.translate((float) windowOffset[0], (float) windowOffset[1]);
                        if (WindowHelper.isDialogOrPopupWindow(view) && !isDrawBackground) {
                            isDrawBackground = true;
                            Paint paint = new Paint();
                            paint.setColor(0xA0000000);
                            canvas.drawRect(-(float) windowOffset[0], -(float) windowOffset[1], canvas.getWidth(), canvas.getHeight(), paint);
                        }
                    }
                    view.draw(canvas);
                    canvas.restoreToCount(1);
                }
            }
            canvas.destroy();
            return fullScreenBitmap;
        }

        private void scaleBitmap(final RootViewInfo info, Bitmap rawBitmap) {
            float scale = 1.0f;
            if (null != rawBitmap) {
                final int rawDensity = rawBitmap.getDensity();
                if (rawDensity != Bitmap.DENSITY_NONE) {
                    scale = ((float) mClientDensity) / rawDensity;
                }
                final int rawWidth = rawBitmap.getWidth();
                final int rawHeight = rawBitmap.getHeight();
                final int destWidth = (int) ((rawBitmap.getWidth() * scale) + 0.5);
                final int destHeight = (int) ((rawBitmap.getHeight() * scale) + 0.5);
                if (rawWidth > 0 && rawHeight > 0 && destWidth > 0 && destHeight > 0) {
                    mCachedBitmap.recreate(destWidth, destHeight, mClientDensity, rawBitmap);
                }
            }
            info.scale = scale;
            info.screenshot = mCachedBitmap;
        }
    }

    private static class CachedBitmap {

        private final Paint mPaint;
        private Bitmap mCached;
        // 含截图和 WebView 页面元素数据
        private String mImageHash = "";

        public CachedBitmap() {
            mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
            mCached = null;
        }

        public synchronized void recreate(int width, int height, int destDensity, Bitmap source) {
            if (null == mCached || mCached.getWidth() != width || mCached.getHeight() != height) {
                try {
                    mCached = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                } catch (final Throwable e) {
                    mCached = null;
                }

                if (null != mCached) {
                    mCached.setDensity(destDensity);
                }
            }

            if (null != mCached) {
                final Canvas scaledCanvas = new Canvas(mCached);
                scaledCanvas.drawBitmap(source, 0, 0, mPaint);

                try {
                    final ByteArrayOutputStream imageByte = new ByteArrayOutputStream();
                    mCached.compress(Bitmap.CompressFormat.PNG, 100, imageByte);
                    byte[] array = imageByte.toByteArray();

                    final String msg = WebNodesManager.getInstance().getLastWebNodeMsg();
                    if (!TextUtils.isEmpty(msg)) {
                        byte[] webNodesArray = msg.getBytes();
                        if (webNodesArray != null && webNodesArray.length > 0) {
                            array = concat(array, webNodesArray);
                        }
                    }

                    final String debugInfo = VisualizedAutoTrackService.getInstance().getLastDebugInfo();
                    if (!TextUtils.isEmpty(debugInfo)) {
                        byte[] debugInfoBytes = debugInfo.getBytes();
                        if (debugInfoBytes != null && debugInfoBytes.length > 0) {
                            array = concat(array, debugInfoBytes);
                        }
                    }

                    byte[] md5 = MessageDigest.getInstance("MD5").digest(array);
                    mImageHash = toHex(md5);
                } catch (Exception e) {
                    SALog.i(TAG, "CachedBitmap.recreate;Create image_hash error=" + e);
                }
            }
        }

        private static byte[] concat(byte[] first, byte[] second) {
            byte[] result = new byte[first.length + second.length];
            System.arraycopy(first, 0, result, 0, first.length);
            System.arraycopy(second, 0, result, first.length, second.length);
            return result;
        }

        // Writes a QUOTED base64 string (or the string null) to the output stream
        public synchronized void writeBitmapJSON(Bitmap.CompressFormat format, int quality,
                                                 OutputStream out)
                throws IOException {
            if (null == mCached || mCached.getWidth() == 0 || mCached.getHeight() == 0) {
                out.write("null".getBytes());
            } else {
                out.write('"');
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mCached.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.flush();
                String bitmapStr = new String(Base64Coder.encode(stream.toByteArray()));
                out.write(bitmapStr.getBytes());
                out.write('"');
            }
        }

        private String getImageHash() {
            return mImageHash;
        }

        private String toHex(byte[] ary) {
            final String hex = "0123456789ABCDEF";
            String ret = "";
            for (int i = 0; i < ary.length; i++) {
                ret += hex.charAt((ary[i] >> 4) & 0xf);
                ret += hex.charAt(ary[i] & 0xf);
            }
            return ret;
        }
    }

    private static class RootViewInfo {
        final String screenName;
        final String activityTitle;
        final View rootView;
        CachedBitmap screenshot;
        float scale;

        RootViewInfo(String screenName, String activityTitle, View rootView) {
            this.screenName = screenName;
            this.activityTitle = activityTitle;
            this.rootView = rootView;
            this.screenshot = null;
            this.scale = 1.0f;
        }
    }

    private void mergeWebViewNodes(JSONArray j, WebNode view, View webView, float webViewScale) {
        try {
            JSONObject jsonWebView = new JSONObject();
            jsonWebView.put("hashCode", view.getId() + webView.hashCode());
            jsonWebView.put("index", 0);
            if (!TextUtils.isEmpty(view.get$element_selector())) {
                jsonWebView.put("element_selector", view.get$element_selector());
            }
            if (!TextUtils.isEmpty(view.get$element_content())) {
                jsonWebView.put("element_content", view.get$element_content());
            }
            jsonWebView.put("element_level", ++mSnapInfo.elementLevel);
            jsonWebView.put("h5_title", view.get$title());
            if (webViewScale == 0) {
                webViewScale = view.getScale();
            }
            float top = view.getTop() * webViewScale;
            float left = view.getLeft() * webViewScale;
            jsonWebView.put("left", left);
            jsonWebView.put("top", top);
            jsonWebView.put("width", (int) (view.getWidth() * webViewScale));
            jsonWebView.put("height", (int) (view.getHeight() * webViewScale));
            jsonWebView.put("scrollX", 0);
            jsonWebView.put("scrollY", 0);
            boolean insideWebView = view.getOriginTop() * webViewScale <= webView.getHeight() && view.getOriginLeft() * webViewScale <= webView.getWidth();
            jsonWebView.put("visibility", view.isVisibility() && insideWebView ? View.VISIBLE : View.GONE);
            jsonWebView.put("url", view.get$url());
            jsonWebView.put("clickable", view.isEnable_click());
            jsonWebView.put("importantForAccessibility", true);
            jsonWebView.put("is_h5", true);
            jsonWebView.put("is_list_view", view.isIs_list_view());
            jsonWebView.put("element_path", view.get$element_path());
            jsonWebView.put("tag_name", view.getTagName());
            if (!TextUtils.isEmpty(view.get$element_position())) {
                jsonWebView.put("element_position", view.get$element_position());
            }
            // 通过 lib_version 字段用来区分是否可支持 App 内嵌 H5 自定义属性
            mSnapInfo.webLibVersion = view.getLib_version();
            jsonWebView.put("list_selector", view.getList_selector());
            JSONArray classesArray = new JSONArray();
            classesArray.put(view.getTagName());
            Class<?> klass = webView.getClass();
            do {
                String canonicalName = SnapCache.getInstance().getCanonicalName(klass);
                classesArray.put(canonicalName);
                klass = klass.getSuperclass();
            } while (klass != Object.class && klass != null);
            jsonWebView.put("classes", classesArray);

            List<String> list = view.getSubelements();
            JSONArray subviewsArray = new JSONArray();
            if (list != null && list.size() > 0) {
                for (String id : list) {
                    subviewsArray.put(id + webView.hashCode());
                }
            }
            jsonWebView.put("subviews", subviewsArray);
            j.put(jsonWebView);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}