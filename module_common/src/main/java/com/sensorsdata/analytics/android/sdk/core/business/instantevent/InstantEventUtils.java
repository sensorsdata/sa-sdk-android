package com.sensorsdata.analytics.android.sdk.core.business.instantevent;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;

import org.json.JSONObject;

import java.util.List;

public class InstantEventUtils {

    public static boolean isInstantEvent(InputData inputData) {
        try {
            if (inputData == null) {
                return false;
            }
            if (TextUtils.isEmpty(inputData.getExtras())) {
                //App 原生实时数据配置
                List<String> listInstantEvents = SensorsDataAPI.getConfigOptions().getInstantEvents();
                //原生
                if (inputData.getEventType() == null) {
                    return false;
                }
                if (inputData.getEventType().isTrack() && !TextUtils.isEmpty(inputData.getEventName()) && (listInstantEvents != null) && listInstantEvents.contains(inputData.getEventName())) {
                    return true;
                }
            } else {
                //H5 实时数据发送
                JSONObject jsonObject = new JSONObject(inputData.getExtras());
                String type = jsonObject.optString("type", "");
                boolean is_instant_event = jsonObject.optBoolean("is_instant_event", false);
                if (instanceEventType(type) && is_instant_event) {
                    return true;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 获取实时数据，并且删除 H5 过来的 is_instant_event 字段
     *
     * @param jsonObject 待判断的 App 或者打通场景 H5 过来的数据
     * @return 返回实时数据的值
     */
    public static int isInstantEvent(JSONObject jsonObject) {
        int instant_event = 0;
        try {
            boolean _hybrid_h5 = jsonObject.optBoolean("_hybrid_h5", false);
            if (_hybrid_h5) {
                boolean is_instant_event = jsonObject.optBoolean("is_instant_event", false);
                String type = jsonObject.optString("type", "");
                jsonObject.remove("is_instant_event");
                if (instanceEventType(type) && is_instant_event) {
                    instant_event = 1;
                }
            } else {
                String type = jsonObject.optString("type", "");
                String event = jsonObject.optString("event", "");
                List<String> listInstantEvents = SensorsDataAPI.getConfigOptions().getInstantEvents();
                if (instanceEventType(type) && !TextUtils.isEmpty(event) && (listInstantEvents != null) && listInstantEvents.contains(event)) {
                    instant_event = 1;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return instant_event;
    }

    /**
     * 实时数据类型判断
     *
     * @param type 数据类型
     * @return 是否是实时数据发送类型
     */
    private static boolean instanceEventType(String type) {
        if (TextUtils.isEmpty(type)) {
            return false;
        }
        if (type.equals(EventType.TRACK.getEventType()) || type.equals(EventType.TRACK_SIGNUP.getEventType()) || type.equals(EventType.TRACK_ID_BIND.getEventType()) || type.equals(EventType.TRACK_ID_UNBIND.getEventType())) {
            return true;
        }
        return false;
    }
}
