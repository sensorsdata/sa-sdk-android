/*
 * Created by zhangxiangwei on 2019/12/31.
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

import java.io.Serializable;
import java.util.List;

public class WebNode implements Serializable {
    private static final long serialVersionUID = -5865016149609340219L;
    private String id;
    private String tagName;
    private String $element_selector;
    private String $element_content;
    private String $element_path;
    private String $element_position;
    private String list_selector;
    private String lib_version;
    private boolean enable_click;
    private boolean is_list_view;
    private String $title;
    private float originTop;
    private float originLeft;
    private float top;
    private float left;
    private float width;
    private float height;
    private boolean visibility;
    private float scrollX;
    private float scrollY;
    private float scale;
    private String $url;
    private int zIndex;
    private int level;
    private boolean isRootView;
    private List<String> subelements;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String get$element_content() {
        return $element_content;
    }

    public void set$element_content(String $element_content) {
        this.$element_content = $element_content;
    }

    public String get$element_path() {
        return $element_path;
    }

    public void set$element_path(String $element_path) {
        this.$element_path = $element_path;
    }

    public String get$element_position() {
        return $element_position;
    }

    public void set$element_position(String $element_position) {
        this.$element_position = $element_position;
    }

    public String getList_selector() {
        return list_selector;
    }

    public void setList_selector(String list_selector) {
        this.list_selector = list_selector;
    }

    public String getLib_version() {
        return lib_version;
    }

    public void setLib_version(String lib_version) {
        this.lib_version = lib_version;
    }

    public boolean isEnable_click() {
        return enable_click;
    }

    public void setEnable_click(boolean enable_click) {
        this.enable_click = enable_click;
    }

    public boolean isIs_list_view() {
        return is_list_view;
    }

    public void setIs_list_view(boolean is_list_view) {
        this.is_list_view = is_list_view;
    }

    public String get$title() {
        return $title;
    }

    public void set$title(String $title) {
        this.$title = $title;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public float getScrollX() {
        return scrollX;
    }

    public void setScrollX(float scrollX) {
        this.scrollX = scrollX;
    }

    public float getScrollY() {
        return scrollY;
    }

    public void setScrollY(float scrollY) {
        this.scrollY = scrollY;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public String get$url() {
        return $url;
    }

    public void set$url(String $url) {
        this.$url = $url;
    }

    public int getzIndex() {
        return zIndex;
    }

    public void setzIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public List<String> getSubelements() {
        return subelements;
    }

    public void setSubelements(List<String> subelements) {
        this.subelements = subelements;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isRootView() {
        return isRootView;
    }

    public void setRootView(boolean rootView) {
        isRootView = rootView;
    }

    public float getOriginTop() {
        return originTop;
    }

    public void setOriginTop(float originTop) {
        this.originTop = originTop;
    }

    public float getOriginLeft() {
        return originLeft;
    }

    public void setOriginLeft(float originLeft) {
        this.originLeft = originLeft;
    }
}