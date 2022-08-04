package com.sensorsdata.analytics.android.sdk.visual;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

public class VisualDialog {

    public static void showOpenHeatMapDialog(final Activity context, final String featureCode, final String postUrl) {
        boolean isWifi = false;
        try {
            String networkType = NetworkUtils.networkType(context);
            if ("WIFI".equals(networkType)) {
                isWifi = true;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_common_title));
        if (isWifi) {
            builder.setMessage(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_heatmap_wifi_name));
        } else {
            builder.setMessage(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_heatmap_mobile_name));
        }
        builder.setCancelable(false);
        builder.setNegativeButton(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_common_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SensorsDataDialogUtils.startLaunchActivity(context);
            }
        });
        builder.setPositiveButton(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_common_continue), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HeatMapService.getInstance().start(context, featureCode, postUrl);
                SensorsDataDialogUtils.startLaunchActivity(context);
            }
        });
        AlertDialog dialog = builder.create();
        SensorsDataDialogUtils.dialogShowDismissOld(dialog);
        try {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackground(SensorsDataDialogUtils.getDrawable());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackground(SensorsDataDialogUtils.getDrawable());
            } else {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundDrawable(SensorsDataDialogUtils.getDrawable());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundDrawable(SensorsDataDialogUtils.getDrawable());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void showOpenVisualizedAutoTrackDialog(final Activity context, final String featureCode, final String postUrl) {
        boolean isWifi = false;
        try {
            String networkType = NetworkUtils.networkType(context);
            if ("WIFI".equals(networkType)) {
                isWifi = true;
            }
        } catch (Exception e) {
            // ignore
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_common_title));
        if (isWifi) {
            builder.setMessage(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_visual_wifi_name));
        } else {
            builder.setMessage(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_visual_mobile_name));
        }
        builder.setCancelable(false);
        builder.setNegativeButton(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_common_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SensorsDataDialogUtils.startLaunchActivity(context);
            }
        });
        builder.setPositiveButton(SADisplayUtil.getStringResource(context, com.sensorsdata.analytics.android.sdk.R.string.sensors_analytics_common_continue), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                VisualizedAutoTrackService.getInstance().start(context, featureCode, postUrl);
                SensorsDataDialogUtils.startLaunchActivity(context);
            }
        });
        AlertDialog dialog = builder.create();
        SensorsDataDialogUtils.dialogShowDismissOld(dialog);
        try {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackground(SensorsDataDialogUtils.getDrawable());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackground(SensorsDataDialogUtils.getDrawable());
            } else {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundDrawable(SensorsDataDialogUtils.getDrawable());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundDrawable(SensorsDataDialogUtils.getDrawable());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
