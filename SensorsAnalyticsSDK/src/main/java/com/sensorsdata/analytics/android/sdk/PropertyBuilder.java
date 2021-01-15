/*
 * Created by zhangwei on 2019/04/19.
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

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;


public final class PropertyBuilder {
    private static final String TAG = "PropertyBuilder";
    private final LinkedHashMap<String, Object> innerPropertyMap;

    private PropertyBuilder() {
        innerPropertyMap = new LinkedHashMap<>();
    }

    public static PropertyBuilder newInstance() {
        return new PropertyBuilder();
    }

    /**
     * 添加 key - value 对
     *
     * @param key key
     * @param value value
     * @return PropertyBuilder
     */
    public PropertyBuilder append(String key, Object value) {
        innerPropertyMap.put(key, value);
        return this;
    }

    /**
     * 添加 Map 集合
     *
     * @param propertyMap propertyMap
     * @return PropertyBuilder
     */
    public PropertyBuilder append(Map<String, Object> propertyMap) {
        if (propertyMap != null && !propertyMap.isEmpty()) {
            innerPropertyMap.putAll(propertyMap);
        }
        return this;
    }

    /**
     * 添加键值对，可变参数中，奇数位置对应的是 Key，偶数位置对应的是 Value，如果参数长度不是偶数，那么就忽略最后
     * 一位，保持在偶数长度配对，如果存在将覆盖；如果奇数位 Key 不是 String 类型，那么就忽略对应位置的 Value。
     *
     * @param keyValuePairs 键值对，奇数为 String 类型 key，偶数为 Object 类型 value
     * @return PropertyBuilder
     */
    public PropertyBuilder append(Object... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length <= 1) {
            SALog.i(TAG, "The key value pair is incorrect.");
            return this;
        }
        for (int index = 0; index < keyValuePairs.length; index++) {
            Object keyObj = keyValuePairs[index];
            index = index + 1;
            if (index >= keyValuePairs.length) {
                SALog.i(TAG, "this element key[index= " + index + "] will be ignored," +
                        " because no element can pair with it. ");
                return this;
            }
            Object valueObj = keyValuePairs[index];
            if (keyObj instanceof String) {
                innerPropertyMap.put((String) keyObj, valueObj);
            } else {
                SALog.i(TAG, "this element key[index= " + index + "] is not a String," +
                        " the method will ignore the element and the next element. ");
            }
        }
        return this;
    }

    /**
     * 获取 JSONObject 对象
     *
     * @return JSONObject
     */
    public JSONObject toJSONObject() {
        innerPropertyMap.remove(null);
        if (innerPropertyMap.isEmpty()) {
            return null;
        }

        JSONObject jsonObject = new JSONObject();
        for (String key : innerPropertyMap.keySet()) {
            try {
                jsonObject.put(key, innerPropertyMap.get(key));
            } catch (Exception ex) {
                SALog.printStackTrace(ex);
            }
        }
        return jsonObject;
    }

    /**
     * 获取属性个数
     *
     * @return size
     */
    public int size() {
        return innerPropertyMap.size();
    }

    /**
     * 删除指定属性
     *
     * @param key key
     * @return 删除成功返回 key 对应的 value，否则返回 null (假如 key 对应的 value 是 null，那么返回的值也是 null)
     */
    public Object remove(String key) {
        return innerPropertyMap.remove(key);
    }

    /**
     * 删除所有的 property
     */
    public void clear() {
        innerPropertyMap.clear();
    }
}
