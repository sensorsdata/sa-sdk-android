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

import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;

import org.json.JSONObject;

public class InputData {
    protected EventType mEventType;
    protected String mEventName;
    protected JSONObject mProperties;
    private String mItemType;
    private String mItemId;
    // event track time
    protected long mTime;
    // extend input_data, h5
    protected String mExtras;

    public EventType getEventType() {
        if (mEventType == null) {
            return EventType.TRACK;
        }
        return mEventType;
    }

    public InputData setEventType(EventType mEventType) {
        this.mEventType = mEventType;
        return this;
    }

    public String getEventName() {
        return mEventName;
    }

    public InputData setEventName(String mEventName) {
        this.mEventName = mEventName;
        return this;
    }

    public JSONObject getProperties() {
        return mProperties;
    }

    public InputData setProperties(JSONObject mProperties) {
        this.mProperties = mProperties;
        return this;
    }

    public String getExtras() {
        return mExtras;
    }

    public InputData setExtras(String mExtras) {
        this.mExtras = mExtras;
        return this;
    }

    public String getItemType() {
        return mItemType;
    }

    public InputData setItemType(String mItemType) {
        this.mItemType = mItemType;
        return this;
    }

    public String getItemId() {
        return mItemId;
    }

    public InputData setItemId(String mItemId) {
        this.mItemId = mItemId;
        return this;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long mTime) {
        this.mTime = mTime;
    }
}
