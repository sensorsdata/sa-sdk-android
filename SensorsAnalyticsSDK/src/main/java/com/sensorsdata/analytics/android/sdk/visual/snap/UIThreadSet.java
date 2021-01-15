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

import android.os.Looper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper around a set that will throw RuntimeErrors if accessed in a thread that is not the main thread.
 */
public class UIThreadSet<T> {
    private Set<T> mSet;

    public UIThreadSet() {
        mSet = new HashSet<T>();
    }

    public void add(T item) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Can't add an activity when not on the UI thread");
        }
        mSet.add(item);
    }

    public void remove(T item) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Can't remove an activity when not on the UI thread");
        }
        mSet.remove(item);
    }

    public Set<T> getAll() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Can't remove an activity when not on the UI thread");
        }
        return Collections.unmodifiableSet(mSet);
    }

    public boolean isEmpty() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Can't check isEmpty() when not on the UI thread");
        }
        return mSet.isEmpty();
    }
}
