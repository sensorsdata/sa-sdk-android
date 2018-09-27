/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class PersistentAppStart extends PersistentIdentity<Boolean> {
    PersistentAppStart(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbAdapter.APP_STARTED, new PersistentSerializer<Boolean>() {
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
