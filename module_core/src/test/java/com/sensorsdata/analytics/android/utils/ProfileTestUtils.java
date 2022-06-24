/*
 * Created by dengshiwei on 2022/06/08.
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

package com.sensorsdata.analytics.android.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfileTestUtils {
    /* check profile common property */
    public static void checkProfileEvent(JSONObject jsonObject, String profileType){
        assertNull(jsonObject.opt("event"));
        assertEquals(jsonObject.opt("type"), profileType);
        assertEquals(jsonObject.opt("distinct_id"), jsonObject.opt("anonymous_id"));
        JSONObject identityJson = jsonObject.optJSONObject("identities");
        assertNotNull(identityJson);
        if (identityJson.has("$identity_anonymous_id")) {
            assertEquals(identityJson.opt("$identity_anonymous_id"), jsonObject.opt("distinct_id"));
        } else if (identityJson.has("$identity_login_id"))  {
            assertEquals(identityJson.opt("$identity_login_id"), jsonObject.opt("distinct_id"));
        } else {
            assertEquals(identityJson.opt("$identity_android_uuid"), jsonObject.opt("distinct_id"));
        }
    }

    /* check item common property */
    public static void checkItemEvent(JSONObject jsonObject, String type, String itemType, String itemId) {
        assertNull(jsonObject.opt("event"));
        assertEquals(jsonObject.opt("type"), type);
        assertEquals(jsonObject.opt("item_type"), itemType);
        assertEquals(jsonObject.opt("item_id"), itemId);
    }
}
