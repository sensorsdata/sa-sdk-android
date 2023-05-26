package com.sensorsdata.analytics.android.sdk.core.business.exposure;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;

import org.json.JSONObject;

public class SAExposureData {
    private SAExposureConfig exposureConfig;  //曝光配置
    private JSONObject properties;  //自定义事件属性
    private String event;   //事件名称
    private final String exposureIdentifier;//view 的唯一标志
    private SAExposureListener exposureListener; // 曝光监听回调

    public SAExposureData(String event) {
        this(event, null, null, null);
    }

    public SAExposureData(String event, JSONObject properties) {
        this(event, properties, null, null);
    }

    public SAExposureData(String event, String exposureIdentifier) {
        this(event, null, exposureIdentifier, null);
    }

    public SAExposureData(String event, JSONObject properties, String exposureIdentifier) {
        this(event, properties, exposureIdentifier, null);
    }

    public SAExposureData(String event, JSONObject properties, SAExposureConfig exposureConfig) {
        this(event, properties, null, exposureConfig);
    }
    
    public SAExposureData(String event, JSONObject properties, String exposureIdentifier, SAExposureConfig exposureConfig) {
        this.event = event;
        try {
            this.properties = JSONUtils.cloneJsonObject(properties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        this.exposureIdentifier = exposureIdentifier;
        this.exposureConfig = exposureConfig;
    }

    public SAExposureConfig getExposureConfig() {
        return exposureConfig;
    }

    public void setExposureConfig(SAExposureConfig exposureConfig) {
        this.exposureConfig = exposureConfig;
    }

    public JSONObject getProperties() {
        return properties;
    }

    public void setProperties(JSONObject properties) {
        try {
            this.properties = JSONUtils.cloneJsonObject(properties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getIdentifier() {
        return exposureIdentifier;
    }

    public SAExposureListener getExposureListener() {
        return exposureListener;
    }

    public void setExposureListener(SAExposureListener saExposureListener) {
        this.exposureListener = saExposureListener;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SAExposureData that = (SAExposureData) o;
        return exposureConfig.equals(that.exposureConfig) && properties.toString().equals(that.properties.toString()) && event.equals(that.event) && exposureIdentifier.equals(that.exposureIdentifier);
    }

    @Override
    public String toString() {
        return "SAExposureData{" +
                "exposureConfig=" + exposureConfig +
                ", properties=" + properties +
                ", event='" + event + '\'' +
                ", exposureIdentifier='" + exposureIdentifier + '\'' +
                '}';
    }
}
