/*
 * Created by wangzhuozhou on 2015/08/01.
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
package com.sensorsdata.analytics.android.sdk.visual;

import android.annotation.TargetApi;
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
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.DisplayMetrics;
import android.util.JsonWriter;
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
import com.sensorsdata.analytics.android.sdk.visual.snap.SoftWareCanvas;
import com.sensorsdata.analytics.android.sdk.visual.snap.UIThreadSet;
import com.sensorsdata.analytics.android.sdk.visual.util.Dispatcher;
import com.sensorsdata.analytics.android.sdk.visual.util.VisualUtil;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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

@TargetApi(SensorsDataAPI.VTRACK_SUPPORTED_MIN_API)
public class ViewSnapshot {

    private static final int MAX_CLASS_NAME_CACHE_SIZE = 255;
    private static final int JS_NOT_INTEGRATED_ALERT_TIME_OUT = 5000;
    private static final String TAG = "SA.Snapshot";
    private final RootViewFinder mRootViewFinder;
    private final List<PropertyDescription> mProperties;
    private final ClassNameCache mClassnameCache;
    private final Handler mMainThreadHandler;
    private AlertRunnable mAlertRunnable;
    private final ResourceIds mResourceIds;
    private SnapInfo mSnapInfo = new SnapInfo();

    public ViewSnapshot(List<PropertyDescription> properties, ResourceIds resourceIds) {
        mProperties = properties;
        mResourceIds = resourceIds;
        mMainThreadHandler = new Handler(Looper.getMainLooper());
        mRootViewFinder = new RootViewFinder();
        mClassnameCache = new ClassNameCache(MAX_CLASS_NAME_CACHE_SIZE);
    }

    public synchronized SnapInfo snapshots(UIThreadSet<Activity> liveActivities, OutputStream out, StringBuilder lastImageHash) throws IOException {
        final FutureTask<List<RootViewInfo>> infoFuture =
                new FutureTask<List<RootViewInfo>>(mRootViewFinder);
        mMainThreadHandler.post(infoFuture);

        final OutputStreamWriter writer = new OutputStreamWriter(out);
        List<RootViewInfo> infoList = Collections.emptyList();
        writer.write("[");

        try {
            infoList = infoFuture.get(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            SALog.i(TAG, "Screenshot interrupted, no screenshot will be sent.", e);
        } catch (final TimeoutException e) {
            SALog.i(TAG, "Screenshot took more than 1 second to be scheduled and executed. No screenshot will be sent.", e);
        } catch (final ExecutionException e) {
            SALog.i(TAG, "Exception thrown during screenshot attempt", e);
        }

        String screenName = null, activityTitle = null;
        final int infoCount = infoList.size();
        for (int i = 0; i < infoCount; i++) {
            final RootViewInfo info = infoList.get(i);
            if (i > 0) {
                writer.write(",");
            }
            if (info != null && info.screenshot != null && isSnapShotUpdated(info.screenshot.getImageHash(), lastImageHash)) {
                writer.write("{");
                writer.write("\"activity\":");
                screenName = info.screenName;
                activityTitle = info.activityTitle;
                writer.write(JSONObject.quote(info.screenName));
                writer.write(",");
                writer.write("\"scale\":");
                writer.write(String.format("%s", info.scale));
                writer.write(",");
                writer.write("\"serialized_objects\":");
                {
                    final JsonWriter j = new JsonWriter(writer);
                    j.beginObject();
                    j.name("rootObject").value(info.rootView.hashCode());
                    j.name("objects");
                    snapshotViewHierarchy(j, info.rootView);
                    j.endObject();
                    j.flush();
                }
                writer.write(",");
                writer.write("\"image_hash\":");
                writer.write(JSONObject.quote(info.screenshot.getImageHash()));
                writer.write(",");
                writer.write("\"screenshot\":");
                writer.flush();
                info.screenshot.writeBitmapJSON(Bitmap.CompressFormat.PNG, 70, out);
                writer.write("}");
            } else {
                writer.write("{}");
            }
        }
        writer.write("]");
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
        view.getLocalVisibleRect(rect);
        rect.offset(offset[0], offset[1]);
    }

    private void snapshotViewHierarchy(JsonWriter j, View rootView)
            throws IOException {
        reset();
        j.beginArray();
        snapshotView(j, rootView, 0);
        j.endArray();
    }

    private void reset() {
        mSnapInfo = new SnapInfo();
        ViewUtil.clear();
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


    private void snapshotView(final JsonWriter j, final View view, int viewIndex)
            throws IOException {
        // 处理内嵌 H5 页面
        if (ViewUtil.isViewSelfVisible(view)) {
            List<String> webNodeIds = null;
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
                                WebNodeInfo webNodeInfo = WebNodesManager.getInstance().getWebNodes(url);
                                //获取不到页面元素有两种可能 1. 未集成 JS SDK 2. WebView 在扫码前已经打开。这里针对第二种情况尝试通知 JS 获取数据。
                                if (webNodeInfo == null) {
                                    //WebView 扫码前已打开，此时需要通知 JS 发送数据
                                    SensorsDataAutoTrackHelper.loadUrl(view, "javascript:window.sensorsdata_app_call_js('visualized')");
                                }
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
                                    webNodeIds.add(webNode.getId());
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
            j.beginObject();
            j.name("hashCode").value(view.hashCode());
            j.name("id").value(view.getId());
            j.name("index").value(VisualUtil.getChildIndex(view.getParent(), view));
            j.name("element_level").value(++mSnapInfo.elementLevel);
            j.name("element_selector").value(ViewUtil.getElementSelector(view));

            JSONObject object = VisualUtil.getScreenNameAndTitle(view, mSnapInfo);
            if (object != null) {
                String screenName = object.optString(AopConstants.SCREEN_NAME);
                String title = object.optString(AopConstants.TITLE);
                if (!TextUtils.isEmpty(screenName)) {
                    j.name("screen_name").value(screenName);
                }
                if (!TextUtils.isEmpty(title)) {
                    j.name("title").value(title);
                }
            }

            ViewNode viewNode = ViewUtil.getViewNode(view, viewIndex, true);
            if (viewNode != null) {
                if (!TextUtils.isEmpty(viewNode.getViewPath())) {
                    j.name("element_path").value(viewNode.getViewPath());
                }
                if (!TextUtils.isEmpty(viewNode.getViewPosition())) {
                    j.name("element_position").value(viewNode.getViewPosition());
                }
                if (!TextUtils.isEmpty(viewNode.getViewContent()) && VisualUtil.isSupportElementContent(view)) {
                    j.name("element_content").value(viewNode.getViewContent());
                }
                j.name("is_list_view").value(viewNode.isListView());
            }

            j.name("sa_id_name").value(getResName(view));
            try {
                String saId = (String) view.getTag(R.id.sensors_analytics_tag_view_id);
                if (!TextUtils.isEmpty(saId)) {
                    j.name("sa_id_name").value(saId);
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
                    j.name("top").value(view.getTop());
                    j.name("left").value(view.getLeft());
                    j.name("width").value(screenWidth);
                    j.name("height").value(screenHeight);
                } else {
                    ViewParent parent = view.getParent();
                    if (parent != null && WindowHelper.isDecorView(parent.getClass())) {
                        Rect rect = new Rect();
                        getVisibleRect(view, rect, false);
                        j.name("top").value(rect.top);
                        j.name("left").value(rect.left);
                        j.name("width").value(rect.width());
                        j.name("height").value(rect.height());
                    } else {
                        j.name("top").value(view.getTop());
                        j.name("left").value(view.getLeft());
                        j.name("width").value(view.getWidth());
                        j.name("height").value(view.getHeight());
                    }
                }
            } else {
                j.name("top").value(view.getTop());
                j.name("left").value(view.getLeft());
                j.name("width").value(view.getWidth());
                j.name("height").value(view.getHeight());
            }

            int scrollX = view.getScrollX();
            // 适配解决 textView 配置了 maxLines = 1 和 gravity = center|right 时 scrollX 属性异常问题
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                if (textView.getMaxLines() == 1) {
                    scrollX = 0;
                }
            }
            j.name("scrollX").value(scrollX);
            j.name("scrollY").value(view.getScrollY());
            j.name("visibility").value(VisualUtil.getVisibility(view));

            float translationX = 0;
            float translationY = 0;
            if (Build.VERSION.SDK_INT >= 11) {
                translationX = view.getTranslationX();
                translationY = view.getTranslationY();
            }
            j.name("translationX").value(translationX);
            j.name("translationY").value(translationY);

            j.name("classes");
            j.beginArray();
            Class<?> klass = view.getClass();
            do {
                j.value(mClassnameCache.get(klass));
                klass = klass.getSuperclass();
            } while (klass != Object.class && klass != null);
            j.endArray();

            addProperties(j, view);

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) layoutParams;
                int[] rules = relativeLayoutParams.getRules();
                j.name("layoutRules");
                j.beginArray();
                for (int rule : rules) {
                    j.value(rule);
                }
                j.endArray();
            }

            j.name("subviews");
            j.beginArray();
            // 添加 WebView 控件所有子元素
            if (webNodeIds != null && webNodeIds.size() > 0) {
                for (String id : webNodeIds) {
                    j.value(id);
                }
            } else if (view instanceof ViewGroup) {
                final ViewGroup group = (ViewGroup) view;
                final int childCount = group.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final View child = group.getChildAt(i);
                    if (null != child) {
                        j.value(child.hashCode());
                    }
                }
            }
            j.endArray();
            j.endObject();
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

    private void addProperties(JsonWriter j, View v)
            throws IOException {
        final Class<?> viewClass = v.getClass();
        for (final PropertyDescription desc : mProperties) {
            if (desc.targetClass.isAssignableFrom(viewClass) && null != desc.accessor) {
                final Object value = desc.accessor.applyMethod(v);
                if (null == value) {
                    // Don't produce anything in this case
                } else if (value instanceof Number) {
                    j.name(desc.name).value((Number) value);
                } else if (value instanceof Boolean) {
                    boolean clickable = (boolean) value;
                    if (TextUtils.equals("clickable", desc.name)) {
                        if (VisualUtil.isSupportClick(v)) {
                            clickable = true;
                        } else if (VisualUtil.isForbiddenClick(v)) {
                            clickable = false;
                        }
                    }
                    j.name(desc.name).value(clickable);
                } else if (value instanceof ColorStateList) {
                    j.name(desc.name).value((Integer) ((ColorStateList) value).getDefaultColor());
                } else if (value instanceof Drawable) {
                    final Drawable drawable = (Drawable) value;
                    final Rect bounds = drawable.getBounds();
                    j.name(desc.name);
                    j.beginObject();
                    j.name("classes");
                    j.beginArray();
                    Class klass = drawable.getClass();
                    while (klass != Object.class) {
                        j.value(klass.getCanonicalName());
                        klass = klass.getSuperclass();
                    }
                    j.endArray();
                    j.name("dimensions");
                    j.beginObject();
                    j.name("left").value(bounds.left);
                    j.name("right").value(bounds.right);
                    j.name("top").value(bounds.top);
                    j.name("bottom").value(bounds.bottom);
                    j.endObject();
                    if (drawable instanceof ColorDrawable) {
                        final ColorDrawable colorDrawable = (ColorDrawable) drawable;
                        j.name("color").value(colorDrawable.getColor());
                    }
                    j.endObject();
                } else {
                    j.name(desc.name).value(value.toString());
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
        boolean isUpdated = !TextUtils.equals(newImageHash, lastImageHash) || WebNodesManager.getInstance().hasH5AlertInfo();
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
            Activity activity = AppStateManager.getInstance().getForegroundActivity();
            if (activity != null) {
                JSONObject object = AopUtil.buildTitleAndScreenName(activity);
                VisualUtil.mergeRnScreenNameAndTitle(object);
                String screenName = object.optString(AopConstants.SCREEN_NAME);
                String activityTitle = object.optString(AopConstants.TITLE);
                final Window window = activity.getWindow();
                final View rootView = window.getDecorView().getRootView();
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
                        RootViewInfo subInfo = new RootViewInfo(screenName, activityTitle, view.getRootView());
                        scaleBitmap(subInfo, bitmap);
                        mRootViews.add(subInfo);
                    }
                }
                if (mRootViews.size() == 0) {
                    scaleBitmap(info, bitmap);
                    mRootViews.add(info);
                }
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
            boolean skipOther;
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
                        if (WindowHelper.isDialogOrPopupWindow(view)) {
                            Paint mMaskPaint = new Paint();
                            mMaskPaint.setColor(0xA0000000);
                            canvas.drawRect(-(float) windowOffset[0], -(float) windowOffset[1], canvas.getWidth(), canvas.getHeight(), mMaskPaint);
                        }
                    }
                    view.draw(canvas);
                    canvas.restore();
                    canvas.destroy();
                }
            }
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
                } catch (final OutOfMemoryError e) {
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
                final Base64OutputStream imageOut = new Base64OutputStream(out, Base64.NO_WRAP);
                mCached.compress(Bitmap.CompressFormat.PNG, 100, imageOut);
                imageOut.flush();
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

    private void mergeWebViewNodes(JsonWriter j, WebNode view, View webView, float webViewScale) {
        try {
            j.beginObject();
            j.name("hashCode").value(view.getId());
            j.name("index").value(0);
            if (!TextUtils.isEmpty(view.get$element_selector())) {
                j.name("element_selector").value(view.get$element_selector());
            }
            if (!TextUtils.isEmpty(view.get$element_content())) {
                j.name("element_content").value(view.get$element_content());
            }
            j.name("element_level").value(++mSnapInfo.elementLevel);
            j.name("h5_title").value(view.get$title());
            float scale = view.getScale();
            if (webViewScale == 0) {
                webViewScale = scale;
            }
            // 原生 WebView getScrollX 能取到值，而 X5WebView 始终是 0
            float left = 0f, top = 0f;
            if (webView.getScrollX() == 0) {
                left = view.getLeft() * webViewScale;
            } else {
                left = (view.getLeft() + view.getScrollX()) * webViewScale;
            }
            if (webView.getScrollY() == 0) {
                top = view.getTop() * webViewScale;
            } else {
                top = (view.getTop() + view.getScrollY()) * webViewScale;
            }
            j.name("left").value((int) left);
            j.name("top").value((int) top);
            j.name("width").value((int) (view.getWidth() * webViewScale));
            j.name("height").value((int) (view.getHeight() * webViewScale));
            j.name("scrollX").value(0);
            j.name("scrollY").value(0);
            j.name("visibility").value(view.isVisibility() ? View.VISIBLE : View.GONE);
            j.name("url").value(view.get$url());
            j.name("clickable").value(true);
            j.name("importantForAccessibility").value(true);
            j.name("is_h5").value(true);

            j.name("classes");
            j.beginArray();
            j.value(view.getTagName());
            Class<?> klass = webView.getClass();
            do {
                j.value(klass.getCanonicalName());
                klass = klass.getSuperclass();
            } while (klass != Object.class && klass != null);
            j.endArray();

            List<String> list = view.getSubelements();
            if (list != null && list.size() > 0) {
                j.name("subviews");
                j.beginArray();
                for (String id : list) {
                    j.value(id);
                }
                j.endArray();
            }
            j.endObject();
        } catch (IOException e) {
            SALog.printStackTrace(e);
        }

    }
}