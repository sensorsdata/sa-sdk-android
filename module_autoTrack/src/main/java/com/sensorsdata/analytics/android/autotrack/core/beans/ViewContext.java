/*
 * Created by dengshiwei on 2022/07/07.
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

package com.sensorsdata.analytics.android.autotrack.core.beans;

import android.app.Activity;
import android.view.View;

public class ViewContext {
    public Activity activity;
    public Object fragment;
    public View view;

    public ViewContext(Activity activity, Object fragment) {
        this.activity = activity;
        this.fragment = fragment;
    }

    public ViewContext(View view) {
        this.view = view;
    }

    public ViewContext(Activity activity, Object fragment, View view) {
        this.activity = activity;
        this.fragment = fragment;
        this.view = view;
    }
}
