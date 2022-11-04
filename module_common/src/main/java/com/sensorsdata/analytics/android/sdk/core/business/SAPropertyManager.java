/*
 * Created by dengshiwei on 2022/10/19.
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

package com.sensorsdata.analytics.android.sdk.core.business;

import java.util.HashMap;
import java.util.Map;

public class SAPropertyManager {
    private Map<String, String> mLimitKeys;

    public static SAPropertyManager getInstance() {
        return Holder.INSTANCE;
    }

    static class Holder {
        public static SAPropertyManager INSTANCE = new SAPropertyManager();
    }

    private SAPropertyManager() {
        this.mLimitKeys = new HashMap<>();
    }

    public void registerLimitKeys(Map<String, String> limitKeys) {
        if (limitKeys != null) {
            mLimitKeys.putAll(limitKeys);
        }
    }

    public boolean isLimitKey(String key) {
        return mLimitKeys != null && mLimitKeys.containsKey(key);
    }

    public String getLimitValue(String key) {
        if (mLimitKeys == null) {
            return null;
        }
        return mLimitKeys.get(key);
    }
}
