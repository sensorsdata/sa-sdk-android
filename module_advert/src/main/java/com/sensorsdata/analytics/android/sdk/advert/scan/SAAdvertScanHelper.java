/*
 * Created by chenru on 2022/7/5 下午6:08.
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

package com.sensorsdata.analytics.android.sdk.advert.scan;

import android.app.Activity;
import android.net.Uri;

public class SAAdvertScanHelper {
    public static boolean scanHandler(Activity activity, Uri uri) {
        String host = uri.getHost();
        IAdvertScanListener scan = null;
        if ("channeldebug".equals(host)) {
            scan = new ChannelDebugScanHelper();
        }else if (("adsScanDeviceInfo").equals(host)) {
            scan = new WhiteListScanHelper();
        }
        if (scan != null) {
            scan.handlerScanUri(activity, uri);
            return true;
        }
        return false;
    }
}
