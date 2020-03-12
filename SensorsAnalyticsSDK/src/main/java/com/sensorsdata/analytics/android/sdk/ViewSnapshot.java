/*
 * Created by wangzhuozhou on 2015/08/01.
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
package com.sensorsdata.analytics.android.sdk;

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
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.visual.SnapInfo;
import com.sensorsdata.analytics.android.sdk.visual.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.VisualUtil;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@TargetApi(SensorsDataAPI.VTRACK_SUPPORTED_MIN_API)
public class ViewSnapshot {

    private static final int MAX_CLASS_NAME_CACHE_SIZE = 255;
    private static final String TAG = "SA.Snapshot";
    private final RootViewFinder mRootViewFinder;
    private final List<PropertyDescription> mProperties;
    private final ClassNameCache mClassnameCache;
    private final Handler mMainThreadHandler;
    private final ResourceIds mResourceIds;
    private String[] mLastImageHashArray = null;
    private SnapInfo mSnapInfo = new SnapInfo();

    public ViewSnapshot(List<PropertyDescription> properties, ResourceIds resourceIds) {
        mProperties = properties;
        mResourceIds = resourceIds;
        mMainThreadHandler = new Handler(Looper.getMainLooper());
        mRootViewFinder = new RootViewFinder();
        mClassnameCache = new ClassNameCache(MAX_CLASS_NAME_CACHE_SIZE);
    }

    public synchronized SnapInfo snapshots(UIThreadSet<Activity> liveActivities, OutputStream out) throws IOException {
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
            SALog.i(TAG,
                    "Screenshot took more than 1 second to be scheduled and executed. No screenshot will be sent.",
                    e);
        } catch (final ExecutionException e) {
            SALog.i(TAG, "Exception thrown during screenshot attempt", e);
        }

        String activityName = null, activityTitle = null;
        final int infoCount = infoList.size();
        for (int i = 0; i < infoCount; i++) {
            final RootViewInfo info = infoList.get(i);
            if (i > 0) {
                writer.write(",");
            }
            if (info != null && info.screenshot != null && isSnapShotUpdated(info.screenshot.getImageHash())) {
                writer.write("{");
                writer.write("\"activity\":");
                activityName = info.activityName;
                activityTitle = info.activityTitle;
                writer.write(JSONObject.quote(info.activityName));
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
        mSnapInfo.screenName = activityName;
        mSnapInfo.title = activityTitle;
        return mSnapInfo;
    }

    public void getVisibleRect(View view, Rect rect, boolean fullscreen) {
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

    private void snapshotView(JsonWriter j, View view, int viewIndex)
            throws IOException {
        j.beginObject();
        j.name("hashCode").value(view.hashCode());
        j.name("id").value(view.getId());
        j.name("index").value(AopUtil.getChildIndex(view.getParent(), view));
        j.name("element_level").value(++mSnapInfo.elementLevel);
        ViewNode viewNode = ViewUtil.getViewNode(view, viewIndex);
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
        }
        if (view instanceof WebView || ViewUtil.instanceOfX5WebView(view)) {
            mSnapInfo.isWebView = true;
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
        /**
         *  对于 Dialog、PopupWindow 类型的非全屏 View 由于 rootView 存在 getTop = 0 / getLeft = 0 的问题，所以先设置了一个全屏 RootView,
         *  再设置次级 View 的实际布局。
         */
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
        j.name("scrollX").value(view.getScrollX());
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
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = group.getChildAt(i);
                // child can be null when views are getting disposed.
                if (null != child) {
                    j.value(child.hashCode());
                }
            }
        }
        j.endArray();
        j.endObject();

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

    public void updateLastImageHashArray(String lastImageHashList) {
        if (lastImageHashList == null || lastImageHashList.length() <= 0) {
            mLastImageHashArray = null;
        } else {
            mLastImageHashArray = lastImageHashList.split(",");
        }
    }

    private boolean isSnapShotUpdated(String newImageHash) {
        if (newImageHash == null || newImageHash.length() <= 0 ||
                mLastImageHashArray == null || mLastImageHashArray.length <= 0) {
            return true;
        }
        for (String temp : mLastImageHashArray) {
            if (temp.equals(newImageHash))
                return false;
        }
        return true;
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
            if (AppSateManager.getInstance().isInBackground()) {
                return mRootViews;
            }
            Activity activity = AppSateManager.getInstance().getForegroundActivity();
            if (activity != null) {
                final String activityName = activity.getClass().getCanonicalName();
                final String activityTitle = SensorsDataUtils.getActivityTitle(activity);
                final Window window = activity.getWindow();
                final View rootView = window.getDecorView().getRootView();
                final RootViewInfo info = new RootViewInfo(activityName, activityTitle, rootView);
                final View[] views = WindowHelper.getSortedWindowViews();
                Bitmap bitmap = null;
                if (views != null && views.length > 0) {
                    bitmap = mergeViewLayers(views, info);
                    for (View view : views) {
                        if (view.getWindowVisibility() != View.VISIBLE || view.getVisibility() != View.VISIBLE
                                || view.getWidth() == 0 || view.getHeight() == 0
                                || TextUtils.equals(WindowHelper.getWindowPrefix(view), WindowHelper.getMainWindowPrefix()))
                            continue;
                        RootViewInfo subInfo = new RootViewInfo(activityName, activityTitle, view.getRootView());
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
                return null;
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
                    view.getLocationOnScreen(windowOffset);
                    canvas.save();
                    canvas.translate((float) windowOffset[0], (float) windowOffset[1]);
                    if (!TextUtils.equals(WindowHelper.getWindowPrefix(view), WindowHelper.getMainWindowPrefix())) {
                        Paint mMaskPaint = new Paint();
                        mMaskPaint.setColor(0xA0000000);
                        canvas.drawRect(-(float) windowOffset[0], -(float) windowOffset[1], canvas.getWidth(), canvas.getHeight(), mMaskPaint);
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
                    byte[] byteArray = imageByte.toByteArray();
                    byte[] md5 = MessageDigest.getInstance("MD5").digest(byteArray);
                    mImageHash = toHex(md5);
                } catch (Exception e) {
                    SALog.i(TAG, "CachedBitmap.recreate;Create image_hash error=" + e);
                }
            }
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

        public String getImageHash() {
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
        final String activityName;
        final String activityTitle;
        final View rootView;
        CachedBitmap screenshot;
        float scale;

        RootViewInfo(String activityName, String activityTitle, View rootView) {
            this.activityName = activityName;
            this.activityTitle = activityTitle;
            this.rootView = rootView;
            this.screenshot = null;
            this.scale = 1.0f;
        }
    }
}