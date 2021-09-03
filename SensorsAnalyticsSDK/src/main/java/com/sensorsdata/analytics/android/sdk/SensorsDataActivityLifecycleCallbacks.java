/*
 * Created by wangzhuozhou on 2017/4/12.
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

package com.sensorsdata.analytics.android.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import java.util.HashSet;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SensorsDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private final Set<SAActivityLifecycleCallbacks> mActivityCallbacks = new HashSet<>();

    public SensorsDataActivityLifecycleCallbacks() {
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        for (SAActivityLifecycleCallbacks activityLifecycleCallbacks : mActivityCallbacks) {
            try {
                activityLifecycleCallbacks.onActivityCreated(activity, bundle);
            } catch (Exception exception) {
                SALog.printStackTrace(exception);
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        for (SAActivityLifecycleCallbacks activityLifecycleCallbacks : mActivityCallbacks) {
            try {
                activityLifecycleCallbacks.onActivityStarted(activity);
            } catch (Exception exception) {
                SALog.printStackTrace(exception);
            }
        }
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        for (SAActivityLifecycleCallbacks activityLifecycleCallbacks : mActivityCallbacks) {
            try {
                activityLifecycleCallbacks.onActivityResumed(activity);
            } catch (Exception exception) {
                SALog.printStackTrace(exception);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        for (SAActivityLifecycleCallbacks activityLifecycleCallbacks : mActivityCallbacks) {
            try {
                activityLifecycleCallbacks.onActivityPaused(activity);
            } catch (Exception exception) {
                SALog.printStackTrace(exception);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        for (SAActivityLifecycleCallbacks activityLifecycleCallbacks : mActivityCallbacks) {
            try {
                activityLifecycleCallbacks.onActivityStopped(activity);
            } catch (Exception exception) {
                SALog.printStackTrace(exception);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        for (SAActivityLifecycleCallbacks activityLifecycleCallbacks : mActivityCallbacks) {
            try {
                activityLifecycleCallbacks.onActivitySaveInstanceState(activity, bundle);
            } catch (Exception exception) {
                SALog.printStackTrace(exception);
            }
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        for (SAActivityLifecycleCallbacks activityLifecycleCallbacks : mActivityCallbacks) {
            try {
                activityLifecycleCallbacks.onActivityDestroyed(activity);
            } catch (Exception exception) {
                SALog.printStackTrace(exception);
            }
        }
    }

    public void addActivityLifecycleCallbacks(SAActivityLifecycleCallbacks callbacks) {
        mActivityCallbacks.add(callbacks);
    }

    public interface SAActivityLifecycleCallbacks extends Application.ActivityLifecycleCallbacks {
        void onNewIntent(Intent intent);
    }
}