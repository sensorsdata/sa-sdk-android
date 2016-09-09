package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/* package */ class SharedPreferencesLoader {

    /* package */ interface OnPrefsLoadedListener {
        void onPrefsLoaded(SharedPreferences prefs);
    }

    public SharedPreferencesLoader() {
        mExecutor = Executors.newSingleThreadExecutor();
    }

    public Future<SharedPreferences> loadPreferences(Context context, String name,
                                                     OnPrefsLoadedListener listener) {
        final LoadSharedPreferences loadSharedPrefs =
                new LoadSharedPreferences(context, name, listener);
        final FutureTask<SharedPreferences> task = new FutureTask<SharedPreferences>(loadSharedPrefs);
        mExecutor.execute(task);
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


    private final Executor mExecutor;
}
