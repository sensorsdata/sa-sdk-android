/*
 * Created by zhangxiangwei on 2019/12/31.
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

package com.sensorsdata.analytics.android.sdk.visual.model;

public class WebNode extends CommonNode {
    private static final long serialVersionUID = -5865016149609340219L;
    private String tagName;
    private String $element_selector;
    private String $url;
    private int zIndex;
    private String list_selector;
    private String $title;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String get$element_selector() {
        return $element_selector;
    }

    public void set$element_selector(String $element_selector) {
        this.$element_selector = $element_selector;
    }

    public void set$url(String $url) {
        this.$url = $url;
    }

    public String get$url() {
        return $url;
    }

    public int getzIndex() {
        return zIndex;
    }

    public void setzIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public String getList_selector() {
        return list_selector;
    }

    public void setList_selector(String list_selector) {
        this.list_selector = list_selector;
    }

    public String get$title() {
        return $title;
    }

    public void set$title(String $title) {
        this.$title = $title;
    }

}