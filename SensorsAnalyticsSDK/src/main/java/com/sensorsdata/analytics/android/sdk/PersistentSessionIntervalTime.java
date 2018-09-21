package com.sensorsdata.analytics.android.sdk;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

/**
 * Created by 王灼洲 on 2017/4/10
 */

class PersistentSessionIntervalTime extends PersistentIdentity<Integer> {
    PersistentSessionIntervalTime(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbAdapter.SESSION_INTERVAL_TIME, new PersistentSerializer<Integer>() {
            @Override
            public Integer load(String value) {
                return Integer.valueOf(value);
            }

            @Override
            public String save(Integer item) {
                return item == null ? "" : item.toString();
            }

            @Override
            public Integer create() {
                return 30 * 1000;
            }
        });
    }
}
