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
 
package com.sensorsdata.analytics.android.sdk.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.sensorsdata.analytics.android.sdk.SensorsDataThreadPool;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/* package */ class SharedPreferencesLoader {

    /* package */ interface OnPrefsLoadedListener {
        void onPrefsLoaded(SharedPreferences prefs);
    }

    public SharedPreferencesLoader() {
        sensorsDataThreadPool = SensorsDataThreadPool.getInstance();
    }

    public Future<SharedPreferences> loadPreferences(Context context, String name,
                                                     OnPrefsLoadedListener listener) {
        final LoadSharedPreferences loadSharedPrefs =
                new LoadSharedPreferences(context, name, listener);
        final FutureTask<SharedPreferences> task = new FutureTask<SharedPreferences>(loadSharedPrefs);
        sensorsDataThreadPool.execute(task);
        return task;
    }

    private static class LoadSharedPreferences implements Callable<SharedPreferences> {
        public LoadSharedPreferences(Context context, String prefsName,
                                     OnPrefsLoadedListener listener) {
            mContext = context;
            mPrefsName = prefsName;
            mListener = listener;
        }

        @Override
        public SharedPreferences call() {
            final SharedPreferences ret = mContext.getSharedPreferences(mPrefsName, Context.MODE_PRIVATE);
            if (null != mListener) {
                mListener.onPrefsLoaded(ret);
            }
            return ret;
        }

        private final Context mContext;
        private final String mPrefsName;
        private final OnPrefsLoadedListener mListener;
    }


    private final SensorsDataThreadPool sensorsDataThreadPool;
}
