/*
 * Created by chenru on 2020/01/17.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class SADataHelper {

    private static final String TAG = "SA.SADataHelper";

    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^((?!^distinct_id$|^original_id$|^time$|^properties$|^id$|^first_id$|^second_id$|^users$|^events$|^event$|^user_id$|^date$|^datetime$|^user_tag.*|^user_group.*)[a-zA-Z_$][a-zA-Z\\d_$]*)$",
            Pattern.CASE_INSENSITIVE);
    public static final int MAX_LENGTH_1024 = 1024;
    private static final int MAX_LENGTH_100 = 100;

    public static void assertPropertyTypes(JSONObject properties) throws InvalidDataException {
        if (properties == null) {
            return;
        }

        for (Iterator<String> iterator = properties.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            try {
                // Check Keys
                if (!assertPropertyKey(key)){
                    iterator.remove();
                    continue;
                }
                Object value = properties.get(key);

                if (value == JSONObject.NULL) {
                    SALog.i(TAG, "Property value is empty or null");
                    iterator.remove();
                    continue;
                }

                if (value instanceof List<?>) {
                    List<?> list = (List<?>) value;
                    int size = list.size();
                    JSONArray array = new JSONArray();
                    for (int i = 0; i < size; i++) {
                        array.put(list.get(i));
                    }
                    value = array;
                    properties.put(key, value);
                }

                if (!(value instanceof CharSequence || value instanceof Number || value instanceof JSONArray ||
                        value instanceof Boolean || value instanceof Date)) {
                    throw new InvalidDataException("The property value must be an instance of "
                            + "CharSequence/Number/Boolean/JSONArray/Date/List<String>. [key='" + key
                            + "', value='" + value.toString()
                            + "', class='" + value.getClass().getCanonicalName()
                            + "']");
                }

                if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    int size = array.length();
                    for (int i = 0; i < size; i++) {
                        if (!(array.get(i) instanceof CharSequence)) {
                            throw new InvalidDataException("The array property value must be an instance of "
                                    + "List<String> or JSONArray only contains String. [key='" + key
                                    + "', value='" + value.toString()
                                    + "']");
                        }
                    }
                    continue;
                }

                if (value instanceof String && ((String) value).length() > MAX_LENGTH_1024) {
                    SALog.i(TAG, value + " length is longer than " + MAX_LENGTH_1024);
                }
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property key. [key='" + key + "']");
            } catch (Error e) {
                SALog.i(TAG, e);
            }
        }
    }

    public static void assertEventName(String key) {
        if (TextUtils.isEmpty(key)) {
            SALog.i(TAG,"EventName is empty or null");
            return;
        }
        int length = key.length();
        if (length > MAX_LENGTH_100) {
            SALog.i(TAG, key + "'s length is longer than " + MAX_LENGTH_100);
            return;
        }
        if (!(KEY_PATTERN.matcher(key).matches())) {
            SALog.i(TAG, key + " is invalid");
        }
    }

    /**
     * 校验属性 key、item_type
     * @param key key、item_type
     * @return true 为不删除该属性，false 需要移除属性
     */
    public static boolean assertPropertyKey(String key) {
        if (TextUtils.isEmpty(key)) {
            SALog.i(TAG, "Property key is empty or null");
            return false;
        }

        if (!(KEY_PATTERN.matcher(key).matches())) {
            SALog.i(TAG, key + " is invalid");
            return false;
        }
        int length = key.length();
        if (length > MAX_LENGTH_100) {
            SALog.i(TAG, key + "'s length is longer than " + MAX_LENGTH_100);
        }
        return true;
    }

    /**
     * 校验 item_id
     * @param key key、item_type
     */
    public static void assertItemId(String key) {
        if (null == key) {
            SALog.i(TAG, "ItemId is empty or null");
            return;
        }
        int length = key.length();
        if (length > MAX_LENGTH_1024) {
            SALog.i(TAG, key + "'s length is longer than " + MAX_LENGTH_1024);
        }
    }

    public static void assertDistinctId(String value) throws InvalidDataException {
        if (TextUtils.isEmpty(value)) {
            throw new InvalidDataException("Id is empty or null");
        }
        if (value.length() > MAX_LENGTH_1024) {
            SALog.i(TAG, value + "'s length is longer than " + MAX_LENGTH_1024);
        }
    }

    /**
     * 校验属性
     * @param property 属性
     * @return String
     */
    public static String assertPropertyValue(String property) {
        if (property == null) {
            SALog.i(TAG, "Property value is empty or null");
            return property;
        }

        if (property.length() > MAX_LENGTH_1024) {
            SALog.i(TAG, property + "'s length is longer than " + MAX_LENGTH_1024);
        }
        return property;
    }

    public static JSONObject appendLibMethodAutoTrack(JSONObject jsonObject) {
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        try {
            jsonObject.put("$lib_method", "autoTrack");
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return jsonObject;
    }

    public static void addTimeProperty(JSONObject jsonObject) {
        if (!jsonObject.has("$time")) {
            try {
                jsonObject.put("$time", new Date(System.currentTimeMillis()));
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }
    }
    public static void addCarrier(Context context, JSONObject property) {
        try {
            if (TextUtils.isEmpty(property.optString("$carrier"))) {
                String carrier = SensorsDataUtils.getOperator(context);
                if (!TextUtils.isEmpty(carrier)) {
                    property.put("$carrier", carrier);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
