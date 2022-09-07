package com.sensorsdata.analytics.android.sdk.useridentity;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.persistent.LoginIdKeyPersistent;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoginId;
import com.sensorsdata.analytics.android.sdk.data.persistent.UserIdentityPersistent;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 主要解决 identities 结构体的生成和具体操作逻辑：具体结构体包含登录 ID（loginKeyID：loginID） 、业务 ID（keyID：valueID）、特殊的 IDKey 和 IDValue
 */
public class Identities {
    private static final String TAG = "SA.Identities";
    private JSONObject mIdentities;
    private JSONObject mLoginIdentities;
    private JSONObject mUnbindIdentities;
    private final LoginIDAndKey mLoginIDAndKey;
    public static final String ANONYMOUS_ID = "$identity_anonymous_id";
    public static final String ANDROID_ID = "$identity_android_id";
    public static final String ANDROID_UUID = "$identity_android_uuid";
    public static final String COOKIE_ID = "$identity_cookie_id";
    public static final String IDENTITIES_KEY = "identities";

    //LOGINKEY 登录 ID 操作状态处理、REMOVEKEYID 业务 ID 操作状态处理
    public enum State {
        LOGIN_KEY, REMOVE_KEYID, DEFAULT
    }

    //特殊的 IDKey 和 IDValue：android_id、ANONYMOUS_ID
    public enum SpecialID {
        ANONYMOUS_ID, ANDROID_ID, ANDROID_UUID
    }

    public Identities() {
        mLoginIDAndKey = new LoginIDAndKey();
    }

    /**
     * 主要是 SDK 初始化的时候进行初始化 Identities
     *
     * @param mayEmpty_anonymousId 进行判断本地是否存储 anonymousId，有存储获取，未存储则是 null
     * @param androidId android_id
     * @param anonymousId 获取本地的 anonymousId，未存储的会创建文件
     * @throws JSONException 抛出 JSONException 异常
     */
    public void init(String mayEmpty_anonymousId, String androidId, String anonymousId) throws JSONException {
        String oldLoginIDKey = Local.getLoginIdKeyFromLocal();
        String oldLoginID = Local.getLoginIdFromLocal();
        mLoginIDAndKey.init(oldLoginIDKey);
        //1. SP 文件缓存读取
        JSONObject identities = getInitIdentities();
        //2.构建 identities：主要针对特殊 ID 处理
        identities = createIdentities(identities, mayEmpty_anonymousId, androidId, anonymousId);
        //3.针对 loginIDKey、loginID 处理
        initLoginIDAndKeyIdentities(oldLoginIDKey, oldLoginID, identities);
        //4.本地保存 identities
        mIdentities = identities;
        saveIdentities();
    }

    private void initLoginIDAndKeyIdentities(String oldLoginIDKey, String oldLoginID, JSONObject identities) throws JSONException {
        if (TextUtils.isEmpty(oldLoginID)) {
            if (identities.has(oldLoginIDKey)) {
                clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID, ANONYMOUS_ID), identities);
                mLoginIDAndKey.setLoginIDKey("");
            }
        } else {
            if (identities.has(oldLoginIDKey)) {
                String identitiesLoginID = identities.getString(mLoginIDAndKey.getLoginIDKey());//oldLoginIDKey 非法的话，前面已经处理了
                if (!identitiesLoginID.equals(oldLoginID)) {
                    identities.put(Local.getLoginIdKeyFromLocal(), Local.getLoginIdFromLocal());
                    clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID, mLoginIDAndKey.getLoginIDKey()), identities);
                }
            } else {
                identities.put(Local.getLoginIdKeyFromLocal(), Local.getLoginIdFromLocal());
                clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID, mLoginIDAndKey.getLoginIDKey()), identities);
            }
        }
    }

    private JSONObject createIdentities(JSONObject identities, String mayEmpty_anonymousId, String androidId, String anonymousId) throws JSONException {
        JSONObject tmp_identities = identities;
        if (tmp_identities == null) {
            tmp_identities = new JSONObject();
            // 判断匿名 ID 是否存在
            if (mayEmpty_anonymousId != null) {
                tmp_identities.put(ANONYMOUS_ID, mayEmpty_anonymousId);
            }
            if (SensorsDataUtils.isValidAndroidId(androidId)) {
                tmp_identities.put(ANDROID_ID, androidId);
            } else {
                tmp_identities.put(ANDROID_UUID, anonymousId);
            }
        } else {
            if (tmp_identities.has(ANONYMOUS_ID)) {
                tmp_identities.put(ANONYMOUS_ID, anonymousId);
            }
        }
        return tmp_identities;
    }

    /**
     * 清除 identities 中的 ID
     *
     * @param whiteListKey 不清除的 IDKey
     * @param identities 待处理的 identities
     */
    private void clearIdentities(List<String> whiteListKey, JSONObject identities) {
        if (identities != null) {
            Iterator<String> iterator = identities.keys();
            while (iterator.hasNext()) {
                if (!whiteListKey.contains(iterator.next())) {
                    iterator.remove();
                }
            }
        }
    }

    public boolean updateLoginKeyAndID(String loginIDKey, String loginID, String anonymousId) throws Exception {
        //1、loginIDKey 和 loginID 校验
        boolean flag = isInValid(loginIDKey, loginID, anonymousId);
        if (flag) {
            return false;
        }
        //2、更新 identities 中的 loginIDKey 和 loginID
        mIdentities.put(loginIDKey, loginID);
        mLoginIdentities = new JSONObject(mIdentities.toString());
        //3、清除业务 ID，只保留 ANDROID_ID, ANDROID_UUID, LOGIN_ID_KEY
        clearIdentities(Arrays.asList(ANDROID_ID, ANDROID_UUID, loginIDKey), mIdentities);
        //4、保存 identities 到本地
        saveIdentities();
        return true;
    }

    /**
     * 移除 Identities 中的 key 和 value
     */
    public void removeLoginKeyAndID() {
        //1、移除 loginIDKey 和 LoginID
        mLoginIDAndKey.removeLoginKeyAndID();
        //2、清除业务 ID，只保留 ANDROID_ID, ANDROID_UUID
        mLoginIdentities = new JSONObject();
        clearIdentities(Arrays.asList(Identities.ANDROID_ID, Identities.ANDROID_UUID), mIdentities);
        //3、保存 identities 到本地
        saveIdentities();
    }

    public boolean update(String key, String value) throws JSONException {
        //1、业务 ID 绑定的 key 和 value 校验
        boolean flag = isInvalidBusinessID(key, value, false);
        if (flag) {
            return false;
        }
        //2、identities 更新
        mIdentities.put(key, value);
        //3、保存 identities
        saveIdentities();
        return true;
    }

    public boolean remove(String key, String value) throws JSONException {
        //1、业务 ID 绑定的 key 和 value 校验
        if (isInvalidBusinessID(key, value, true)) {
            return false;
        }
        //2、identities 更新
        mUnbindIdentities = new JSONObject();
        mUnbindIdentities.put(key, value);
        //当 android_id、android_uuid、cookie_id 不移除对应的 identities 内容
        boolean isNotRemove = Identities.ANDROID_ID.equals(key) || Identities.ANDROID_UUID.equals(key);
        if (!isNotRemove) {
            if (mIdentities.has(key) && mIdentities.getString(key).equals(value)) {
                mIdentities.remove(key);
            }
        }
        //3、允许解绑登录 key : value，这里对这种情况进行登录 key : value 操作
        String tmp_loginID = key + "+" + value;
        if (tmp_loginID.equals(getJointLoginID())) {
            mIdentities.remove(key);
            mLoginIDAndKey.removeLoginKeyAndID();
        }
        //4、保存 identities
        saveIdentities();
        return true;
    }

    /**
     * 主要处理非匿名 ID 的情况
     *
     * @param specialID 待处理的特殊 ID
     * @param value 特殊 ID 值
     * @throws JSONException 异常抛出
     */
    public void updateSpecialIDKeyAndValue(SpecialID specialID, String value) throws JSONException {
        switch (specialID) {
            case ANONYMOUS_ID:
                mIdentities.put(ANONYMOUS_ID, value);
                break;
            case ANDROID_ID:
                mIdentities.put(ANDROID_ID, value);
                break;
            case ANDROID_UUID:
                mIdentities.put(ANDROID_UUID, value);
                break;
            default:
                break;
        }
        saveIdentities();
    }

    //正常是不需要的，内部改变，自身是感知的；但是为了兼容以前的设计方案需要一个 saveIdentities 来保持逻辑的一致性
    private void saveIdentities() {
        DbAdapter.getInstance().commitIdentities(mIdentities.toString());
    }


    /**
     * 更新业务 ID 的校验
     *
     * @param key ID_Key
     * @param value ID_Value
     * @param isRemove true 表示移除业务 ID，false 表示增加业务 ID
     * @return 非法 ID
     */
    private boolean isInvalidBusinessID(String key, String value, boolean isRemove) {
        //1、业务 key 的校验
        boolean flag = false;
        if (isRemove) {
            if (!isRemoveKeyValid(key) || !SADataHelper.assertPropertyKey(key)) {
                flag = true;
                SALog.i(TAG, "unbind key is invalid, key = " + key);
            }
        } else {
            if (!isKeyValid(key) || !SADataHelper.assertPropertyKey(key)) {
                flag = true;
                SALog.i(TAG, "bind key is invalid, key = " + key);
            }
        }

        try {
            SADataHelper.assertDistinctId(value);
        } catch (Exception e) {
            flag = true;
            SALog.i(TAG, e);
        }
        return flag;
    }

    /**
     * 检查 Key 是否合法, $identity_anonymous_id，$identity_android_uuid、$identity_android_id、$identity_login_id是保留字段，不允许外部使用
     *
     * @param key Key 被校验的 key 值
     * @return 是否合法
     */
    private boolean isKeyValid(String key) {
        return !ANONYMOUS_ID.equals(key) && !ANDROID_UUID.equals(key) && !ANDROID_ID.equals(key) && !mLoginIDAndKey.getLoginIDKey().equals(key) && !LoginIDAndKey.LOGIN_ID_KEY_DEFAULT.equals(key);
    }

    private boolean isRemoveKeyValid(String key) {
        return !ANONYMOUS_ID.equals(key) && !LoginIDAndKey.LOGIN_ID_KEY_DEFAULT.equals(key);
    }

    private boolean isInValid(String loginIDKey, String loginID, String anonymousId) {
        return !mLoginIDAndKey.setLoginKeyAndID(loginIDKey, loginID, anonymousId);
    }

    public JSONObject getIdentities(State state) {
        JSONObject jsonObject = null;
        switch (state) {
            case LOGIN_KEY:
                jsonObject = mLoginIdentities;
                break;
            case REMOVE_KEYID:
                if (mUnbindIdentities != null) {
                    jsonObject = mUnbindIdentities;
                }
                break;
            case DEFAULT:
                if (mIdentities == null) {
                    jsonObject = getDefaultIdentities();
                } else {
                    jsonObject = mIdentities;
                }
                break;
        }
        return jsonObject;
    }

    private JSONObject getDefaultIdentities() {
        JSONObject jsonObject = null;
        try {
            jsonObject = getCacheIdentities();
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return jsonObject;
    }

    /**
     * 避免多进场数据的不同步
     */
    public void updateIdentities() {
        try {
            mIdentities = getCacheIdentities();
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    public void mergeIdentities(JSONObject source) throws JSONException {
        Iterator<String> iteratorKeys = source.keys();
        // 遍历执行 H5 属性新增到原生侧
        while (iteratorKeys.hasNext()) {
            String key = iteratorKeys.next();
            if (!mIdentities.has(key)) {
                mIdentities.put(key, source.optString(key));
            }
        }
        saveIdentities();
    }

    private JSONObject getInitIdentities() throws JSONException {
        String cacheIdentities = Local.getIdentitiesFromLocal();
        if (cacheIdentities != null && !TextUtils.isEmpty(cacheIdentities)) {
            return new JSONObject(cacheIdentities);
        }
        return null;
    }

    private JSONObject getCacheIdentities() throws JSONException {
        String cacheIdentities = DbAdapter.getInstance().getIdentities();
        if (!TextUtils.isEmpty(cacheIdentities)) {
            return new JSONObject(cacheIdentities);
        }
        return null;
    }

    /**
     * 返回业务需要的拼接的 loginID=loginIDKey+loginID
     *
     * @return 返回业务拼接的 loginID
     */
    public String getJointLoginID() {
        return mLoginIDAndKey.getJointLoginID();
    }

    /**
     * 返回原始的存储的用户传递过来的 loginID
     *
     * @return 返回原始的存储的用户传递过来的 loginID
     */
    public String getLoginId() {
        return mLoginIDAndKey.getLoginId();
    }

    /**
     * 返回登录 IDKey
     *
     * @return 登录 LoginIDKey
     */
    public String getLoginIDKey() {
        return mLoginIDAndKey.getLoginIDKey();
    }


    /**
     * 获取数据，从当前进程读取，避免 ContentProvider 启动的时候耗时
     */
    public static class Local {
        /**
         * 获取 LoginId，从当前进程读取
         *
         * @return LoginId
         */
        public static String getLoginIdFromLocal() {
            try {
                PersistentLoginId persistentLoginId = PersistentLoader.getInstance().getLoginIdPst();
                return (persistentLoginId == null) ? "" : persistentLoginId.get();
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            return "";
        }

        /**
         * 获取 loginIDKey，从当前进程读取
         *
         * @return loginIDKey
         */
        public static String getLoginIdKeyFromLocal() {
            try {
                LoginIdKeyPersistent loginIdKeyPersistent = PersistentLoader.getInstance().getLoginIdKeyPst();
                return (loginIdKeyPersistent == null) ? "" : loginIdKeyPersistent.get();
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            return "";
        }

        /**
         * 获取 identities，从当前进程读取
         *
         * @return ID 标识
         */
        public static String getIdentitiesFromLocal() {
            try {
                UserIdentityPersistent userPersistent = PersistentLoader.getInstance().getUserIdsPst();
                if (userPersistent != null) {
                    return DbAdapter.decodeIdentities(userPersistent.get());
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            return null;
        }
    }
}