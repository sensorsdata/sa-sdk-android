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

import android.content.Context;

import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;

import org.json.JSONObject;

/**
 * 用户标识接口
 */
public interface IUserIdentityAPI {
    /**
     * 绑定事件名称
     */
    String BIND_ID = "$BindID";
    /**
     * 解绑事件名称
     */
    String UNBIND_ID = "$UnbindID";

    /**
     * 获取当前用户的 distinctId
     *
     * @return 优先返回登录 ID，登录 ID 为空时，返回匿名 ID
     */
    String getDistinctId();

    /**
     * 获取当前用户的匿名 ID
     * 若调用前未调用 {@link #identify(String)} 设置用户的匿名 ID，SDK 会优先调用 {@link com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils#getIdentifier(Context)}获取 Android ID，
     * 如获取的 Android ID 非法，则调用 {@link java.util.UUID} 随机生成 UUID，作为用户的匿名 ID
     *
     * @return 当前用户的匿名 ID
     */
    @Deprecated
    String getAnonymousId();

    /**
     * 重置默认匿名id
     */
    @Deprecated
    void resetAnonymousId();

    /**
     * 获取当前用户的 loginId
     * 若调用前未调用 {@link #login(String)} 设置用户的 loginId，会返回 null
     *
     * @return 当前用户的 loginId
     */
    String getLoginId();

    /**
     * 设置当前用户的 distinctId。一般情况下，如果是一个注册用户，则应该使用注册系统内
     * 的 user_id，如果是个未注册用户，则可以选择一个不会重复的匿名 ID，如设备 ID 等，如果
     * 客户没有调用 identify，则使用SDK自动生成的匿名 ID
     *
     * @param distinctId 当前用户的 distinctId，仅接受数字、下划线和大小写字母
     */
    @Deprecated
    void identify(String distinctId);

    /**
     * 获取当前的 identities
     *
     * @return identities
     */
    JSONObject getIdentities();

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于 255
     */
    void login(String loginId);

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于 255
     * @param properties 用户登录属性
     */
    void login(final String loginId, final JSONObject properties);

    /**
     * 登录，设置当前用户的登录 IDKey 和 loginId
     *
     * @param loginIDKey 登录 IDKey
     * @param loginID 登录 loginId
     */
    void loginWithKey(String loginIDKey, String loginID);

    /**
     * 登录，设置当前用户的登录 IDKey 和 loginId
     *
     * @param loginIDKey loginIDKey 登录 IDKey
     * @param loginID loginID 登录 loginId
     * @param properties properties 用户登录属性
     */
    void loginWithKey(String loginIDKey, String loginID, JSONObject properties);

    /**
     * 注销，清空当前用户的 loginId
     */
    void logout();

    /**
     * 绑定业务 ID
     *
     * @param key ID
     * @param value 值
     * @throws InvalidDataException 数据不合法
     */
    void bind(String key, String value) throws InvalidDataException;

    /**
     * 解绑业务 ID
     *
     * @param key ID
     * @param value 值
     * @throws InvalidDataException 数据不合法
     */
    void unbind(String key, String value) throws InvalidDataException;
}
