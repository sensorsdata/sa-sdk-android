package com.sensorsdata.analytics.android.sdk.core.mediator.exposure;

import android.view.View;

import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureData;

import org.json.JSONObject;

public interface SAExposureAPIProtocol {

    /**
     * 设置曝光 view 唯一标记位，一般只在列表复用的情况下使用
     *
     * @param view 被标记的 view
     * @param exposureIdentifier 被标记 view 的唯一标记位
     */
    void setExposureIdentifier(View view, String exposureIdentifier);

    /**
     * 曝光 view 标记
     *
     * @param view 被标记的 view
     * @param exposureData 曝光配置
     */
    void addExposureView(View view, SAExposureData exposureData);

    /**
     * 曝光 view 标记取消
     *
     * @param view 被标记的 view
     * @param identifier 被标记的 view 的唯一标识
     */
    void removeExposureView(View view, String identifier);

    /**
     * 曝光 view 标记取消
     *
     * @param view 被标记的 view
     */
    void removeExposureView(View view);

    /**
     * 更新曝光 View 绑定的属性
     *
     * @param view View
     * @param properties 属性
     */
    void updateExposureProperties(View view, JSONObject properties);
}
