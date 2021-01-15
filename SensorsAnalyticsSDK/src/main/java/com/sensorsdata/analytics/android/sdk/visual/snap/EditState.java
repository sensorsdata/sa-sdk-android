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

package com.sensorsdata.analytics.android.sdk.visual.snap;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles applying and managing the life cycle of edits in an application. Clients
 * can replace all of the edits in an app with}.
 * Some client is responsible for informing the EditState about the presence or absence
 * of Activities, by calling {@link EditState#add(Activity)} and {@link EditState#remove(Activity)}
 */
public class EditState extends UIThreadSet<Activity> {

    private static final String LOGTAG = "SA.EditState";
    private final Handler mUiThreadHandler;
    private final Map<String, List<ViewVisitor>> mIntendedEdits;
    private final Map<Activity, Set<EditBinding>> mCurrentEdits;

    public EditState() {
        mUiThreadHandler = new Handler(Looper.getMainLooper());
        mIntendedEdits = new HashMap<>();
        mCurrentEdits = new HashMap<>();
    }

    /**
     * Should be called whenever a new Activity appears in the application.
     */
    @Override
    public void add(Activity newOne) {
        super.add(newOne);
//    applyEditsOnUiThread();
        applyEditsOnActivity(newOne);
    }

    /**
     * Should be called whenever an activity leaves the application, or is otherwise no longer relevant to our edits.
     */
    @Override
    public void remove(Activity oldOne) {
        super.remove(oldOne);
        removeChangesOnActivity(oldOne);
    }

    private void applyEditsOnActivity(Activity activity) {
        final String activityName = activity.getClass().getCanonicalName();
        final View rootView = activity.getWindow().getDecorView().getRootView();

        final List<ViewVisitor> specificChanges;
        final List<ViewVisitor> wildcardChanges;
        synchronized (mIntendedEdits) {
            specificChanges = mIntendedEdits.get(activityName);
            wildcardChanges = mIntendedEdits.get(null);
        }

        if (null != specificChanges) {
            applyChangesFromList(activity, rootView, specificChanges);
        }

        if (null != wildcardChanges) {
            applyChangesFromList(activity, rootView, wildcardChanges);
        }
    }

    // Must be called on UI Thread
    private void applyChangesFromList(final Activity activity, final View rootView,
                                      final List<ViewVisitor> changes) {
        synchronized (mCurrentEdits) {
            if (!mCurrentEdits.containsKey(activity)) {
                mCurrentEdits.put(activity, new HashSet<EditBinding>());
            }

            final int size = changes.size();
            for (int i = 0; i < size; i++) {
                final ViewVisitor visitor = changes.get(i);
                final EditBinding binding = new EditBinding(rootView, visitor, mUiThreadHandler);
                mCurrentEdits.get(activity).add(binding);
            }
        }
    }

    private void removeChangesOnActivity(Activity activity) {
        synchronized (mCurrentEdits) {
            final Set<EditBinding> bindingSet = mCurrentEdits.get(activity);
            if (bindingSet == null) {
                return;
            }

            for (final EditBinding binding : bindingSet) {
                binding.kill();
            }

            mCurrentEdits.remove(activity);
        }
    }

    /* The binding between a bunch of edits and a view. Should be instantiated and live on the UI thread */
    private static class EditBinding implements ViewTreeObserver.OnGlobalLayoutListener, Runnable {

        private final WeakReference<View> mViewRoot;
        private final ViewVisitor mEdit;
        private final Handler mHandler;
        private volatile boolean mDying;
        private boolean mAlive;

        public EditBinding(View viewRoot, ViewVisitor edit, Handler uiThreadHandler) {
            mEdit = edit;
            mViewRoot = new WeakReference<View>(viewRoot);
            mHandler = uiThreadHandler;
            mAlive = true;
            mDying = false;

            final ViewTreeObserver observer = viewRoot.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.addOnGlobalLayoutListener(this);
            }
            run();
        }

        @Override
        public void onGlobalLayout() {
            run();
        }

        @Override
        public void run() {
            if (!mAlive) {
                return;
            }

            final View viewRoot = mViewRoot.get();
            if (null == viewRoot || mDying) {
                cleanUp();
                return;
            }

            // ELSE View is alive and we are alive
            mEdit.visit(viewRoot);

            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, 5000);
        }

        public void kill() {
            mDying = true;
            mHandler.post(this);
        }

        private void cleanUp() {
            if (mAlive) {
                final View viewRoot = mViewRoot.get();
                if (null != viewRoot) {
                    final ViewTreeObserver observer = viewRoot.getViewTreeObserver();
                    if (observer.isAlive()) {
                        if (Build.VERSION.SDK_INT < 16) {
                            observer.removeGlobalOnLayoutListener(this);
                        } else {
                            observer.removeOnGlobalLayoutListener(this);
                        }
                    }
                }
                mEdit.cleanup();
            }
            mAlive = false;
        }
    }
}
