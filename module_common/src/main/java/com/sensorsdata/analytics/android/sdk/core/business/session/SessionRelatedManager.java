/*
 * Created by dengshiwei on 2022/06/21.
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

package com.sensorsdata.analytics.android.sdk.core.business.session;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * session 相关管理类-单例
 * 主要功能：
 * 1.session 字段的数据生成、修改，存储、删除。
 * 2.session 切割规则的管理
 * 3.根据 session 规则，对外部 json 逻辑处理。
 * （1）对外部 json 是否添加 session 进行功能逻辑处理，例如：添加和修改 session。
 * （2）对外部 json 是否添加 session 进行业务逻辑处理，例如，只对 track 事件处理，profile、item 相关事件不包含。
 */
public class SessionRelatedManager {
    private volatile static SessionRelatedManager mSessionRelatedManager = null;
    private final String SHARED_PREF_SESSION_CUTDATA = "sensorsdata.session.cutdata";
    public final String EVENT_SESSION_ID = "$event_session_id";
    private final String KEY_SESSION_ID = "sessionID";
    private final String KEY_START_TIME = "startTime";
    private final String KEY_LAST_EVENT_TIME = "lastEventTime";
    private long SESSION_LAST_INTERVAL_TIME = 5 * 60 * 1000;
    private final long SESSION_START_INTERVAL_TIME = 12 * 60 * 60 * 1000;

    /**
     * UUID
     */
    private String mSessionID;
    /**
     * 事件触发时间
     */
    private long mStartTime;
    /**
     * 最近一次事件触发时间
     */
    private long mLastEventTime;

    public static SessionRelatedManager getInstance() {
        if (null == mSessionRelatedManager) {
            synchronized (SessionRelatedManager.class) {
                if (null == mSessionRelatedManager) {
                    mSessionRelatedManager = new SessionRelatedManager();
                }
            }
        }
        return mSessionRelatedManager;
    }

    private SessionRelatedManager() {
        try {
            setSessionLastIntervalTime(SensorsDataAPI.getConfigOptions().getEventSessionTimeout());
            if (!SensorsDataAPI.getConfigOptions().isEnableSession()) {
                deleteSessionData();
            } else {
                readSessionData();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 处理事件中的 event_session_id
     * 1.是否开始 session 的采集
     * 2.是否有 event_session_id 事件
     * 3.事件时间是否满足，是否要更新 session
     * 4.保存数据
     *
     * @param eventName 事件名
     * @param property 事件 json 数据
     * @param time 时间戳
     */
    public void handleEventOfSession(String eventName, JSONObject property, long time) {
        if (!SensorsDataAPI.getConfigOptions().isEnableSession()) return;
        try {
            if ("$AppEnd".equals(eventName)) {
                if (time > mLastEventTime) {//$AppEnd 需要更新上一个时间戳
                    mLastEventTime = time;
                }
                return;
            }
            handleSessionState(time);
            property.put(EVENT_SESSION_ID, mSessionID);
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 更新 session 数据中的 lastEventTime
     */
    private void updateSessionLastTime(long eventTime) {
        mLastEventTime = eventTime;
        SAStoreManager.getInstance().setString(SHARED_PREF_SESSION_CUTDATA, getSessionDataPack());
    }

    /**
     * 删除 session 数据
     */
    private void deleteSessionData() {
        mSessionID = null;
        mStartTime = -1;
        mLastEventTime = -1;
        SAStoreManager.getInstance().remove(SHARED_PREF_SESSION_CUTDATA);
    }

    /**
     * 创建 Session 数据
     */
    private synchronized void createSessionData(long eventTime, boolean isUpdateSessionStartTime) {
        mSessionID = UUID.randomUUID().toString();
        if (isUpdateSessionStartTime) {
            mStartTime = eventTime;
        }
        mLastEventTime = Math.max(eventTime, mLastEventTime);  // 避免补发 $AppEnd 事件时间戳被覆盖
        SAStoreManager.getInstance().setString(SHARED_PREF_SESSION_CUTDATA, getSessionDataPack());
    }

    /**
     * 从 Sp 中读取 session 的数据
     */
    private void readSessionData() {
        String sessionJson = SAStoreManager.getInstance().getString(SHARED_PREF_SESSION_CUTDATA, "");
        if (TextUtils.isEmpty(sessionJson)) return;
        try {
            JSONObject jsonObject = new JSONObject(sessionJson);
            if (jsonObject.has(KEY_SESSION_ID)) {
                mSessionID = jsonObject.optString(KEY_SESSION_ID);
            }
            if (jsonObject.has(KEY_START_TIME)) {
                mStartTime = jsonObject.optLong(KEY_START_TIME);
            }
            if (jsonObject.has(KEY_LAST_EVENT_TIME)) {
                mLastEventTime = jsonObject.optLong(KEY_LAST_EVENT_TIME);
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 处理 session 的状态
     *
     * @param eventTime 事件触发时间
     */
    private synchronized void handleSessionState(long eventTime) {
        if (eventTime <= 0) return;
        if (TextUtils.isEmpty(mSessionID) || eventTime - mLastEventTime > SESSION_LAST_INTERVAL_TIME || eventTime - mStartTime > SESSION_START_INTERVAL_TIME) {
            //生成 session
            createSessionData(eventTime, true);
        } else {
            //更新 session
            updateSessionLastTime(eventTime);
        }
    }

    /**
     * 刷新的时间 不对 session 时间生效，只有当前超过 30 分钟，或者 12 小时生效。
     *
     * @param refreshTime 心跳机制刷新时间
     */
    public void refreshSessionByTimer(long refreshTime) {
        if (refreshTime - mLastEventTime > SESSION_LAST_INTERVAL_TIME) {
            createSessionData(refreshTime, TextUtils.isEmpty(mSessionID));  // 首个打点的需要更新 Session 启动时间戳
        }
    }

    private String getSessionDataPack() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_SESSION_ID, mSessionID);
            jsonObject.put(KEY_START_TIME, mStartTime);
            jsonObject.put(KEY_LAST_EVENT_TIME, mLastEventTime);
            return jsonObject.toString();
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    public String getSessionID() {
        return mSessionID;
    }

    private void setSessionLastIntervalTime(int intervalTime) {
        if (intervalTime > 0) {
            SESSION_LAST_INTERVAL_TIME = intervalTime * 1000L;
        }
    }
}
