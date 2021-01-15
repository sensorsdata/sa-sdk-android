/*
 * Created by zhangwei on 2019/04/25.
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

import androidx.test.espresso.core.internal.deps.guava.collect.Lists;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PropertyBuilderTest {
    /**
     * 测试 newInstance 返回数据不为空
     */
    @Test
    public void newInstance_notNull() {
        PropertyBuilder builder = PropertyBuilder.newInstance();
        assertNotNull(builder);
    }

    /**
     * 测试 append(String key, Object value) 中代码逻辑
     */
    @Test
    public void append_stringKey_objectValue() {
        PropertyBuilder builder = PropertyBuilder.newInstance();
        builder.append("foo", null).append(null, "bar")
                .append("key1", 2).append("key2", "value2");
        JSONObject jsonObject = builder.toJSONObject();
        assertNotNull(jsonObject);
        assertEquals(3, jsonObject.length());
        assertThat(Lists.newArrayList(jsonObject.keys()), allOf(hasItem("foo"), hasItem("key1"), hasItem("key2")));
    }

    @Test
    public void append_map() {
        PropertyBuilder builder = PropertyBuilder.newInstance();
        Map<String, Object> userPropertyMap = new HashMap<>();
        userPropertyMap.put("foo", null);
        userPropertyMap.put(null, "bar");
        userPropertyMap.put(null, null);
        userPropertyMap.put("key1", 2);
        userPropertyMap.put("key2", "value2");
        builder.append(userPropertyMap);
        JSONObject jsonObject = builder.toJSONObject();
        assertNotNull(jsonObject);
        assertEquals(3, jsonObject.length());
        assertThat(Lists.newArrayList(jsonObject.keys()), allOf(hasItem("foo"), hasItem("key1"), hasItem("key2")));
    }

    @Test
    public void append_varargs() {
        PropertyBuilder builder = PropertyBuilder.newInstance();
        builder.append(
                "key1", 11, //valid
                new Object(), 22,
                null, 22,
                222, 22,
                "key2", 22,//valid
                new Object(),
                Arrays.asList(1, 2, 3, 4), Arrays.asList(1, 2, "3", 4));
        JSONObject jsonObject = builder.toJSONObject();
        assertNotNull(jsonObject);
        assertEquals(2, jsonObject.length());
    }

    @Test
    public void toJSONObject() {
        PropertyBuilder builder = PropertyBuilder.newInstance();
        builder.append(null, 11);
        builder.append("foo", "bar");
        JSONObject jsonObject = builder.toJSONObject();
        assertNotNull(jsonObject);
        assertEquals(1, jsonObject.length());
        assertTrue(jsonObject.has("foo"));
        assertThat(Lists.newArrayList(jsonObject.keys()), hasItem("foo"));
    }
}
