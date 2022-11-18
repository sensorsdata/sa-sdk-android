package com.sensorsdata.analytics.android.sdk.visual.model;

import java.io.Serializable;
import java.util.List;

public class CommonNode implements Serializable {
    private static final long serialVersionUID = -5865016149609340219L;
    private String id;
    private String $element_content;
    private String $element_path;
    private String $element_position;
    private String lib_version;
    private boolean enable_click;
    private boolean is_list_view;
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
    private int level;
    private boolean isRootView;
    private List<String> subelements;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
