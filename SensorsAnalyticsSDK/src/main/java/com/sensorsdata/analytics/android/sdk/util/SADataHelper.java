/*
 * Created by chenru on 2020/01/17.
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

package com.sensorsdata.analytics.android.sdk.util;

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
            "^((?!^distinct_id$|^original_id$|^time$|^properties$|^id$|^first_id$|^second_id$|^users$|^events$|^event$|^user_id$|^date$|^datetime$)[a-zA-Z_$][a-zA-Z\\d_$]{0,99})$",
            Pattern.CASE_INSENSITIVE);

    public static void assertPropertyTypes(JSONObject properties) throws InvalidDataException {
        if (properties == null) {
            return;
        }

        for (Iterator<String> iterator = properties.keys(); iterator.hasNext(); ) {
            String key = iterator.next();

            // Check Keys
            assertKey(key);

            try {
                Object value = properties.get(key);

                if (value == JSONObject.NULL) {
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

                if ("app_crashed_reason".equals(key)) {
                    if (value instanceof String && ((String) value).length() > 8191 * 2) {
                        properties.put(key, ((String) value).substring(0, 8191 * 2) + "$");
                        SALog.d(TAG, "The property value is too long. [key='" + key
                                + "', value='" + value.toString() + "']");
                    }
                } else {
                    if (value instanceof String && ((String) value).length() > 8191) {
                        properties.put(key, ((String) value).substring(0, 8191) + "$");
                        SALog.d(TAG, "The property value is too long. [key='" + key
                                + "', value='" + value.toString() + "']");
                    }
                }
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property key. [key='" + key + "']");
            }
        }
    }

    public static void assertKey(String key) throws InvalidDataException {
        if (null == key || key.length() < 1) {
            throw new InvalidDataException("The key is empty.");
        }
        if (!(KEY_PATTERN.matcher(key).matches())) {
            throw new InvalidDataException("The key '" + key + "' is invalid.");
        }
    }

    public static void assertValue(String value) throws InvalidDataException {
        if (TextUtils.isEmpty(value)) {
            throw new InvalidDataException("The value is empty.");
        }
        if (value.length() > 255) {
            throw new InvalidDataException("The " + value + " is too long, max length is 255.");
        }
    }

    public static String assertPropertyLength(String property) {
        if (property != null && property.length() > 8191) {
            property = property.substring(0, 8191) + "$";
            SALog.d(TAG, "The property value is too long. property=" + property);
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
}
