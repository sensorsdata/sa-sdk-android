/*
 * Created by wangzhuozhou on 2019/02/01.
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

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

import java.util.concurrent.Future;

public class PersistentLoader {

    private static volatile PersistentLoader instance;
    private static Context context;
    private static Future<SharedPreferences> storedPreferences;

    private PersistentLoader(Context context) {
        PersistentLoader.context = context.getApplicationContext();
        final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
        final String prefsName = "com.sensorsdata.analytics.android.sdk.SensorsDataAPI";
        storedPreferences = sPrefsLoader.loadPreferences(context, prefsName);
    }

    public static PersistentLoader initLoader(Context context) {
        if (instance == null) {
            instance = new PersistentLoader(context);
        }
        return instance;
    }

    public static PersistentIdentity loadPersistent(String persistentKey) {
        if (instance == null) {
            throw new RuntimeException("you should call 'PersistentLoader.initLoader(Context)' first");
        }
        if (TextUtils.isEmpty(persistentKey)) {
            return null;
        }
        switch (persistentKey) {
            case PersistentName.APP_END_DATA:
                return new PersistentAppEndData(storedPreferences);
            case PersistentName.DISTINCT_ID:
                return new PersistentDistinctId(storedPreferences, context);
            case PersistentName.FIRST_DAY:
                return new PersistentFirstDay(storedPreferences);
            case PersistentName.FIRST_INSTALL:
                return new PersistentFirstTrackInstallation(storedPreferences);
            case PersistentName.FIRST_INSTALL_CALLBACK:
                return new PersistentFirstTrackInstallationWithCallback(storedPreferences);
            case PersistentName.FIRST_START:
                return new PersistentFirstStart(storedPreferences);
            case PersistentName.LOGIN_ID:
                return new PersistentLoginId(storedPreferences);
            case PersistentName.REMOTE_CONFIG:
                return new PersistentRemoteSDKConfig(storedPreferences);
            case PersistentName.SUPER_PROPERTIES:
                return new PersistentSuperProperties(storedPreferences);
            case PersistentName.SUB_PROCESS_FLUSH_DATA:
                return new PersistentFlushDataState(storedPreferences);
            case PersistentName.VISUAL_PROPERTIES:
                return new PersistentVisualConfig(storedPreferences);
            default:
                return null;
        }
    }

    public interface PersistentName {
        String APP_END_DATA = DbParams.TABLE_APP_END_DATA;
        String SUB_PROCESS_FLUSH_DATA = DbParams.TABLE_SUB_PROCESS_FLUSH_DATA;
        String DISTINCT_ID = "events_distinct_id";
        String FIRST_DAY = "first_day";
        String FIRST_START = "first_start";
        String FIRST_INSTALL = "first_track_installation";
        String FIRST_INSTALL_CALLBACK = "first_track_installation_with_callback";
        String LOGIN_ID = "events_login_id";
        String REMOTE_CONFIG = "sensorsdata_sdk_configuration";
        String SUPER_PROPERTIES = "super_properties";
        String VISUAL_PROPERTIES = "visual_properties";
    }
}
