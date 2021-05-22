/*
 * Created by wangzhuozhou on 2015/08/01.
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

import org.json.JSONException;
import org.json.JSONObject;

public class SensorsDataGPSLocation {
    /**
     * 纬度
     */
    private long latitude;

    /**
     * 经度
     */
    private long longitude;

    /**
     * 坐标系
     */
    private String coordinate;

    public long getLatitude() {
        return latitude;
    }

    public void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    public long getLongitude() {
        return longitude;
    }

    public void setLongitude(long longitude) {
        this.longitude = longitude;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public void toJSON(JSONObject jsonObject) {
        try {
            jsonObject.put("$latitude", latitude);
            jsonObject.put("$longitude", longitude);
            jsonObject.put("$geo_coordinate_system", coordinate);
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 坐标系
     */
    public final class CoordinateType {
        /**
         * 地球坐标系
         */
        public static final String WGS84 = "WGS84";
        /**
         * 火星坐标系
         */
        public static final String GCJ02 = "GCJ02";
        /**
         * 百度坐标系
         */
        public static final String BD09 = "BD09";
    }
}
