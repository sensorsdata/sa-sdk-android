/*
 * Created by chenru on 2022/4/7 下午5:13.
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

package com.sensorsdata.analytics.android.sdk.core.eventbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SAEventBus {
    private static SAEventBus sSAEventBus;
    private final Map<String, CopyOnWriteArrayList<Subscription>> mSubscriberMap = new ConcurrentHashMap<>();


    public static SAEventBus getInstance() {
        if (sSAEventBus == null) {
            synchronized (SAEventBus.class) {
                if (sSAEventBus == null) {
                    sSAEventBus = new SAEventBus();
                }
            }
        }
        return sSAEventBus;
    }

    /**
     * 注册订阅
     * @param tag  tag {@link SAEventBusConstants.Tag}
     * @param subscription 订阅者
     */
    public void register(String tag, Subscription subscription) {
        if (subscription == null) {
            return;
        }
        subscription.eventTag = tag;
        if (mSubscriberMap.containsKey(tag)) {
            mSubscriberMap.get(tag).add(subscription);
        } else {
            CopyOnWriteArrayList<Subscription> list = new CopyOnWriteArrayList<>();
            list.add(subscription);
            mSubscriberMap.put(tag, list);
        }
    }

    /**
     * 清除所有订阅
     */
    public void clear() {
        mSubscriberMap.clear();
    }

    /**
     * 取消订阅
     * @param subscription 订阅对象
     */
    public void unRegister(Subscription subscription) {
        if (subscription == null) {
            return;
        }
        if (mSubscriberMap.containsKey(subscription.eventTag)) {
            mSubscriberMap.get(subscription.eventTag).remove(subscription);
        }
    }

    /**
     * 给观察者推送变化消息
     * @param eventBusTag 观察 tag
     * @param result 变化对象
     */
    public void post(String eventBusTag, Object result) {
        if (mSubscriberMap.containsKey(eventBusTag)) {
            CopyOnWriteArrayList<Subscription> list = mSubscriberMap.get(eventBusTag);
            for (Subscription subscription : list) {
                if (checkType(subscription, result)) {
                    subscription.notify(result);
                }
            }
        }
    }

    /**
     * 检测监听类型是否一致
     * @param subscription 观察者
     * @param result 变化对象
     * @return 类型是否一致
     */
    private boolean checkType(Subscription subscription, Object result) {
        try {
            subscription.getClass().getDeclaredMethod("notify", result.getClass());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}