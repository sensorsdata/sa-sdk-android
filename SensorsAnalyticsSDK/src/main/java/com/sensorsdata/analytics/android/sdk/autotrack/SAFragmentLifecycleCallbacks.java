/*
 * Created by dengshiwei on 2021/07/29.
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

package com.sensorsdata.analytics.android.sdk.autotrack;

import android.os.Bundle;
import android.view.View;

public interface SAFragmentLifecycleCallbacks {
    void onCreate(Object object);
    void onViewCreated(Object object, View rootView, Bundle bundle);
    void onStart(Object object);
    void onResume(Object object);
    void onPause(Object object);
    void onStop(Object object);
    void onHiddenChanged(Object object, boolean hidden);
    void setUserVisibleHint(Object object, boolean isVisibleToUser);
}