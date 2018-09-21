package com.sensorsdata.analytics.android.sdk;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class PersistentAppStartTime extends PersistentIdentity<Long> {
    PersistentAppStartTime(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbAdapter.APP_START_TIME, new PersistentSerializer<Long>() {
            @Override
            public Long load(String value) {
                return Long.valueOf(value);
            }

            @Override
            public String save(Long item) {
                return String.valueOf(item);
            }

            @Override
            public Long create() {
                return Long.valueOf(0);
            }
        });
    }
}
