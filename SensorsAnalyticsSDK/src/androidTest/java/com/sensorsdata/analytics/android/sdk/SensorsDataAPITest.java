/*
 * Created by zhangwei on 2019/04/25.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sensorsdata.analytics.android.sdk.internal.beans.EventTimer;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class SensorsDataAPITest {

    private static final String TAG = "SensorsDataAPITest";

    private static String userId;

    @BeforeClass
    public static void setUp() {
        userId = "123123123";
    }

    @Test
    public void getInstance() {
    }

    @Test
    public void setUserId() {
        Context context = ApplicationProvider.getApplicationContext();
        //Config.getInstance(context).setUserId(userId);
    }

    @Test
    public void getUserId() {
        Context context = ApplicationProvider.getApplicationContext();
        //String id = Config.getInstance(context).getUserId();
        // assertEquals(userId, id);
    }

    /**
     * 测试事件暂停功能操作流程  start（0）---->pause（10）----->resume（20）-------end（30）
     * 结果：20
     */
    @Test
    public void eventPause() {
        Context context = ApplicationProvider.getApplicationContext();
        SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));
        SensorsDataAPI.sharedInstance().trackTimerStart("event_pause");
        Field field;
        Map<String, EventTimer> map;
        try {
            Thread.sleep(1000);
            field = SensorsDataAPI.class.getDeclaredField("mTrackTimer");
            assertNotNull(field);
            field.setAccessible(true);
            map = (Map<String, EventTimer>) field.get(SensorsDataAPI.sharedInstance());
            assertNotNull(map);
            assertEquals(1, map.size());
            Thread.sleep(9000);
            SensorsDataAPI.sharedInstance().trackTimerPause("event_pause");
            Thread.sleep(10000);
            SensorsDataAPI.sharedInstance().trackTimerResume("event_pause");
            Thread.sleep(10000);
            EventTimer eventTimer = map.get("event_pause");
            eventTimer.duration();
            long time = eventTimer.getEndTime() - eventTimer.getStartTime() + eventTimer.getEventAccumulatedDuration();
            assertThat((double) time, closeTo(20000, 1000));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试事件暂停功能操作流程  start(0)------>pause(10)----->pause(20)------>end(30)
     * 结果：10
     */
    @Test
    public void eventPause2() {
        Context context = ApplicationProvider.getApplicationContext();
        SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));
        SensorsDataAPI.sharedInstance().trackTimerStart("event_pause");
        Field field;
        Map<String, EventTimer> map;
        try {
            Thread.sleep(1000);
            field = SensorsDataAPI.class.getDeclaredField("mTrackTimer");
            assertNotNull(field);
            field.setAccessible(true);
            map = (Map<String, EventTimer>) field.get(SensorsDataAPI.sharedInstance());
            assertNotNull(map);
            assertEquals(1, map.size());
            Thread.sleep(9000);
            SensorsDataAPI.sharedInstance().trackTimerPause("event_pause");
            Thread.sleep(10000);
            SensorsDataAPI.sharedInstance().trackTimerPause("event_pause");
            Thread.sleep(10000);
            EventTimer eventTimer = map.get("event_pause");
            eventTimer.duration();
            long time = eventTimer.getEndTime() - eventTimer.getStartTime() + eventTimer.getEventAccumulatedDuration();
            assertThat((double) time, closeTo(10000, 1000));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试事件暂停功能操作流程
     * start(0)------>pause(10)----->resume(30)------start(40)------>pause(50)----->resume(60)------>end(70)
     * 结果：20
     */
    @Test
    public void eventPause3() {
        Context context = ApplicationProvider.getApplicationContext();
        SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));
        SensorsDataAPI.sharedInstance().trackTimerStart("event_pause");
        Field field;
        Map<String, EventTimer> map;
        try {
            Thread.sleep(1000);
            field = SensorsDataAPI.class.getDeclaredField("mTrackTimer");
            assertNotNull(field);
            field.setAccessible(true);
            map = (Map<String, EventTimer>) field.get(SensorsDataAPI.sharedInstance());
            assertNotNull(map);
            assertEquals(1, map.size());
            Thread.sleep(9000);
            SensorsDataAPI.sharedInstance().trackTimerPause("event_pause");
            Thread.sleep(10000);
            SensorsDataAPI.sharedInstance().trackTimerResume("event_pause");
            Thread.sleep(10000);
            SensorsDataAPI.sharedInstance().trackTimerStart("event_pause");
            Thread.sleep(10000);
            SensorsDataAPI.sharedInstance().trackTimerPause("event_pause");
            Thread.sleep(10000);
            SensorsDataAPI.sharedInstance().trackTimerResume("event_pause");
            Thread.sleep(10000);
            EventTimer eventTimer = map.get("event_pause");
            eventTimer.duration();
            long time = eventTimer.getEndTime() - eventTimer.getStartTime() + eventTimer.getEventAccumulatedDuration();
            assertThat((double) time, closeTo(20000, 1000));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getLoginId() {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));
            // 先清除数据，不然一直会存在 xml 中
            SensorsDataAPI.sharedInstance().logout();
            Thread.sleep(1000);
            String loginId = SensorsDataAPI.sharedInstance().getLoginId();
            // loginId 未登录时为空
            assertNull(loginId);
            // 登录
            String userId = "sensors_data_cn";
            SensorsDataAPI.sharedInstance().login(userId);
            Thread.sleep(1000);
            loginId = SensorsDataAPI.sharedInstance().getLoginId();
            assertEquals(loginId, userId);
            // 先清除数据，不然一直会存在 xml 中
            SensorsDataAPI.sharedInstance().login(null);
            loginId = SensorsDataAPI.sharedInstance().getLoginId();
            assertEquals(loginId, userId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void getDistinctId() {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));
            // 先清除数据，不然一直会存在 xml 中
            SensorsDataAPI.sharedInstance().logout();
            Thread.sleep(1000);
            String loginId = SensorsDataAPI.sharedInstance().getLoginId();
            // loginId 未登录时为空
            assertNull(loginId);
            String anonymousId = SensorsDataAPI.sharedInstance().getAnonymousId();
            String distinct_id = SensorsDataAPI.sharedInstance().getDistinctId();
            // loginId 未登录时 匿名 Id 和 Distinct_id 相同
            assertEquals(anonymousId, distinct_id);
            // 登录
            String userId = "sensors_data_cn";
            SensorsDataAPI.sharedInstance().login(userId);
            Thread.sleep(1000);
            loginId = SensorsDataAPI.sharedInstance().getLoginId();
            distinct_id = SensorsDataAPI.sharedInstance().getDistinctId();
            assertEquals(loginId, userId);
            assertEquals(loginId, distinct_id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void logout() {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));
            SensorsDataAPI.sharedInstance().logout();
            Thread.sleep(1000);
            String loginId = SensorsDataAPI.sharedInstance().getLoginId();
            Thread.sleep(1000);
            assertNull(loginId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void itemSet() {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));
            SensorsDataAPI.sharedInstance().deleteAll();
            Thread.sleep(1000);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ProductType", "TV");
            jsonObject.put("ProductDetail", "watch Tv");
            SensorsDataAPI.sharedInstance().itemSet("ProductSet", "100", jsonObject);
            // 刷新入库
            SensorsDataAPI.sharedInstance().flush();
            Thread.sleep(1000);
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://" + context.getPackageName() + ".SensorsDataContentProvider/events");
            Cursor cursor = contentResolver.query(uri, null, null, null);
            assertNotNull(cursor);
            assertEquals(1, cursor.getCount());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void itemDelete() {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));
            SensorsDataAPI.sharedInstance().deleteAll();
            Thread.sleep(1000);
            SensorsDataAPI.sharedInstance().itemDelete("ProductDelete", "100");
            // 刷新入库
            SensorsDataAPI.sharedInstance().flush();
            Thread.sleep(1000);
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://" + context.getPackageName() + ".SensorsDataContentProvider/events");
            Cursor cursor = contentResolver.query(uri, null, null, null);
            assertNotNull(cursor);
            assertEquals(1, cursor.getCount());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void trackWithTimeProperty() {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            SensorsDataAPI.startWithConfigOptions(context, new SAConfigOptions(""));

            // Invalid Time
            SensorsDataAPI.sharedInstance().deleteAll();
            Thread.sleep(1000);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$time", new Date(115, 1, 1));
            jsonObject.put("invalidTime", "invalid");
            SensorsDataAPI.sharedInstance().track("InvalidTime", jsonObject);
            SensorsDataAPI.sharedInstance().flush();
            Thread.sleep(1000);
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(Uri.parse("content://" + context.getPackageName() + ".SensorsDataContentProvider/events"), null, null, null);
            assertNotNull(cursor);
            int dataIndex = cursor.getColumnIndex("data");
            cursor.moveToNext();
            JSONObject jsonObjectData = new JSONObject(cursor.getString(dataIndex));
            Date dateReal = new Date(jsonObjectData.optLong("time"));
            Date dateCurr = new Date(System.currentTimeMillis());
            assertEquals(dateCurr.getYear(), dateReal.getYear());
            assertEquals(dateCurr.getMonth(), dateReal.getMonth());
            assertEquals(dateCurr.getDay(), dateReal.getDay());

            // Valid Time
            SensorsDataAPI.sharedInstance().deleteAll();
            Thread.sleep(1000);
            jsonObject = new JSONObject();
            jsonObject.put("$time", new Date(125, 6, 6));
            jsonObject.put("ValidTime", "valid");
            SensorsDataAPI.sharedInstance().track("InvalidTime", jsonObject);
            SensorsDataAPI.sharedInstance().flush();
            Thread.sleep(1000);
            cursor = contentResolver.query(Uri.parse("content://" + context.getPackageName() + ".SensorsDataContentProvider/events"), null, null, null);
            assertNotNull(cursor);
            dataIndex = cursor.getColumnIndex("data");
            cursor.moveToNext();
            jsonObjectData = new JSONObject(cursor.getString(dataIndex));
            dateReal = new Date(jsonObjectData.optLong("time"));
            dateCurr = new Date(System.currentTimeMillis());
            assertNotEquals(dateCurr.getYear(), dateReal.getYear());
            assertNotEquals(dateCurr.getMonth(), dateReal.getMonth());
            assertNotEquals(dateCurr.getDay(), dateReal.getDay());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void assertValueTest() {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            SAConfigOptions saConfigOptions = new SAConfigOptions("");
            saConfigOptions.enableLog(true);
            SensorsDataAPI.startWithConfigOptions(context, saConfigOptions);
            SensorsDataAPI.sharedInstance().identify("");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Test
    public void SAConfigOptionsTest() {
        try {
            final boolean IS_LOG_ENABLE = true;
            final int FLUSH_BULK_SIZE = 60;
            final int FLUSH_INTERVAL = 6 * 1000;
            final long MAX_CACHE_SIZE = 24 * 1024 * 1024L;
            final int NETWORK_TYPE = SensorsNetworkType.TYPE_WIFI;
            final int AUTO_TRACK_EVENT_TYPE = SensorsDataAPI.AutoTrackEventType.APP_CLICK.getEventValue();
            final boolean FLAG = false;

            Context context = ApplicationProvider.getApplicationContext();

            SAConfigOptions saConfigOptions = new SAConfigOptions("");
            saConfigOptions.enableLog(IS_LOG_ENABLE)
                    .setFlushBulkSize(FLUSH_BULK_SIZE)
                    .setFlushInterval(FLUSH_INTERVAL)
                    .setMaxCacheSize(MAX_CACHE_SIZE)
                    .setNetworkTypePolicy(NETWORK_TYPE)
                    .setAutoTrackEventType(AUTO_TRACK_EVENT_TYPE)
                    .enableVisualizedAutoTrack(FLAG)
                    .enableTrackScreenOrientation(FLAG)
                    .enableHeatMap(FLAG)
                    .enableTrackAppCrash();

            SensorsDataAPI.sharedInstance().startWithConfigOptions(context, saConfigOptions);
            assertEquals(FLUSH_BULK_SIZE, SensorsDataAPI.sharedInstance().getFlushBulkSize());
            assertEquals(FLUSH_INTERVAL, SensorsDataAPI.sharedInstance().getFlushInterval());
            assertEquals(MAX_CACHE_SIZE, SensorsDataAPI.sharedInstance().getMaxCacheSize());
            assertEquals(NETWORK_TYPE, SensorsDataAPI.sharedInstance().getFlushNetworkPolicy());

            assertEquals(FLAG, SensorsDataAPI.sharedInstance().isVisualizedAutoTrackEnabled());
            assertEquals(FLAG, SensorsDataAPI.sharedInstance().isHeatMapEnabled());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Test
    public void maxCacheSizeTest() {
        Context context = ApplicationProvider.getApplicationContext();
        long normal = 16 * 1024 * 1024;
        long greater = 20 * 1024 * 1024;
        SensorsDataAPI.sharedInstance().setMaxCacheSize(greater);
        assertEquals(greater, SensorsDataAPI.sharedInstance().getMaxCacheSize());
        long lesser = 10 * 1024 * 1024;
        SensorsDataAPI.sharedInstance().setMaxCacheSize(lesser);
        assertEquals(normal, SensorsDataAPI.sharedInstance().getMaxCacheSize());
        long negative = -1;
        SensorsDataAPI.sharedInstance().setMaxCacheSize(negative);
        assertEquals(normal, SensorsDataAPI.sharedInstance().getMaxCacheSize());
    }
}