package com.sensorsdata.analytics.android.sdk.useridentity;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;

public class LoginIDAndKey {
    private static final String TAG = "SA.LoginIDAndKey";
    public static final String LOGIN_ID_KEY_DEFAULT = "$identity_login_id";

    public LoginIDAndKey() {
    }

    public void init(String loginIDKey) {
        //loginIDKey 合法性
        if (isInValidLoginIDKey(loginIDKey)) {
            restoreDefaultLoginIDKey();
        }
    }

    public String getLoginIDKey() {
        return DbAdapter.getInstance().getLoginIdKey();
    }

    /**
     * 获取原始保存的本地 loginID
     *
     * @return 返回本地存储的 loginID
     */
    public String getLoginId() {
        return DbAdapter.getInstance().getLoginId();
    }

    /**
     * 返回业务需要的拼接的 loginID=loginIDKey+loginID
     *
     * @return 返回业务拼接的 loginID
     */
    public String getJointLoginID() {
        String loginIDKey = getLoginIDKey();
        String loginID = getLoginId();
        return jointLoginID(loginIDKey, loginID);
    }

    public static String jointLoginID(String loginIDKey, String loginID) {
        if (!TextUtils.isEmpty(loginIDKey)) {
            if (loginIDKey.equals("$identity_login_id")) {
                return loginID;
            } else {
                return loginIDKey + "+" + loginID;
            }
        } else {
            return loginID;
        }
    }

    /**
     * 设置登录 IDKey 和 value
     *
     * @param loginIDKey 登录 IDKey
     * @param loginID 登录 value
     * @param anonymousId 匿名 ID
     * @return true 代表设置成功，false 代表设置失败
     */
    public boolean setLoginKeyAndID(String loginIDKey, String loginID, String anonymousId) {
        //1、登录 IDKey 和 loginID 校验
        if (isInValidLogin(loginIDKey, loginID, getLoginIDKey(), getLoginId(), anonymousId)) {
            return false;
        }
        //2、本地设置 loginIDKey 和 loginID
        setLoginIDKey(loginIDKey);
        setLoginId(loginID);
        return true;
    }

    public void removeLoginKeyAndID() {
        //参考登出操作
        setLoginIDKey("");
        setLoginId("");
    }

    /**
     * 恢复默认登录 IDKey
     */
    private void restoreDefaultLoginIDKey() {
        setLoginIDKey(LOGIN_ID_KEY_DEFAULT);
    }

    public void setLoginId(String loginID) {
        //loginID 为 "" 获 null 的时候清空
        DbAdapter.getInstance().commitLoginId(loginID);
    }


    public void setLoginIDKey(String loginIDKey) {
        //loginIDKey 为 "" 获 null 的时候清空
        DbAdapter.getInstance().commitLoginIdKey(loginIDKey);
    }


    private static boolean isInValidLoginIDKey(String loginIDKey) {
        //1、属性校验
        if (!SADataHelper.assertPropertyKey(loginIDKey)) {
            return true;
        }
        //2、不能为匿名 ID、AndroidID、登录ID
        if (Identities.ANONYMOUS_ID.equals(loginIDKey) || Identities.ANDROID_UUID.equals(loginIDKey) || Identities.ANDROID_ID.equals(loginIDKey)) {
            SALog.i(TAG, "login key cannot be an anonymous id or android_uuid or android_id!");
            return true;
        }
        return false;
    }

    /**
     * 登录 loginID 校验
     *
     * @param loginID 待校验 loginID
     * @param anonymousId 匿名 ID
     * @return 返回是否校验成功，false 校验失败，true 校验成功
     */
    private static boolean isInValidLoginID(String loginID, String anonymousId) {
        //1、属性 value 校验
        try {
            SADataHelper.assertDistinctId(loginID);
        } catch (Exception e) {
            SALog.i(TAG, e);
            return true;
        }
        //2、匿名ID
        if (loginID.equals(anonymousId)) {
            SALog.i(TAG, "login value cannot be an anonymous id!");
            return true;
        }
        return false;
    }


    public static boolean isInValidLogin(String loginIDKey, String loginID, String currentLoginIDKey, String currentLoginID, String anonymousId) {
        //1、loginIDKey、loginID 合法性校验
        if (isInValidLoginIDKey(loginIDKey)) {
            return true;
        }
        if (isInValidLoginID(loginID, anonymousId)) {
            return true;
        }
        //2、登录 IDKey 和 loginID 是否同本地相同
        if (loginIDKey.equals(currentLoginIDKey) && loginID.equals(currentLoginID)) {
            SALog.i(TAG, "login key and value already exist!");
            return true;
        }
        return false;
    }


}