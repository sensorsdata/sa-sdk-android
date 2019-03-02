/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.data.persistent;

import android.content.SharedPreferences;

import com.sensorsdata.analytics.android.sdk.data.DbParams;

import java.util.concurrent.Future;

/**
 * Created by 王灼洲 on 2017/4/10
 */

public class PersistentSessionIntervalTime extends PersistentIdentity<Integer> {
    public PersistentSessionIntervalTime(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbParams.TABLE_SESSIONINTERVALTIME, new PersistentSerializer<Integer>() {
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
