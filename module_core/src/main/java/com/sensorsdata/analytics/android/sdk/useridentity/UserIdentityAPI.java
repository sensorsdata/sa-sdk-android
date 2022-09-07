/*
 * Created by dengshiwei on 2021/03/25.
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

package com.sensorsdata.analytics.android.sdk.useridentity;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentDistinctId;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.monitor.TrackMonitor;
import com.sensorsdata.analytics.android.sdk.useridentity.h5identity.H5UserIdentityStrategy;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

import java.util.UUID;

public final class UserIdentityAPI implements IUserIdentityAPI {
    private static final String TAG = "SA.UserIdentityAPI";
    private final SAContextManager mSAContextManager;
    private final PersistentDistinctId mAnonymousId;

    //临时方案避免主线程获取登录 ID 同子线程不同步
    private String mLoginIdValue = null;
    private final Identities mIdentitiesInstance;
    private H5UserIdentityStrategy mH5UserIdentityStrategy;

    public UserIdentityAPI(SAContextManager contextManager) {
        this.mSAContextManager = contextManager;
        this.mAnonymousId = PersistentLoader.getInstance().getAnonymousIdPst();
        mIdentitiesInstance = new Identities();
        try {
            String mayEmpty_anonymousId = null;
            if (this.mAnonymousId != null && this.mAnonymousId.isExists()) {
                mayEmpty_anonymousId = mAnonymousId.get();
            }
            mIdentitiesInstance.init(mayEmpty_anonymousId, SensorsDataUtils.getIdentifier(contextManager.getContext()), mAnonymousId.get());
            mLoginIdValue = mIdentitiesInstance.getJointLoginID();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public String getDistinctId() {
        try {
            String loginId = getLoginId();
            if (!TextUtils.isEmpty(loginId)) {
                return loginId;
            }
            return getAnonymousId();
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    @Override
    public String getAnonymousId() {
        try {
            synchronized (mAnonymousId) {
                return mAnonymousId.get();
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public void resetAnonymousId() {
        try {
            synchronized (mAnonymousId) {
                SALog.i(TAG, "resetAnonymousId is called");
                String androidId = SensorsDataUtils.getIdentifier(mSAContextManager.getContext());
                if (androidId.equals(mAnonymousId.get())) {
                    SALog.i(TAG, "DistinctId not change");
                    return;
                }
                String newDistinctId;
                if (SensorsDataUtils.isValidAndroidId(androidId)) {
                    newDistinctId = androidId;
                } else {
                    newDistinctId = UUID.randomUUID().toString();
                }
                mAnonymousId.commit(newDistinctId);
                if (mIdentitiesInstance.getIdentities(Identities.State.DEFAULT).has(Identities.ANONYMOUS_ID)) {
                    mIdentitiesInstance.updateSpecialIDKeyAndValue(Identities.SpecialID.ANONYMOUS_ID, mAnonymousId.get());
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
                TrackMonitor.getInstance().callResetAnonymousId(newDistinctId);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 用于主线程调用 login 时及时更新 LoginId 值
     *
     * @param loginIDKey 登录 loginIDKey
     * @param loginId 登录 ID
     */
    public void updateLoginId(String loginIDKey, String loginId) {
        mLoginIdValue = LoginIDAndKey.jointLoginID(loginIDKey, loginId);
    }


    @Override
    public String getLoginId() {
        if (AppInfoUtils.isTaskExecuteThread()) {
            return mIdentitiesInstance.getJointLoginID();
        }
        return mLoginIdValue;
    }

    @Override
    public void identify(String distinctId) {
        try {
            SALog.i(TAG, "identify is called");
            synchronized (mAnonymousId) {
                if (!distinctId.equals(mAnonymousId.get())) {
                    mAnonymousId.commit(distinctId);
                    mIdentitiesInstance.updateSpecialIDKeyAndValue(Identities.SpecialID.ANONYMOUS_ID, distinctId);
                    if (mSAContextManager.getEventListenerList() != null) {
                        for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                            try {
                                eventListener.identify();
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }
                        }
                    }
                    TrackMonitor.getInstance().callIdentify(distinctId);
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void login(String loginId) {
        loginWithKeyBack(LoginIDAndKey.LOGIN_ID_KEY_DEFAULT, loginId);
    }

    @Override
    public void login(String loginId, JSONObject properties) {
        loginWithKeyBack(LoginIDAndKey.LOGIN_ID_KEY_DEFAULT, loginId);
    }

    @Override
    public void loginWithKey(String loginIDKey, String loginID) {
        loginWithKeyBack(loginIDKey, loginID);
    }

    @Override
    public void loginWithKey(String loginIDKey, String loginID, JSONObject properties) {
        loginWithKeyBack(loginIDKey, loginID);
    }

    /**
     * 实际登录逻辑处理
     *
     * @param loginIDKey 登录 IDKey
     * @param loginID 登录 ID
     * @return 返回是否登录成功
     */
    public boolean loginWithKeyBack(String loginIDKey, String loginID) {
        boolean flag;
        try {
            flag = mIdentitiesInstance.updateLoginKeyAndID(loginIDKey, loginID, getAnonymousId());
            if (flag) {
                //1、回调给业务 loginID
                if (mSAContextManager.getEventListenerList() != null) {
                    for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                        try {
                            eventListener.login();
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                }
                TrackMonitor.getInstance().callLogin(mIdentitiesInstance.getJointLoginID());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
            flag = false;
        }
        return flag;
    }

    @Override
    public void logout() {
        //1、避免 SensorsDataContentObserver 跨进程数据不断进来
        if (TextUtils.isEmpty(mIdentitiesInstance.getLoginId())) {
            return;
        }
        SALog.i(TAG, "logout is called");
        //2、进行登录 IDKey 和 ID 的重置、identities 的处理
        mIdentitiesInstance.removeLoginKeyAndID();
        // 3、进行通知调用 logout 接口
        if (mSAContextManager.getEventListenerList() != null) {
            for (SAEventListener eventListener : mSAContextManager.getEventListenerList()) {
                try {
                    eventListener.logout();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
        TrackMonitor.getInstance().callLogout();
        SALog.i(TAG, "Clean loginId");
    }

    @Override
    public void bind(String key, String value) {
        bindBack(key, value);
    }

    public boolean bindBack(String key, String value) {
        boolean flag;
        try {
            flag = mIdentitiesInstance.update(key, value);
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return false;
        }
        return flag;
    }

    @Override
    public void unbind(String key, String value) {
        unbindBack(key, value);
    }

    public boolean unbindBack(String key, String value) {
        boolean flag;
        try {
            flag = mIdentitiesInstance.remove(key, value);
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return false;
        }
        return flag;
    }

    /**
     * 读取对应的 identities 属性
     *
     * @param eventType 事件类型
     * @return identities 属性
     */
    public JSONObject getIdentities(EventType eventType) {
        if (EventType.TRACK_SIGNUP == eventType) {
            return mIdentitiesInstance.getIdentities(Identities.State.LOGIN_KEY);
        } else if (EventType.TRACK_ID_UNBIND == eventType) {
            return mIdentitiesInstance.getIdentities(Identities.State.REMOVE_KEYID);
        } else {
            return mIdentitiesInstance.getIdentities(Identities.State.DEFAULT);
        }
    }

    @Override
    public JSONObject getIdentities() {
        return mIdentitiesInstance.getIdentities(Identities.State.DEFAULT);
    }

    public Identities getIdentitiesInstance() {
        return mIdentitiesInstance;
    }

    /**
     * 合并 H5 的 Identities 属性
     *
     * @param eventType 事件类型
     * @param eventObject 属性
     *
     * @return true 代表 h5 打通用户标识成功，false 代表 h5 打通用户标识失败
     */
    public boolean mergeH5Identities(EventType eventType, JSONObject eventObject) {
        if (mH5UserIdentityStrategy == null) {
            mH5UserIdentityStrategy = new H5UserIdentityStrategy(this);
        }
        return mH5UserIdentityStrategy.processH5UserIdentity(eventType, eventObject);
    }

    public void trackH5Notify(JSONObject eventObject) {
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
        TrackMonitor.getInstance().callTrack(eventObject);
    }
}