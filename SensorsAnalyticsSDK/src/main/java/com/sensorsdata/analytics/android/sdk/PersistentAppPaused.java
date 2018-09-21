package com.sensorsdata.analytics.android.sdk;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class PersistentAppPaused extends PersistentIdentity<Long> {
    PersistentAppPaused(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "app_paused", new PersistentSerializer<Long>() {
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
