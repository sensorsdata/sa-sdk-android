/*
 * Created by dengshiwei on 2022/08/02.
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

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.sensorsdata.analytics.android.sdk.dialog.SchemeActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.robolectric.Robolectric;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class MockDataTest {
    public static final String N_TITLE = "n_title";
    public static final String N_CONTENT = "n_content";
    public static final String N_EXTRAS = "n_extras";
    public static final String N_ROM_TYPE = "rom_type";
    public static Intent mockJPushIntent() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(N_TITLE,"mock_title");
            jsonObject.put(N_CONTENT,"mock_content");
            jsonObject.put(N_EXTRAS,"mock_extras");
            jsonObject.put(N_ROM_TYPE,1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("JMessageExtra",jsonObject.toString());
        intent.putExtras(bundle);
        return intent;
    }

    public static PendingIntent mockPendingIntent() {
        final SchemeActivity activity = Robolectric.setupActivity(SchemeActivity.class);
        return PendingIntent.getActivity(activity, 100, activity.getIntent(), PendingIntent.FLAG_NO_CREATE);
    }

}