package com.sensorsdata.analytics.android.sdk.core.business.exposure;

public class SAExposureConfig {
    private float areaRate = 0.0f;//曝光比例,默认值 0.0,范围是 0~1
    private boolean repeated = true;//重复曝光,默认值是 true
    private double stayDuration = 0;//有效停留时长,默认值是 0.0,时长单位是秒
    private long delayTime = 500;//默认页面改变时间

    public SAExposureConfig(float areaRate, double stayDuration, boolean repeated) {
        this.areaRate = areaRate;
        this.stayDuration = stayDuration;
        this.repeated = repeated;
    }

    public float getAreaRate() {
        return areaRate;
    }

    public void setAreaRate(float areaRate) {
        this.areaRate = areaRate;
    }

    public boolean isRepeated() {
        return repeated;
    }

    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }

    public double getStayDuration() {
        return stayDuration;
    }

    public void setStayDuration(double stayDuration) {
        this.stayDuration = stayDuration;
    }

    public long getDelayTime() {
        return delayTime;
    }

    /**
     * 全局曝光配置设置生效，页面改变时间监听，单页面不生效
     *
     * @param delayTime 页面改变时间，单位毫秒(ms)
     */
    public void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SAExposureConfig that = (SAExposureConfig) o;
        return (that.areaRate == areaRate) && (repeated == that.repeated) && (that.stayDuration == stayDuration);
    }

    @Override
    public String toString() {
        return "SAExposureConfig{" +
                "areaRate=" + areaRate +
                ", repeated=" + repeated +
                ", stayDuration=" + stayDuration +
                '}';
    }
}
