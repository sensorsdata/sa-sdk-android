package com.sensorsdata.analytics.android.sdk;

import android.content.SharedPreferences;

import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Created by 王灼洲 on 2017/4/10
 */

class PersistentDistinctId extends PersistentIdentity<String> {
    PersistentDistinctId(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "events_distinct_id", new PersistentSerializer<String>() {
            @Override
            public String load(String value) {
                return value;
            }

            @Override
            public String save(String item) {
                return item;
            }

            @Override
            public String create() {
                return UUID.randomUUID().toString();
            }
        });
    }
}
