/*
 * Created by chenru on 2020/07/02.
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

package com.sensorsdata.analytics.android.sdk.deeplink;

import android.content.Intent;
import android.net.Uri;

public abstract class AbsDeepLink implements DeepLinkProcessor {
    protected DeepLinkManager.OnDeepLinkParseFinishCallback mCallBack;
    private String deepLinkUrl;

    AbsDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        Uri uri = intent.getData();
        setDeepLinkUrl(uri.toString());
    }

    @Override
    public void setDeepLinkUrl(String deepLinkUrl) {
        this.deepLinkUrl = deepLinkUrl;
    }

    @Override
    public String getDeepLinkUrl() {
        return deepLinkUrl;
    }

    @Override
    public void setDeepLinkParseFinishCallback(DeepLinkManager.OnDeepLinkParseFinishCallback callBack) {
        this.mCallBack = callBack;
    }
}
