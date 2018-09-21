package com.sensorsdata.analytics.android.sdk;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class PersistentAppEndEventState extends PersistentIdentity<Boolean> {
    PersistentAppEndEventState(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbAdapter.APP_END_STATE, new PersistentSerializer<Boolean>() {
            @Override
            public Boolean load(String value) {
                return Boolean.valueOf(value);
            }

            @Override
            public String save(Boolean item) {
                return String.valueOf(item);
            }

            @Override
            public Boolean create() {
                return true;
            }
        });
    }
}
