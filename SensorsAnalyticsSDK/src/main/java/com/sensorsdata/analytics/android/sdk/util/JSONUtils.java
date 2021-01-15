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

package com.sensorsdata.analytics.android.sdk.util;

import com.sensorsdata.analytics.android.sdk.SALog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class JSONUtils {

    public static String optionalStringKey(JSONObject o, String k) throws JSONException {
        if (o.has(k) && !o.isNull(k)) {
            return o.getString(k);
        }
        return null;
    }

    private static void addIndentBlank(StringBuilder sb, int indent) {
        try {
            for (int i = 0; i < indent; i++) {
                sb.append('\t');
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    public static String formatJson(String jsonStr) {
        try {
            if (null == jsonStr || "".equals(jsonStr)) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            char last = '\0';
            char current = '\0';
            int indent = 0;
            boolean isInQuotationMarks = false;
            for (int i = 0; i < jsonStr.length(); i++) {
                last = current;
                current = jsonStr.charAt(i);
                switch (current) {
                    case '"':
                        if (last != '\\') {
                            isInQuotationMarks = !isInQuotationMarks;
                        }
                        sb.append(current);
                        break;
                    case '{':
                    case '[':
                        sb.append(current);
                        if (!isInQuotationMarks) {
                            sb.append('\n');
                            indent++;
                            addIndentBlank(sb, indent);
                        }
                        break;
                    case '}':
                    case ']':
                        if (!isInQuotationMarks) {
                            sb.append('\n');
                            indent--;
                            addIndentBlank(sb, indent);
                        }
                        sb.append(current);
                        break;
                    case ',':
                        sb.append(current);
                        if (last != '\\' && !isInQuotationMarks) {
                            sb.append('\n');
                            addIndentBlank(sb, indent);
                        }
                        break;
                    case '\\':
                        break;
                    default:
                        sb.append(current);
                }
            }

            return sb.toString();
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            return "";
        }
    }

    public static Map<String, String> json2Map(JSONObject json) {
        if (json != null && json.length() > 0) {
            Map<String, String> maps = new HashMap<>();
            Iterator<String> iterator = json.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                maps.put(key, json.optString(key));
            }
            return maps;
        }
        return null;
    }

    /**
     * 创建新的 JSONObject 对象,防止 SDK  内部修改对传入的 property 做出改变
     * @param properties JSONObject
     * @return 新的 JSONObject
     */
    public static JSONObject makeNewObject(JSONObject properties) {
        JSONObject _properties;
        try {
            if (properties != null) {
                _properties = new JSONObject(properties.toString());
            } else {
                _properties = new JSONObject();
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
            _properties = new JSONObject();
        }
        return _properties;
    }

    /**
     * 合并非重复的属性
     * @param source 源
     * @param dest 目标
     */
    public static void mergeDistinctProperty(final JSONObject source, JSONObject dest) {
        try {
            Iterator<String> superPropertiesIterator = source.keys();
            while (superPropertiesIterator.hasNext()) {
                String key = superPropertiesIterator.next();
                if (dest.has(key)) {
                    continue;
                }
                Object value = source.get(key);
                if (value instanceof Date && !"$time".equals(key)) {
                    dest.put(key, TimeUtils.formatDate((Date) value, Locale.CHINA));
                } else {
                    dest.put(key, value);
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }
}
