package com.sensorsdata.analytics.android.sdk.visual.utils;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

public class FlutterUtils {
    private static final String FLUTTER_ACTION = "android.intent.action.FLUTTER_VISUALIZED";
    private static final String FLUTTER_EXTRA = "visualizedChanged";

    public static void visualizedConnectionStatusChanged() {
        sendFlutterBroadcast("visualizedConnectionStatusChanged");
    }

    public static void visualizedPropertiesConfigChanged() {
        sendFlutterBroadcast("visualizedPropertiesConfigChanged");
    }

    private static void sendFlutterBroadcast(String name) {
        Context context = SensorsDataAPI.sharedInstance().getSAContextManager().getContext();
        if (context != null && !TextUtils.isEmpty(name)) {
            Intent intent = new Intent();
            intent.setAction(FLUTTER_ACTION);
            intent.putExtra(FLUTTER_EXTRA, name);
            context.sendBroadcast(intent);
        }
    }
}
