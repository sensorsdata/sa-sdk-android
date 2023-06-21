/*
 * Created by dengshiwei on 2022/06/14.
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

package com.sensorsdata.analytics.android.sdk.core.event;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import org.json.JSONObject;

public class TrackEvent extends Event {
    // distinct_id
    private String mDistinctId;
    // login_id
    private String mLoginId;
    // anonymous_id
    private String mAnonymousId;
    private String mOriginalId;
    // identities
    private JSONObject mIdentities;
    private String mEventName;
    private String mItemType;
    private String mItemId;

    @Override
    public JSONObject toJSONObject() {
        try {
            if (getExtras() != null) {// H5 track
                return getExtras();
            }
            JSONObject jsonObject = new JSONObject();
            if (getTrackId() != 0) {
                jsonObject.put("_track_id", getTrackId());
            }
            jsonObject.put("time", getTime());
            jsonObject.put("type", getType());
            jsonObject.put("distinct_id", getDistinctId());
            jsonObject.put("anonymous_id", getAnonymousId());
            if (getIdentities() != null && getIdentities().length() > 0) {
                jsonObject.put("identities", getIdentities());
            }
            if (!TextUtils.isEmpty(getProject())) {
                jsonObject.put("project", getProject());
            }
            if (!TextUtils.isEmpty(getToken())) {
                jsonObject.put("token", getToken());
            }
            if (!TextUtils.isEmpty(getEventName())) {
                jsonObject.put("event", getEventName());
            }
            if (!TextUtils.isEmpty(getOriginalId())) {
                jsonObject.put("original_id", getOriginalId());
            }
            if (!TextUtils.isEmpty(getLoginId())) {
                jsonObject.put("login_id", getLoginId());
            }
            if (!TextUtils.isEmpty(getItemType())) {
                jsonObject.put("item_type", getItemType());
            }
            if (!TextUtils.isEmpty(getItemId())) {
                jsonObject.put("item_id", getItemId());
            }

            jsonObject.put("lib", getLib());
            jsonObject.put("properties", getProperties());
            return jsonObject;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    public String getDistinctId() {
        return mDistinctId;
    }

    public void setDistinctId(String distinctId) {
        this.mDistinctId = distinctId;
    }

    public String getLoginId() {
        return mLoginId;
    }

    public void setLoginId(String loginId) {
        this.mLoginId = loginId;
    }

    public String getAnonymousId() {
        return mAnonymousId;
    }

    public void setAnonymousId(String anonymousId) {
        this.mAnonymousId = anonymousId;
    }

    public JSONObject getIdentities() {
        return mIdentities;
    }

    public void setIdentities(JSONObject identities) {
        this.mIdentities = identities;
    }

    public String getEventName() {
        return mEventName;
    }

    public void setEventName(String eventName) {
        this.mEventName = eventName;
    }

    public String getOriginalId() {
        return mOriginalId;
    }

    public void setOriginalId(String mOriginalId) {
        this.mOriginalId = mOriginalId;
    }

    public String getItemType() {
        return mItemType;
    }

    public void setItemType(String mItemType) {
        this.mItemType = mItemType;
    }

    public String getItemId() {
        return mItemId;
    }

    public void setItemId(String mItemId) {
        this.mItemId = mItemId;
    }
}
