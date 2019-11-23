/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2019 Sensors Data Inc.
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
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.DisplayMetrics;
import android.util.JsonWriter;
import android.util.LruCache;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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

    public ViewSnapshot(List<PropertyDescription> properties, ResourceIds resourceIds) {
        mProperties = properties;
        mResourceIds = resourceIds;
        mMainThreadHandler = new Handler(Looper.getMainLooper());
        mRootViewFinder = new RootViewFinder();
        mClassnameCache = new ClassNameCache(MAX_CLASS_NAME_CACHE_SIZE);
    }

    public synchronized String snapshots(UIThreadSet<Activity> liveActivities, OutputStream out) throws IOException {
        mRootViewFinder.findInActivities(liveActivities);
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

        String activityName = null;
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
                info.screenshot.writeBitmapJSON(Bitmap.CompressFormat.PNG, 100, out);
                writer.write("}");
            } else {
                writer.write("{}");
            }
        }
        writer.write("]");
        writer.flush();
        return activityName;
    }

    private void snapshotViewHierarchy(JsonWriter j, View rootView)
            throws IOException {
        j.beginArray();
        snapshotView(j, rootView);
        j.endArray();
    }

    private void snapshotView(JsonWriter j, View view)
            throws IOException {
        j.beginObject();
        j.name("hashCode").value(view.hashCode());
        j.name("id").value(view.getId());
        j.name("index").value(getChildIndex(view.getParent(), view));
        j.name("sa_id_name").value(getResName(view));

        try {
            String saId = (String) view.getTag(R.id.sensors_analytics_tag_view_id);
            if (!TextUtils.isEmpty(saId)) {
                j.name("sa_id_name").value(saId);
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }

        j.name("top").value(view.getTop());
        j.name("left").value(view.getLeft());
        j.name("width").value(view.getWidth());
        j.name("height").value(view.getHeight());
        j.name("scrollX").value(view.getScrollX());
        j.name("scrollY").value(view.getScrollY());
        j.name("visibility").value(view.getVisibility());

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
                    snapshotView(j, child);
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

                    //针对部分类型控件，clickable 特殊处理
                    if (TextUtils.equals("clickable", desc.name)) {
                        // RatingBar 或 SeeekBar 类型，clickable 为 true
                        if (v instanceof RatingBar || v instanceof SeekBar) {
                            clickable = true;
                        } else if (isForbiddenClick(v)) {
                            clickable = false;
                        } else {
                            // ListView item 类型，clickable 为 true
                            ViewParent parent = v.getParent();
                            if (parent instanceof ListView) {
                                clickable = true;
                            }
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

    private boolean isForbiddenClick(View v) {
        if (v instanceof ListView || v instanceof Spinner || v instanceof EditText || isNavigationMenuItemView(v)) {
            return true;
        }

        return isOtherForbiddenClick(v);
    }

    private boolean isNavigationMenuItemView(View view) {
        try {
            Class<?> tabClass = Class.forName("android.support.design.internal.NavigationMenuItemView");
            if (tabClass.isAssignableFrom(view.getClass())) {
                return true;
            }
        } catch (Exception e) {
            //不需要打日志
        }
        return false;
    }

    private boolean isOtherForbiddenClick(View v) {
        if (isAssignableFromClass(v, "android.support.v7.widget.Toolbar")) {
            return true;
        } else return isAssignableFromClass(v, "android.support.design.widget.TabLayout");
    }

    private boolean isAssignableFromClass(View v, String className) {
        try {
            Class<?> someClass = Class.forName(className);
            if (someClass.isAssignableFrom(v.getClass())) {
                return true;
            } else {
                ViewParent viewParent = v.getParent();
                if (viewParent == null) {
                    return false;
                }
                if (!(viewParent instanceof View)) {
                    return false;
                }
                return isAssignableFromClass((View) viewParent, className);
            }
        } catch (Exception e) {
            //不需要打日志
        }
        return false;
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

    private int getChildIndex(ViewParent parent, View child) {
        if (!(parent instanceof ViewGroup)) {
            return -1;
        }

        ViewGroup _parent = (ViewGroup) parent;

        final String childIdName = getResName(child);

        String childClassName = mClassnameCache.get(child.getClass());
        int index = 0;
        for (int i = 0; i < _parent.getChildCount(); i++) {
            View brother = _parent.getChildAt(i);

            if (!Pathfinder.hasClassName(brother, childClassName)) {
                continue;
            }

            String brotherIdName = getResName(brother);

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
        private final DisplayMetrics mDisplayMetrics;
        private final CachedBitmap mCachedBitmap;
        private final int mClientDensity = DisplayMetrics.DENSITY_DEFAULT;
        private UIThreadSet<Activity> mLiveActivities;
        private HandlerThread mHandlerThread;
        private Handler mHandler;


        public RootViewFinder() {
            mDisplayMetrics = new DisplayMetrics();
            mRootViews = new ArrayList<RootViewInfo>();
            mCachedBitmap = new CachedBitmap();
        }

        public void findInActivities(UIThreadSet<Activity> liveActivities) {
            mLiveActivities = liveActivities;
        }

        @Override
        public List<RootViewInfo> call() throws Exception {
            mRootViews.clear();
            final Set<Activity> liveActivities = mLiveActivities.getAll();
            for (final Activity a : liveActivities) {
                final Window window = a.getWindow();
                final View rootView = window.getDecorView().getRootView();
                if (rootView.getWidth() == 0 || rootView.getHeight() == 0) {
                    continue;
                }
                final String activityName = a.getClass().getCanonicalName();
                a.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
                final RootViewInfo info = new RootViewInfo(activityName, rootView, window);
                mRootViews.add(info);
            }
            final int viewCount = mRootViews.size();
            for (int i = 0; i < viewCount; i++) {
                final RootViewInfo info = mRootViews.get(i);
                takeScreenshot(info);
            }
            return mRootViews;
        }


        private void takeScreenshot(final RootViewInfo info) {
            final View rootView = info.rootView;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    final Bitmap rawBitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
                    final CountDownLatch countDownLatch = new CountDownLatch(1);
                    if (mHandlerThread == null) {
                        mHandlerThread = new HandlerThread(TAG);
                    }
                    mHandlerThread.start();
                    if (mHandler == null) {
                        mHandler = new Handler(mHandlerThread.getLooper());
                    }
                    PixelCopy.request(info.window, rawBitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                        @Override
                        public void onPixelCopyFinished(int copyResult) {
                            try {
                                if (copyResult == PixelCopy.SUCCESS && rawBitmap != null) {
                                    SALog.i(TAG, "PixelCopy success.");
                                    scaleBitmap(info, rawBitmap);
                                } else {
                                    SALog.i(TAG, "PixelCopy fail, copyResult :" + copyResult);
                                }
                                mHandlerThread.quitSafely();
                                countDownLatch.countDown();
                            } catch (Throwable t) {
                                SALog.i(TAG, "Can't take a bitmap snapshot of view " + rootView + ", skipping for now.");
                            }
                        }
                    }, mHandler);
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        SALog.printStackTrace(e);
                    }
                } catch (RuntimeException e) {
                    SALog.i(TAG, "Can't take a bitmap snapshot of view " + rootView + ", skipping for now.", e);
                }
            } else {
                Boolean originalCacheState;
                try {
                    originalCacheState = rootView.isDrawingCacheEnabled();
                    rootView.setDrawingCacheEnabled(true);
                    rootView.buildDrawingCache(true);
                    scaleBitmap(info, rootView.getDrawingCache());
                    if (null != originalCacheState && !originalCacheState) {
                        rootView.setDrawingCacheEnabled(false);
                    }
                } catch (final RuntimeException e) {
                    SALog.i(TAG, "Can't take a bitmap snapshot of view " + rootView + ", skipping for now.", e);
                }
            }

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
        public final String activityName;
        public final View rootView;
        public final Window window;
        public CachedBitmap screenshot;
        public float scale;

        public RootViewInfo(String activityName, View rootView, Window window) {
            this.activityName = activityName;
            this.rootView = rootView;
            this.window = window;
            this.screenshot = null;
            this.scale = 1.0f;
        }
    }
}