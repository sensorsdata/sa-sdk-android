/*
 * Created by dengshiwei on 2020/11/26.
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

import java.util.List;

public interface IFragmentAPI {
    /**
     * 是否开启自动追踪 Fragment 的 $AppViewScreen 事件
     * 默认不开启
     */
    void trackFragmentAppViewScreen();

    /**
     * 是否开启 Fragment 页面浏览
     *
     * @return true：开启，false：关闭
     */
    boolean isTrackFragmentAppViewScreenEnabled();

    /**
     * 指定 fragment 被 AutoTrack 采集
     *
     * @param fragment Fragment
     */
    void enableAutoTrackFragment(Class<?> fragment);

    /**
     * 指定 fragments 被 AutoTrack 采集
     *
     * @param fragmentsList Fragment 集合
     */
    void enableAutoTrackFragments(List<Class<?>> fragmentsList);

    /**
     * 判断 AutoTrack 时，某个 Fragment 的 $AppViewScreen 是否被采集
     *
     * @param fragment Fragment
     * @return Fragment 是否被采集
     */
    boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment);

    /**
     * 指定哪些 Fragment 不被 AutoTrack
     * 指定 Fragment 的格式为：Fragment.getClass().getCanonicalName()
     *
     * @param fragmentList Fragment 列表
     */
    void ignoreAutoTrackFragments(List<Class<?>> fragmentList);

    /**
     * 指定某个 Fragment 不被 AutoTrack
     *
     * @param fragment Fragment
     */
    void ignoreAutoTrackFragment(Class<?> fragment);

    /**
     * 恢复不被 AutoTrack 的 Fragment
     *
     * @param fragmentList List
     */
    void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList);

    /**
     * 恢复不被 AutoTrack 的 Fragment
     *
     * @param fragment Class
     */
    void resumeIgnoredAutoTrackFragment(Class<?> fragment);
}
