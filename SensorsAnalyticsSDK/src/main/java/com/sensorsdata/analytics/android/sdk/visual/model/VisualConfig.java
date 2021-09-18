/*
 * Created by zhangxiangwei on 2021/01/28.
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

package com.sensorsdata.analytics.android.sdk.visual.model;

import java.util.List;


/**
 * 可视化配置
 */
public class VisualConfig {

    public String appId;
    public String os;
    public String project;
    public String version;
    public List<VisualPropertiesConfig> events;

    public static class VisualPropertiesConfig {
        public String eventName;
        public String eventType;
        public VisualEvent event;
        public List<VisualProperty> properties;

        @Override
        public String toString() {
            return "VisualPropertiesConfig{" +
                    "eventName='" + eventName + '\'' +
                    ", eventType='" + eventType + '\'' +
                    ", event=" + event +
                    ", properties=" + properties +
                    '}';
        }
    }

    public static class VisualEvent {
        public String elementPath; // 事件的元素路径
        public String elementPosition; // 事件的元素位置
        public String elementContent; // 事件的元素内容
        public String screenName; // 当前元素所在页面的 screen_name
        public boolean limitElementPosition; // 事件是否限定「元素位置」
        public boolean limitElementContent; // 事件是否限定「元素内容」
        public boolean isH5; // 是否为 H5 元素

        @Override
        public String toString() {
            return "VisualEvent{" +
                    "elementPath='" + elementPath + '\'' +
                    ", elementPosition='" + elementPosition + '\'' +
                    ", elementContent='" + elementContent + '\'' +
                    ", screenName='" + screenName + '\'' +
                    ", limitElementPosition=" + limitElementPosition +
                    ", limitElementContent=" + limitElementContent +
                    ", isH5=" + isH5 +
                    '}';
        }
    }

    public static class VisualProperty {
        public String elementPath; //属性的元素路径
        public String elementPosition; // 属性的元素位置
        public String screenName; // 属性所在的页面
        public String name; // 属性名
        public String regular; // 属性处理规则
        public String type; // 属性类型
        public boolean isH5; // 是否为 H5
        public String webViewElementPath; // webViewElementPath 用于区分 H5 在哪一个 webView

        @Override
        public String toString() {
            return "VisualProperty{" +
                    "elementPath='" + elementPath + '\'' +
                    ", elementPosition='" + elementPosition + '\'' +
                    ", screenName='" + screenName + '\'' +
                    ", name='" + name + '\'' +
                    ", regular='" + regular + '\'' +
                    ", type='" + type + '\'' +
                    ", isH5=" + isH5 +
                    ", webViewElementPath='" + webViewElementPath + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "VisualConfig{" +
                "appId='" + appId + '\'' +
                ", os='" + os + '\'' +
                ", project='" + project + '\'' +
                ", version='" + version + '\'' +
                ", events=" + events +
                '}';
    }
}
