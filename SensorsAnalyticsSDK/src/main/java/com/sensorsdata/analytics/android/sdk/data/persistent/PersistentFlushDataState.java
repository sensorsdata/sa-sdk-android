/*
 * Created by dengshiwei on 2020/07/01.
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

package com.sensorsdata.analytics.android.sdk.data.persistent;

import android.content.SharedPreferences;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

import java.util.concurrent.Future;

public class PersistentFlushDataState extends PersistentIdentity<Boolean> {

    public PersistentFlushDataState(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbParams.TABLE_SUB_PROCESS_FLUSH_DATA, new PersistentSerializer<Boolean>() {
            @Override
            public Boolean load(String value) {
                return "true".equals(value);
            }

            @Override
            public String save(Boolean item) {
                return item == null ? create().toString() : String.valueOf(item);
            }

            @Override
            public Boolean create() {
                return true;
            }
        });
    }
}
