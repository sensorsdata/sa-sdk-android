/*
 * Created by dengshiwei on 2021/04/07.
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

package com.sensorsdata.analytics.android.sdk.data.adapter;

import android.content.ContentValues;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentRemoteSDKConfig;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DbAdapter {
    private static DbAdapter instance;
    private final DbParams mDbParams;
    private DataOperation mTrackEventOperation;
    private DataOperation mPersistentOperation;

    private DbAdapter(SAContextManager saContextManager) {
        mDbParams = DbParams.getInstance(saContextManager.getContext().getPackageName());
        SAConfigOptions saConfigOptions = saContextManager.getInternalConfigs().saConfigOptions;
        if (saConfigOptions.isEnableEncrypt()) {
            mTrackEventOperation = new EncryptDataOperation(saContextManager.getContext().getApplicationContext());
        } else if (saConfigOptions.isTransportEncrypt()) {
            mTrackEventOperation = new TransportEncryption(saContextManager.getContext().getApplicationContext());
        } else {
            mTrackEventOperation = new EventDataOperation(saContextManager.getContext().getApplicationContext());
        }
        mPersistentOperation = new PersistentDataOperation(saContextManager.getContext().getApplicationContext());
    }

    public static DbAdapter getInstance(SAContextManager saContextManager) {
        if (instance == null) {
            instance = new DbAdapter(saContextManager);
        }
        return instance;
    }

    public static DbAdapter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(SAContextManager saContextManager) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j) {
        int code = mTrackEventOperation.insertData(mDbParams.getEventUri(), j);
        if (code == 0) {
            return mTrackEventOperation.queryDataCount(mDbParams.getEventUri(), 2);
        }
        return code;
    }

    /**
     * Removes all events from table
     */
    public void deleteAllEvents() {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), DbParams.DB_DELETE_ALL);
    }

    public int cleanupEvents(JSONArray ids, boolean is_instant_event) {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), ids);
        return mTrackEventOperation.queryDataCount(mDbParams.getEventUri(), is_instant_event ? 1 : 0);
    }

    /**
     * 保存启动的页面个数
     *
     * @param activityCount 页面个数
     */
    public void commitActivityCount(int activityCount) {
        try {
            mPersistentOperation.insertData(mDbParams.getActivityStartCountUri(), new JSONObject().put(DbParams.VALUE, activityCount));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取存储的页面个数
     *
     * @return 存储的页面个数
     */
    public int getActivityCount() {
        String[] values = mPersistentOperation.queryData(mDbParams.getActivityStartCountUri(), 1);
        if (values != null && values.length > 0) {
            return Integer.parseInt(values[0]);
        }
        return 0;
    }

    /**
     * 设置 Activity Start 的时间戳
     *
     * @param appStartTime Activity Start 的时间戳
     */
    public void commitAppStartTime(long appStartTime) {
        try {
            mPersistentOperation.insertData(mDbParams.getAppStartTimeUri(), new JSONObject().put(DbParams.VALUE, appStartTime));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取 Activity Start 的时间戳
     *
     * @return Activity Start 的时间戳
     */
    public long getAppStartTime() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getAppStartTimeUri(), 1);
            if (values != null && values.length > 0) {
                return Long.parseLong(values[0]);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return 0;
    }

    /**
     * 设置 Activity End 的信息
     *
     * @param appEndData Activity End 的信息
     */
    public void commitAppExitData(String appEndData) {
        try {
            mPersistentOperation.insertData(mDbParams.getAppExitDataUri(), new JSONObject().put(DbParams.VALUE, appEndData));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取 Activity End 的信息
     *
     * @return Activity End 的信息
     */
    public String getAppExitData() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getAppExitDataUri(), 1);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 存储 LoginId
     *
     * @param loginId 登录 Id
     */
    public void commitLoginId(String loginId) {
        try {
            mPersistentOperation.insertData(mDbParams.getLoginIdUri(), new JSONObject().put(DbParams.VALUE, loginId));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取 LoginId
     *
     * @return LoginId
     */
    public String getLoginId() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getLoginIdUri(), 1);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 设置 Session 的时长
     *
     * @param sessionIntervalTime Session 的时长
     */
    public void commitSessionIntervalTime(int sessionIntervalTime) {
        try {
            mPersistentOperation.insertData(mDbParams.getSessionTimeUri(), new JSONObject().put(DbParams.VALUE, sessionIntervalTime));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取 Session 的时长
     *
     * @return Session 的时长
     */
    public int getSessionIntervalTime() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getSessionTimeUri(), 1);
            if (values != null && values.length > 0) {
                return Integer.parseInt(values[0]);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return 0;
    }

    /**
     * 查询表中是否有对应的事件
     *
     * @param eventName 事件名
     * @return false 表示已存在，true 表示不存在，是首次
     */
    public boolean isFirstChannelEvent(String[] eventName) {
        try {
            return mTrackEventOperation.queryDataCount(mDbParams.getChannelPersistentUri(),
                    null, DbParams.KEY_CHANNEL_EVENT_NAME + " = ? or " + DbParams.KEY_CHANNEL_EVENT_NAME + " = ? ", eventName, null) <= 0;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 添加渠道事件
     *
     * @param eventName 事件名
     */
    public void addChannelEvent(String eventName) {
        try {
            ContentValues values = new ContentValues();
            values.put(DbParams.KEY_CHANNEL_EVENT_NAME, eventName);
            mTrackEventOperation.insertData(mDbParams.getChannelPersistentUri(), values);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 保存子进程上报数据的状态
     *
     * @param flushState 上报状态
     */
    public void commitSubProcessFlushState(boolean flushState) {
        try {
            mPersistentOperation.insertData(mDbParams.getSubProcessUri(), new JSONObject().put(DbParams.VALUE, flushState));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取子进程上报数据状态
     *
     * @return 上报状态
     */
    public boolean isSubProcessFlushing() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getSubProcessUri(), 1);
            if (values != null && values.length > 0) {
                return Integer.parseInt(values[0]) == 1;
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return true;
    }

    /**
     * 存储 identities
     *
     * @param identities ID 标识
     */
    public void commitIdentities(String identities) {
        try {
            final String encodeIdentities = "Base64:" + Base64Coder.encodeString(identities);
            mPersistentOperation.insertData(mDbParams.getUserIdentities(), new JSONObject().put(DbParams.VALUE, encodeIdentities));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取 identities
     *
     * @return ID 标识
     */
    public String getIdentities() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getUserIdentities(), 1);
            if (values != null && values.length > 0) {
                return decodeIdentities(values[0]);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return null;
    }

    public static String decodeIdentities(String identities) {
        if (identities == null) {
            return null;
        }
        return Base64Coder.decodeString(identities.substring(identities.indexOf(":") + 1));
    }

    /**
     * 存储 LoginId
     *
     * @param loginIdKey 登录 Id
     */
    public void commitLoginIdKey(String loginIdKey) {
        try {
            mPersistentOperation.insertData(mDbParams.getLoginIdKeyUri(), new JSONObject().put(DbParams.VALUE, loginIdKey));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取 LoginIdKey
     *
     * @return LoginIdKey
     */
    public String getLoginIdKey() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getLoginIdKeyUri(), 1);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 保存远程控制下发字段
     *
     * @param config 下发字段
     */
    public void commitRemoteConfig(String config) {
        try {
            mPersistentOperation.insertData(mDbParams.getRemoteConfigUri(), new JSONObject().put(DbParams.VALUE, config));
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 获取远程控制下发字段
     *
     * @return 下发字段
     */
    public String getRemoteConfig() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getRemoteConfigUri(), 1);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 获取远程控制下发字段，从当前进程读取
     *
     * @return 下发字段
     */
    public String getRemoteConfigFromLocal() {
        try {
            PersistentRemoteSDKConfig persistentRemoteSDKConfig = PersistentLoader.getInstance().getRemoteSDKConfig();
            return persistentRemoteSDKConfig == null ? "" : persistentRemoteSDKConfig.get();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    public void commitPushID(String key, String pushId) {
        try {
            JSONObject jsonObject = new JSONObject().put(DbParams.PUSH_ID_KEY, key).put(DbParams.PUSH_ID_VALUE, pushId);
            mPersistentOperation.insertData(mDbParams.getPushIdUri(), jsonObject);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }


    public String getPushId(String key) {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getPushIdUri().buildUpon().appendQueryParameter(DbParams.PUSH_ID_KEY, key).build(), 1);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    public void removePushId(String key) {
        mPersistentOperation.deleteData(mDbParams.getPushIdUri(), key);
    }

    /**
     * 从 Event 表中读取上报数据
     *
     * @param tableName 表名
     * @param limit 条数限制
     * @param is_instant_event 是否实时数据
     * @return 数据
     */
    public String[] generateDataString(String tableName, int limit, boolean is_instant_event) {
        try {
            return mTrackEventOperation.queryData(mDbParams.getEventUri(), is_instant_event, limit);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}