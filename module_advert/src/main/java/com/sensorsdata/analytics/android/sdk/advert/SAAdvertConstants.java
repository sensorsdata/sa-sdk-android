/*
 * Created by chenru on 2023/1/10 下午4:49.
 * Copyright 2015－2023 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.advert;

public interface SAAdvertConstants {
    String TAG = "SA.Advert";
    interface Properties {
         String DEEPLINK_OPTIONS = "$deeplink_options";
         String MATCH_FAIL_REASON = "$deeplink_match_fail_reason";
         String DEEPLINK_URL = "$deeplink_url";

         String SLINK_ID = "$ad_slink_id";
         String SLINK_TYPE = "$ad_slink_type";
         String DEVICE_INFO = "$ad_device_info";
         String MATCH_TYPE = "$ad_app_match_type";
         String CHANNEL_INFO = "$ad_deeplink_channel_info";

         String SLINK_CUSTOM_PARAMS = "$sat_slink_custom_params";
         String SLINK_TEMPLATE_ID = "$ad_slink_template_id";

         String DYNAMIC_SLINK_CHANNEL_NAME = "$ad_dynamic_slink_channel_name";
         String DYNAMIC_SLINK_CHANNEL_TYPE = "$ad_dynamic_slink_channel_type";
         String DYNAMIC_SLINK_SOURCE = "$ad_dynamic_slink_source";
         String DYNAMIC_SLINK_DATA = "$ad_dynamic_slink_data";
         String DYNAMIC_SLINK_SHORT_URL = "$ad_dynamic_slink_short_url";
         String DYNAMIC_SLINK_STATUS = "$ad_dynamic_slink_status";
         String DYNAMIC_SLINK_MSG = "$ad_dynamic_slink_msg";


    }
    interface EventName{
        String MATCH_RESULT = "$AppDeeplinkMatchedResult";
        String DEFERRED_DEEPLINK_JUMP = "$AdAppDeferredDeepLinkJump";
        String DYNAMIC_SLINK_CREATE = "$AdDynamicSlinkCreate";
        String DEEPLINK_LAUNCH = "$AppDeeplinkLaunch";
        String APP_INTERACT = "$AppInteract";
    }
}
