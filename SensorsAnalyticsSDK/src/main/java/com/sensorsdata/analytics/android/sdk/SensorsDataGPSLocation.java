package com.sensorsdata.analytics.android.sdk;

public class SensorsDataGPSLocation {
    /**
     * 纬度
     */
    private long latitude;

    /**
     * 经度
     */
    private long longitude;

    public long getLatitude() {
        return latitude;
    }

    public void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    public long getLongitude() {
        return longitude;
    }

    public void setLongitude(long longitude) {
        this.longitude = longitude;
    }
}
