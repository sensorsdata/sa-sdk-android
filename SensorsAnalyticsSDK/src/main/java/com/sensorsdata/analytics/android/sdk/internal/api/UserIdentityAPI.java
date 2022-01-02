/*
 * Created by dengshiwei on 2021/03/25.
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

package com.sensorsdata.analytics.android.sdk.internal.api;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentDistinctId;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.UserIdentityPersistent;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.listener.SAFunctionListener;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SAContextManager;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class UserIdentityAPI implements IUserIdentityAPI {
    private static final String TAG = "UserIdentityAPI";
    private SAContextManager mSAContextManager;
    private final PersistentDistinctId mDistinctId;
    private String LOGIN_ID_KEY = "$identity_login_id";
    private static final String LOGIN_ID = "$identity_login_id";
    private static final String ANONYMOUS_ID = "$identity_anonymous_id";
    private static final String ANDROID_ID = "$identity_android_id";
    private static final String ANDROID_UUID = "$identity_android_uuid";
    private static final String COOKIE_ID = "$identity_cookie_id";
    private static final String IDENTITIES_KEY = "identities";
    private final Object mLoginIdLock = new Object();
    /* LoginId */
    private String mLoginIdValue = null;
    private JSONObject mIdentities;
    private JSONObject mLoginIdentities;
    private JSONObject mUnbindIdentities;
    // 是否重置过匿名 ID
    private boolean isResetAnonymousId = false;

    public UserIdentityAPI(SAContextManager contextManager, SAConfigOptions saConfigOptions) {
        this.mSAContextManager = contextManager;
        this.mDistinctId = (PersistentDistinctId) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.DISTINCT_ID);
        try {
            String loginIDKey = saConfigOptions.getLoginIDKey();
            if (!LOGIN_ID.equals(loginIDKey)) {
                if (SADataHelper.assertPropertyKey(loginIDKey) && isKeyValid(loginIDKey)) {
                    LOGIN_ID_KEY = loginIDKey;
                } else {
                    SALog.i(TAG, "The LoginIDKey '" + loginIDKey + "' is invalid.");
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        initIdentities();
    }

    @Override
    public String getDistinctId() {
        try {
            String loginId = getLoginId();
            if (TextUtils.isEmpty(loginId)) {// 如果从本地缓存读取失败，则尝试使用内存中的 LoginId 值
                loginId = mLoginIdValue;
            }
            if (!TextUtils.isEmpty(loginId)) {
                return loginId;
            }
            return getAnonymousId();
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public String getAnonymousId() {
        try {
            synchronized (mDistinctId) {
                if (!SensorsDataAPI.getConfigOptions().isDataCollect()) {
                    return "";
                }
                return mDistinctId.get();
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public void resetAnonymousId() {
        try {
            synchronized (mDistinctId) {
                SALog.i(TAG, "resetAnonymousId is called");
                String androidId = mSAContextManager.getAndroidId();
                if (androidId.equals(mDistinctId.get())) {
                    SALog.i(TAG, "DistinctId not change");
                    return;
                }

                isResetAnonymousId = true;
                if (!SensorsDataAPI.getConfigOptions().isDataCollect()) {
                    return;
                }
                String newDistinctId;
                if (SensorsDataUtils.isValidAndroidId(androidId)) {
                    newDistinctId = androidId;
                } else {
                    newDistinctId = UUID.randomUUID().toString();
                }
                mDistinctId.commit(newDistinctId);
                if (mIdentities.has(ANONYMOUS_ID)) {
                    updateIdentities(ANONYMOUS_ID, mDistinctId.get());
                }

                // 通知调用 resetAnonymousId 接口
                if (mSAContextManager.getEventListenerList() != null) {
                    for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                        try {
                            eventListener.resetAnonymousId();
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                }

                if (mSAContextManager.getFunctionListenerList() != null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("distinctId", newDistinctId);
                    for (SAFunctionListener listener : mSAContextManager.getFunctionListenerList()) {
                        try {
                            listener.call("resetAnonymousId", jsonObject);
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public String getLoginId() {
        if (AppInfoUtils.isTaskExecuteThread()) {
            return DbAdapter.getInstance().getLoginId();
        }
        return mLoginIdValue;
    }

    @Override
    public void identify(String distinctId) {
        try {
            SALog.i(TAG, "identify is called");
            synchronized (mDistinctId) {
                if (!distinctId.equals(mDistinctId.get())) {
                    mDistinctId.commit(distinctId);
                    updateIdentities(ANONYMOUS_ID, distinctId);

                    if (mSAContextManager.getEventListenerList() != null) {
                        for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                            try {
                                eventListener.identify();
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }
                        }
                    }

                    if (mSAContextManager.getFunctionListenerList() != null) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("distinctId", distinctId);
                        for (SAFunctionListener listener : mSAContextManager.getFunctionListenerList()) {
                            try {
                                listener.call("identify", jsonObject);
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void login(String loginId) {
        login(loginId, null);
    }

    @Override
    public void login(String loginId, JSONObject properties) {
        try {
            mLoginIdValue = loginId;
            DbAdapter.getInstance().commitLoginId(loginId);
            // 先同步 identities ，多进程在 A 进程修改了，再次进入 B 进程调用 login
            mLoginIdentities = new JSONObject(DbAdapter.getInstance().getIdentities());
            DbAdapter.getInstance().commitLoginIdKey(LOGIN_ID_KEY);
            // 更新 identities 相关信息
            updateIdentities(LOGIN_ID_KEY, loginId);
            clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID, LOGIN_ID_KEY));
            // 通知调用 login 接口
            if (mSAContextManager.getEventListenerList() != null) {
                for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                    try {
                        eventListener.login();
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
            if (mSAContextManager.getFunctionListenerList() != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("distinctId", loginId);
                for (SAFunctionListener listener : mSAContextManager.getFunctionListenerList()) {
                    try {
                        listener.call("login", jsonObject);
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void logout() {
        try {
            synchronized (mLoginIdLock) {
                SALog.i(TAG, "logout is called");
                if (!TextUtils.isEmpty(getLoginId())) {
                    try {
                        DbAdapter.getInstance().commitLoginId(null);
                        mLoginIdValue = null;
                    } catch (Exception ex) {
                        SALog.printStackTrace(ex);
                    }

                    // 进行通知调用 logout 接口
                    if (mSAContextManager.getEventListenerList() != null) {
                        for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                            try {
                                eventListener.logout();
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }
                        }
                    }

                    if (mSAContextManager.getFunctionListenerList() != null) {
                        for (SAFunctionListener listener : mSAContextManager.getFunctionListenerList()) {
                            try {
                                listener.call("logout", null);
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }
                        }
                    }
                    SALog.i(TAG, "Clean loginId");
                }
                DbAdapter.getInstance().commitLoginIdKey("");
                clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID));
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void bind(String key, String value) throws InvalidDataException {
        if (!isKeyValid(key) || !SADataHelper.assertPropertyKey(key)) {
            throw new InvalidDataException("bind key is invalid, key = " + key);
        }
        SADataHelper.assertDistinctId(value);
        try {
            updateIdentities(key, value);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void unbind(String key, String value) throws InvalidDataException {
        if (!isKeyValid(key) || !SADataHelper.assertPropertyKey(key)) {
            throw new InvalidDataException("unbind key is invalid, key = " + key);
        }
        SADataHelper.assertDistinctId(value);
        try {
            mUnbindIdentities = new JSONObject();
            mUnbindIdentities.put(key, value);
            if (mIdentities.has(key) && value.equals(mIdentities.getString(key))) {
                mIdentities.remove(key);
                DbAdapter.getInstance().commitIdentities(mIdentities.toString());
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 读取对应的 identities 属性
     *
     * @param eventType 事件类型
     * @return identities 属性
     */
    public JSONObject getIdentities(EventType eventType) {
        if (EventType.TRACK_SIGNUP == eventType) {
            return mLoginIdentities;
        } else if (EventType.TRACK_ID_UNBIND == eventType) {
            return mUnbindIdentities;
        } else {
            return mIdentities;
        }
    }

    /**
     * 用于主线程调用 login 时及时更新 LoginId 值
     * @param loginId LoginId
     */
    public void updateLoginId(String loginId) {
        mLoginIdValue = loginId;
    }

    /**
     * 同意隐私权限
     *
     * @param androidId AndroidId
     */
    public void enableDataCollect(String androidId) {
        try {
            String key, value;
            if (SensorsDataUtils.isValidAndroidId(androidId)) {
                if (TextUtils.isEmpty(mDistinctId.get()) || isResetAnonymousId) {// 未调用过 identify 或调用过 resetAnonymousId
                    mDistinctId.commit(androidId);
                }
                key = ANDROID_ID;
                value = androidId;
            } else {
                String uuid = UUID.randomUUID().toString();
                if (TextUtils.isEmpty(mDistinctId.get()) || isResetAnonymousId) {// 未调用过 identify 或调用过 resetAnonymousId
                    mDistinctId.commit(uuid);
                }
                key = ANDROID_UUID;
                value = uuid;
            }
            if (mIdentities.has(ANONYMOUS_ID) && isResetAnonymousId) {
                updateIdentities(ANONYMOUS_ID, mDistinctId.get());
            }
            mLoginIdentities.put(key, value);
            mIdentities.put(key, value);
            DbAdapter.getInstance().commitIdentities(mIdentities.toString());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 同意隐私权限时更新默认 ID
     *
     * @param identitiesJson JSONObject
     */
    public void updateIdentities(JSONObject identitiesJson) {
        try {
            if (SensorsDataUtils.isValidAndroidId(mSAContextManager.getAndroidId())) {
                identitiesJson.put(ANDROID_ID, mSAContextManager.getAndroidId());
            } else {
                identitiesJson.put(ANDROID_UUID, mDistinctId.get());
            }
            if (mIdentities.has(ANONYMOUS_ID) && isResetAnonymousId) {
                identitiesJson.put(ANONYMOUS_ID, mDistinctId.get());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 合并 H5 的 Identities 属性
     *
     * @param eventType 事件类型
     * @param eventObject 属性
     * @throws InvalidDataException 数据不合法
     */
    public void mergeH5Identities(EventType eventType, JSONObject eventObject) throws InvalidDataException {
        // 先判断 H5 事件中是否存在 identities
        JSONObject identityJson = null;
        String identities = eventObject.optString(IDENTITIES_KEY);
        try {
            if (!TextUtils.isEmpty(identities)) {
                identityJson = new JSONObject(identities);
                identityJson.remove(ANDROID_ID);
                identityJson.remove(ANONYMOUS_ID);
                identityJson.remove(ANDROID_UUID);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }

        try {
            if (EventType.TRACK_SIGNUP == eventType) {
                mergeSignUpH5(eventObject, identityJson);
            } else if (EventType.TRACK_ID_BIND == eventType) {
                mergeBindH5(eventObject, identityJson);
            } else if (EventType.TRACK_ID_UNBIND == eventType && identityJson != null) {
                mergeUnbindH5(eventObject, identityJson);
            } else {
                mergeTrackH5(eventObject, identityJson);
            }
        } catch (JSONException exception) {
            SALog.printStackTrace(exception);
        }
    }

    /**
     * 从本地缓存文件中加载 Identities
     */
    public void loadIdentitiesFromFile() {
        try {
            mIdentities = new JSONObject(DbAdapter.getInstance().getIdentities());
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 是否合法的 LoginId，不等于登录 ID 且不等于匿名 ID，或者 LoginIdKey 发生变化且不等于匿名 ID
     *
     * @param loginId LoginId
     * @return 合法 true，不合法 false
     */
    public boolean isLoginIdValid(String loginId) {
        String anonymousId = getAnonymousId();
        return !loginId.equals(DbAdapter.getInstance().getLoginId()) && !loginId.equals(anonymousId)
                || !DbAdapter.getInstance().getLoginIdKey().equals(LOGIN_ID_KEY) && !loginId.equals(anonymousId);
    }

    /**
     * 更新 Identities 属性
     *
     * @param key Key
     * @param value Value
     */
    private void updateIdentities(String key, String value) {
        try {
            mIdentities.put(key, value);
            mLoginIdentities.put(key, value);
            DbAdapter.getInstance().commitIdentities(mIdentities.toString());
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 初始化 Identities
     */
    private void initIdentities() {
        try {
            mIdentities = new JSONObject();
            mLoginIdentities = new JSONObject();
            // 判断 identities 是否存在
            final UserIdentityPersistent userPersistent = (UserIdentityPersistent) PersistentLoader.loadPersistent(DbParams.PERSISTENT_USER_ID);
            if (userPersistent != null && userPersistent.isExists()) {
                String identities = DbAdapter.getInstance().getIdentities();
                if (!TextUtils.isEmpty(identities)) {
                    mIdentities = new JSONObject(identities);
                    if (mIdentities.has(ANONYMOUS_ID)) {
                        mIdentities.put(ANONYMOUS_ID, mDistinctId.get());
                    }
                }
            } else {
                // 判断匿名 ID 是否存在
                if (mDistinctId.isExists()) {
                    mIdentities.put(ANONYMOUS_ID, mDistinctId.get());
                }
                // 同意合规时
                if (SensorsDataAPI.getConfigOptions().isDataCollect()) {
                    if (SensorsDataUtils.isValidAndroidId(mSAContextManager.getAndroidId())) {
                        mIdentities.put(ANDROID_ID, mSAContextManager.getAndroidId());
                    } else {
                        mIdentities.put(ANDROID_UUID, UUID.randomUUID().toString());
                    }
                }
            }

            String loginIdValue = DbAdapter.getInstance().getLoginId();
            String oldLoginKey = DbAdapter.getInstance().getLoginIdKey();
            if (!TextUtils.isEmpty(loginIdValue)) {
                mLoginIdValue = loginIdValue;
                if (mIdentities.has(oldLoginKey)) {
                    String oldLoginValue = mIdentities.getString(oldLoginKey);
                    if (!loginIdValue.equals(oldLoginValue)) {
                        mIdentities.put(oldLoginKey, loginIdValue);
                        clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID, oldLoginKey));
                    }
                } else {
                    mIdentities.put(LOGIN_ID_KEY, loginIdValue);
                    DbAdapter.getInstance().commitLoginIdKey(LOGIN_ID_KEY);
                    clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID, LOGIN_ID_KEY));
                }
            } else {
                // 如果 Identities 中已经登录的 ID，则进行清除
                if (mIdentities.has(oldLoginKey)) {
                    mLoginIdValue = null;
                    clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID, ANONYMOUS_ID));
                }
                DbAdapter.getInstance().commitLoginIdKey("");
            }
            String identities = mIdentities.toString();
            mLoginIdentities = new JSONObject(identities);
            DbAdapter.getInstance().commitIdentities(identities);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 清除 keys
     *
     * @param whiteListKey 白名单中的 Key
     */
    private void clearIdentities(List<String> whiteListKey) {
        if (mIdentities != null) {
            Iterator<String> iterator = mIdentities.keys();
            while (iterator.hasNext()) {
                if (!whiteListKey.contains(iterator.next())) {
                    iterator.remove();
                }
            }
            DbAdapter.getInstance().commitIdentities(mIdentities.toString());
        }
    }

    private void mergeSignUpH5(JSONObject eventObject, JSONObject identityJson) throws JSONException, InvalidDataException {
        String loginId;
        boolean isH5IdentityHasLoginId = true;
        if (identityJson != null) {
            loginId = identityJson.optString(LOGIN_ID_KEY);
            if (TextUtils.isEmpty(loginId)) {
                isH5IdentityHasLoginId = false;
            }
        } else {
            identityJson = new JSONObject();
            loginId = eventObject.optString("distinct_id");
        }

        if (!isH5IdentityHasLoginId) {//如果 H5 的 identities 不为空但是 LoginId 为空
            // 合并原生部分的 Identities 属性
            SensorsDataUtils.mergeJSONObject(mIdentities, identityJson);
            eventObject.put(IDENTITIES_KEY, identityJson);
            return;
        }

        // 检查 $identity_login_id 是否合法
        if (!isLoginIdValid(loginId)) {
            throw new InvalidDataException("The " + loginId + " is invalid.");
        }
        SADataHelper.assertDistinctId(loginId);
        mIdentities.put(LOGIN_ID_KEY, loginId);
        // 合并原生部分的 Identities 属性
        SensorsDataUtils.mergeJSONObject(mIdentities, identityJson);
        eventObject.put(IDENTITIES_KEY, identityJson);
        eventObject.put("login_id", loginId);
        login(loginId);
    }

    private void mergeBindH5(JSONObject eventObject, JSONObject identityJson) throws JSONException {
        if (identityJson == null) {
            identityJson = new JSONObject();
        } else {
            Iterator<String> iteratorKeys = identityJson.keys();
            // 遍历执行 H5 属性新增到原生侧
            while (iteratorKeys.hasNext()) {
                String key = iteratorKeys.next();
                if (!mIdentities.has(key)) {
                    mIdentities.put(key, identityJson.optString(key));
                }
            }

            mIdentities.remove(COOKIE_ID);
            DbAdapter.getInstance().commitIdentities(mIdentities.toString());
        }
        // 合并原生部分的 Identities 属性
        SensorsDataUtils.mergeJSONObject(mIdentities, identityJson);
        eventObject.put(IDENTITIES_KEY, identityJson);
    }

    private void mergeUnbindH5(JSONObject eventObject, JSONObject identityJson) throws JSONException {
        Iterator<String> iteratorKeys = identityJson.keys();
        while (iteratorKeys.hasNext()) {
            String key = iteratorKeys.next();
            // 匿名 ID，android_uuid 和 android_id 不允许解绑
            if (ANONYMOUS_ID.equals(key) || ANDROID_UUID.equals(key) || ANDROID_ID.equals(key)) {
                continue;
            }

            if (mIdentities.has(key) && mIdentities.get(key).equals(identityJson.opt(key))) {
                mIdentities.remove(key);
            }
        }
        eventObject.put(IDENTITIES_KEY, identityJson);
        DbAdapter.getInstance().commitIdentities(mIdentities.toString());
    }

    private void mergeTrackH5(JSONObject eventObject, JSONObject identityJson) throws JSONException {
        try {
            if (identityJson == null) {
                identityJson = new JSONObject();
            }
            // 合并原生部分的 Identities 属性
            SensorsDataUtils.mergeJSONObject(mIdentities, identityJson);
            eventObject.put(IDENTITIES_KEY, identityJson);
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }

        try {
            if (mSAContextManager.getEventListenerList() != null) {
                for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                    try {
                        eventListener.trackEvent(eventObject);
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        if (mSAContextManager.getFunctionListenerList() != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("eventJSON", eventObject);
            for (SAFunctionListener listener : mSAContextManager.getFunctionListenerList()) {
                try {
                    listener.call("trackEvent", jsonObject);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    /**
     * 检查 Key 是否合法, $identity_anonymous_id，$identity_android_uuid、$identity_android_id、$identity_login_id、$Bind、$Unbind 是保留字段，不允许外部使用
     *
     * @param key Key
     * @return 是否合法
     */
    private boolean isKeyValid(String key) {
        return !ANONYMOUS_ID.equals(key) && !ANDROID_UUID.equals(key) && !ANDROID_ID.equals(key)
                && !LOGIN_ID_KEY.equals(key) && !LOGIN_ID.equals(key)
                && !BIND_ID.equals(key) && !UNBIND_ID.equals(key);
    }
}