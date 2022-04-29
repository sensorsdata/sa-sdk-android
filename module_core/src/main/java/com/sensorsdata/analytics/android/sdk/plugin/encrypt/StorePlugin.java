/*
 * Created by yuejianzhong on 2021/12/14.
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

package com.sensorsdata.analytics.android.sdk.plugin.encrypt;

public interface StorePlugin {

    void upgrade(StorePlugin oldPlugin);

    void setString(String key, String value);

    void setBool(String key, boolean value);

    void setInteger(String key, int value);

    void setFloat(String key, float value);

    void setLong(String key, long value);

    String getString(String key);

    Boolean getBool(String key);

    Integer getInteger(String key);

    Float getFloat(String key);

    Long getLong(String key);

    void remove(String key);

    boolean isExists(String key);

    String type();
}
