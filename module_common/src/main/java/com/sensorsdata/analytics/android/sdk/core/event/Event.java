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

import org.json.JSONObject;

public abstract class Event {
    // _track_id
    private long mTrackId;
    // time
    private long mTime;
    // type
    private String mType;
    // lib
    private JSONObject mLib;
    // properties
    private JSONObject mProperties;
    // project
    private String mProject;
    // token
    private String mToken;
    // extend field, for h5...
    protected JSONObject mExtras;

    public abstract JSONObject toJSONObject();

    public long getTime() {
        return mTime == 0 ? System.currentTimeMillis() : mTime;
    }

    public void setTime(long time) {
        if (time > 0) {
            this.mTime = time;
        } else {
            this.mTime = System.currentTimeMillis();
        }
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public JSONObject getLib() {
        return mLib;
    }

    public void setLib(JSONObject mLib) {
        this.mLib = mLib;
    }

    public JSONObject getProperties() {
        return mProperties;
    }

    public void setProperties(JSONObject mProperties) {
        this.mProperties = mProperties;
    }

    public String getProject() {
        return mProject;
    }

    public void setProject(String mProject) {
        this.mProject = mProject;
    }

    public String getToken() {
        return mToken;
    }

    public void setToken(String mToken) {
        this.mToken = mToken;
    }

    public long getTrackId() {
        return mTrackId;
    }

    public void setTrackId(long mTrackId) {
        this.mTrackId = mTrackId;
    }

    public JSONObject getExtras() {
        return mExtras;
    }

    public void setExtras(JSONObject mExtras) {
        this.mExtras = mExtras;
    }
}
