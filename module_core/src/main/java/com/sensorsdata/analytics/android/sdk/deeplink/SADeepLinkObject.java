/*
 * Created by chenru on 2022/4/24 下午3:54(format year/.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.deeplink;

import java.io.Serializable;

public class SADeepLinkObject implements Serializable {
    /**
     * 链接设置的 App 内参数
     */
    private String mParams;
    /**
     * 链接设置的 归因数据
     */
    private String mChannels;
    /**
     * 是否请求成功
     */
    private boolean success;
    /**
     * 请求时长
     */
    private long mAppAwakePassedTime;

    public SADeepLinkObject(String params, String channels, boolean success, long appAwakePassedTime) {
        this.mParams = params;
        this.mChannels = channels;
        this.success = success;
        this.mAppAwakePassedTime = appAwakePassedTime;
    }

    public String getParams() {
        return mParams;
    }

    public String getChannels() {
        return mChannels;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getAppAwakePassedTime() {
        return mAppAwakePassedTime;
    }
}
