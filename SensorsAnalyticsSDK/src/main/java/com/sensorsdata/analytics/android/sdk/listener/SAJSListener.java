/*
 * Created by zhangxiangwei on 2020/10/15.
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

package com.sensorsdata.analytics.android.sdk.listener;

import android.view.View;

import java.lang.ref.WeakReference;

public interface SAJSListener {
    /**
     * 打通场景下监听 JS 的消息
     *
     * @param view WebView 对象
     * @param message JS 发送的消息
     */
    void onReceiveJSMessage(WeakReference<View> view, String message);
}
