/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.data.persistent;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

/**
 * Created by 王灼洲 on 2017/4/10
 */

public class PersistentFirstStart extends PersistentIdentity<Boolean> {
    public PersistentFirstStart(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "first_start", new PersistentSerializer<Boolean>() {
            @Override
            public Boolean load(String value) {
                return false;
            }

            @Override
            public String save(Boolean item) {
                return String.valueOf(true);
            }

            @Override
            public Boolean create() {
                return true;
            }
        });
    }
}
