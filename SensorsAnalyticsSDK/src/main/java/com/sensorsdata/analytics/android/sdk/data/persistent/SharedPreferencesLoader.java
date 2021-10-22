/*
 * Created by dengshiwei on 2021/04/07.
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

package com.sensorsdata.analytics.android.sdk.data.persistent;

import android.content.Context;
import android.content.SharedPreferences;

import com.sensorsdata.analytics.android.sdk.util.SASpUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

class SharedPreferencesLoader {

    private final Executor mExecutor;

    SharedPreferencesLoader() {
        mExecutor = Executors.newSingleThreadExecutor();
    }

    Future<SharedPreferences> loadPreferences(Context context, String name) {
        final LoadSharedPreferences loadSharedPrefs =
                new LoadSharedPreferences(context, name);
        final FutureTask<SharedPreferences> task = new FutureTask<>(loadSharedPrefs);
        mExecutor.execute(task);
        return task;
    }

    private static class LoadSharedPreferences implements Callable<SharedPreferences> {
        private final Context mContext;
        private final String mPrefsName;

        LoadSharedPreferences(Context context, String prefsName) {
            mContext = context;
            mPrefsName = prefsName;
        }

        @Override
        public SharedPreferences call() {
            return SASpUtils.getSharedPreferences(mContext, mPrefsName, Context.MODE_PRIVATE);
        }
    }
}
