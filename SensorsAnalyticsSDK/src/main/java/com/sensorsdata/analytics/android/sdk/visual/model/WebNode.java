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
    private String $title;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebNode)) return false;

        WebNode webNode = (WebNode) o;

        if (Float.compare(webNode.getTop(), getTop()) != 0) return false;
        if (Float.compare(webNode.getLeft(), getLeft()) != 0) return false;
        if (Float.compare(webNode.getWidth(), getWidth()) != 0) return false;
        if (Float.compare(webNode.getHeight(), getHeight()) != 0) return false;
        if (isVisibility() != webNode.isVisibility()) return false;
        if (Float.compare(webNode.getScrollX(), getScrollX()) != 0) return false;
        if (Float.compare(webNode.getScrollY(), getScrollY()) != 0) return false;
        if (Float.compare(webNode.getScale(), getScale()) != 0) return false;
        if (getzIndex() != webNode.getzIndex()) return false;
        if (!getId().equals(webNode.getId())) return false;
        if (getTagName() != null ? !getTagName().equals(webNode.getTagName()) : webNode.getTagName() != null)
            return false;
        if (get$element_selector() != null ? !get$element_selector().equals(webNode.get$element_selector()) : webNode.get$element_selector() != null)
            return false;
        if (get$element_content() != null ? !get$element_content().equals(webNode.get$element_content()) : webNode.get$element_content() != null)
            return false;
        if (get$title() != null ? !get$title().equals(webNode.get$title()) : webNode.get$title() != null)
            return false;
        if (get$url() != null ? !get$url().equals(webNode.get$url()) : webNode.get$url() != null)
            return false;
        return getSubelements() != null ? getSubelements().equals(webNode.getSubelements()) : webNode.getSubelements() == null;
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + (getTagName() != null ? getTagName().hashCode() : 0);
        result = 31 * result + (get$element_selector() != null ? get$element_selector().hashCode() : 0);
        result = 31 * result + (get$element_content() != null ? get$element_content().hashCode() : 0);
        result = 31 * result + (get$title() != null ? get$title().hashCode() : 0);
        result = 31 * result + (getTop() != +0.0f ? Float.floatToIntBits(getTop()) : 0);
        result = 31 * result + (getLeft() != +0.0f ? Float.floatToIntBits(getLeft()) : 0);
        result = 31 * result + (getWidth() != +0.0f ? Float.floatToIntBits(getWidth()) : 0);
        result = 31 * result + (getHeight() != +0.0f ? Float.floatToIntBits(getHeight()) : 0);
        result = 31 * result + (isVisibility() ? 1 : 0);
        result = 31 * result + (getScrollX() != +0.0f ? Float.floatToIntBits(getScrollX()) : 0);
        result = 31 * result + (getScrollY() != +0.0f ? Float.floatToIntBits(getScrollY()) : 0);
        result = 31 * result + (getScale() != +0.0f ? Float.floatToIntBits(getScale()) : 0);
        result = 31 * result + (get$url() != null ? get$url().hashCode() : 0);
        result = 31 * result + getzIndex();
        result = 31 * result + (getSubelements() != null ? getSubelements().hashCode() : 0);
        return result;
    }
}