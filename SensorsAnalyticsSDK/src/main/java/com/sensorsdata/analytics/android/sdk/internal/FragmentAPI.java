/*
 * Created by dengshiwei on 2019/09/12.
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
package com.sensorsdata.analytics.android.sdk.internal;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppViewScreen;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppViewScreenAndAppClick;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class FragmentAPI implements IFragmentAPI {
    private static final String TAG = "FragmentAPI";
    /* $AppViewScreen 事件是否支持 Fragment*/
    private boolean mTrackFragmentAppViewScreen;
    private Set<Integer> mAutoTrackFragments;
    private Set<Integer> mAutoTrackIgnoredFragments;

    public FragmentAPI(Context context) {
        ArrayList<String> autoTrackFragments = getAutoTrackFragments(context);
        if (autoTrackFragments.size() > 0) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
            for (String fragment : autoTrackFragments) {
                mAutoTrackFragments.add(fragment.hashCode());
            }
        }
    }

    @Override
    public void trackFragmentAppViewScreen() {
        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null) {
                return;
            }

            if (mAutoTrackFragments == null) {
                mAutoTrackFragments = new CopyOnWriteArraySet<>();
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackFragments.add(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        if (fragmentsList == null || fragmentsList.size() == 0) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            String canonicalName;
            for (Class fragment : fragmentsList) {
                canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    mAutoTrackFragments.add(canonicalName.hashCode());
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        if (fragment == null) {
            return false;
        }
        try {
            if (mAutoTrackFragments != null && mAutoTrackFragments.size() > 0) {
                String canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    return mAutoTrackFragments.contains(canonicalName.hashCode());
                }
            }

            if (fragment.getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
                return false;
            }

            if (fragment.getAnnotation(SensorsDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
                return false;
            }

            if (mAutoTrackIgnoredFragments != null && mAutoTrackIgnoredFragments.size() > 0) {
                String canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    return !mAutoTrackIgnoredFragments.contains(canonicalName.hashCode());
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        return true;
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {
        try {
            if (fragmentList == null || fragmentList.size() == 0) {
                return;
            }

            if (mAutoTrackIgnoredFragments == null) {
                mAutoTrackIgnoredFragments = new CopyOnWriteArraySet<>();
            }

            for (Class<?> fragment : fragmentList) {
                if (fragment != null) {
                    String canonicalName = fragment.getCanonicalName();
                    if (!TextUtils.isEmpty(canonicalName)) {
                        mAutoTrackIgnoredFragments.add(canonicalName.hashCode());
                    }
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null) {
                return;
            }

            if (mAutoTrackIgnoredFragments == null) {
                mAutoTrackIgnoredFragments = new CopyOnWriteArraySet<>();
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackIgnoredFragments.add(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {
        try {
            if (fragmentList == null || fragmentList.size() == 0 ||
                    mAutoTrackIgnoredFragments == null) {
                return;
            }

            for (Class fragment : fragmentList) {
                if (fragment != null) {
                    String canonicalName = fragment.getCanonicalName();
                    if (!TextUtils.isEmpty(canonicalName)) {
                        mAutoTrackIgnoredFragments.remove(canonicalName.hashCode());
                    }
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null || mAutoTrackIgnoredFragments == null) {
                return;
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackIgnoredFragments.remove(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 读取配置配置的 AutoTrack 的 Fragment
     *
     * @param context Context
     * @return ArrayList Fragment 列表
     */
    private static ArrayList<String> getAutoTrackFragments(Context context) {
        ArrayList<String> autoTrackFragments = new ArrayList<>();
        BufferedReader bf = null;
        try {
            //获取assets资源管理器
            AssetManager assetManager = context.getAssets();
            //通过管理器打开文件并读取
            bf = new BufferedReader(new InputStreamReader(
                    assetManager.open("sa_autotrack_fragment.config")));
            String line;
            while ((line = bf.readLine()) != null) {
                if (!TextUtils.isEmpty(line) && !line.startsWith("#")) {
                    autoTrackFragments.add(line);
                }
            }
        } catch (FileNotFoundException ex) {
            SALog.d(TAG, "SensorsDataAutoTrackFragment file doesn't exists.");
        } catch (IOException e) {
            SALog.printStackTrace(e);
        } finally {
            if (bf != null) {
                try {
                    bf.close();
                } catch (IOException e) {
                    SALog.printStackTrace(e);
                }
            }
        }
        return autoTrackFragments;
    }
}
