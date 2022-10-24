package com.sensorsdata.analytics.android.sdk.core.business.exposure;

import org.json.JSONObject;

public class SAExposureData {
    private SAExposureConfig exposureConfig;  //曝光配置
    private JSONObject properties;  //自定义事件属性
    private String event;   //事件名称
    private final String exposureIdentifier;//view 的唯一标志

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
        this.properties = properties;
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
        this.properties = properties;
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
