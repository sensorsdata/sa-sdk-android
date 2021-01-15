/*
 * Created by zhangxiangwei on 2019/11/05.
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

public interface ThreadNameConstants {
    String THREAD_APP_END_DATA_SAVE_TIMER = "SA.AppEndDataSaveTimerThread";
    String THREAD_TASK_QUEUE = "SA.TaskQueueThread";
    String THREAD_SEND_DISTINCT_ID = "SA.SendDistinctIDThread";
    String THREAD_GET_SDK_REMOTE_CONFIG = "SA.GetSDKRemoteConfigThread";
    String THREAD_DEEP_LINK_REQUEST = "SA.DeepLinkRequest";
}
