/*
 * Created by wangzhuozhou on 2019/02/01.
 * Copyright 2015－2019 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppEndData;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppEndEventState;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppPaused;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppStart;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppStartTime;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentDistinctId;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstTrackInstallation;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstTrackInstallationWithCallback;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentIdentity;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoginId;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentRemoteSDKConfig;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSessionIntervalTime;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSuperProperties;

import java.util.concurrent.Future;

public class PersistentLoader {

    private static volatile PersistentLoader instance;
    private static Context context;
    private static Future<SharedPreferences> storedPreferences;

    private PersistentLoader(Context context) {
        this.context = context;
        final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
        final String prefsName = "com.sensorsdata.analytics.android.sdk.SensorsDataAPI";
        final SharedPreferencesLoader.OnPrefsLoadedListener listener =
                        new SharedPreferencesLoader.OnPrefsLoadedListener() {
                            @Override
                            public void onPrefsLoaded(SharedPreferences preferences) {
                            }
                        };
        storedPreferences =
                sPrefsLoader.loadPreferences(context, prefsName, listener);
    }

    public static PersistentLoader initLoader(Context context) {
        if (instance == null) {
            instance = new PersistentLoader(context);
        }
        return instance;
    }

    public static PersistentIdentity loadPersistent(String persistentKey) {
        switch (persistentKey) {
            case PersistentName.APP_END_DATA:
                return new PersistentAppEndData(storedPreferences);
            case PersistentName.APP_END_STATE:
                return new PersistentAppEndEventState(storedPreferences);
            case PersistentName.APP_PAUSED_TIME:
                return new PersistentAppPaused(storedPreferences);
            case PersistentName.APP_SESSION_TIME:
                return new PersistentSessionIntervalTime(storedPreferences);
            case PersistentName.APP_START_STATE:
                return new PersistentAppStart(storedPreferences);
            case PersistentName.APP_START_TIME:
                return new PersistentAppStartTime(storedPreferences);
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
        }
        return null;
    }

    public static class PersistentName {
        static final String APP_END_DATA = DbParams.TABLE_APPENDDATA;
        static final String APP_END_STATE = DbParams.TABLE_APPENDSTATE;
        static final String APP_PAUSED_TIME = DbParams.TABLE_APPPAUSEDTIME;
        static final String APP_START_STATE = DbParams.TABLE_APPSTARTED;
        static final String APP_START_TIME = DbParams.TABLE_APPSTARTTIME;
        static final String APP_SESSION_TIME = DbParams.TABLE_SESSIONINTERVALTIME;
        public static final String DISTINCT_ID = "events_distinct_id";
        public static final String FIRST_DAY = "first_day";
        public static final String FIRST_START = "first_start";
        public static final String FIRST_INSTALL = "first_track_installation";
        public static final String FIRST_INSTALL_CALLBACK = "first_track_installation_with_callback";
        public static final String LOGIN_ID = "events_login_id";
        public static final String REMOTE_CONFIG = "sensorsdata_sdk_configuration";
        public static final String SUPER_PROPERTIES = "super_properties";
    }
}
