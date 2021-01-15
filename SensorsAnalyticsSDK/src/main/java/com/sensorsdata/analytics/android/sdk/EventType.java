/*
 * Created by wangzhuozhou on 2017/4/10.
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

enum EventType {
    TRACK("track", true, false),
    TRACK_SIGNUP("track_signup", true, false),
    PROFILE_SET("profile_set", false, true),
    PROFILE_SET_ONCE("profile_set_once", false, true),
    PROFILE_UNSET("profile_unset", false, true),
    PROFILE_INCREMENT("profile_increment", false, true),
    PROFILE_APPEND("profile_append", false, true),
    PROFILE_DELETE("profile_delete", false, true),
    REGISTER_SUPER_PROPERTIES("register_super_properties", false, false),
    ITEM_SET("item_set", false, false),
    ITEM_DELETE("item_delete", false, false);

    private String eventType;
    private boolean track;
    private boolean profile;

    EventType(String eventType, boolean isTrack, boolean isProfile) {
        this.eventType = eventType;
        this.track = isTrack;
        this.profile = isProfile;
    }

    public String getEventType() {
        return eventType;
    }

    public boolean isTrack() {
        return track;
    }

    public boolean isProfile() {
        return profile;
    }
}
