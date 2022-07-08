package com.sensorsdata.analytics.android.sdk.visual;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;

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
        builder.setTitle("提示");
        if (isWifi) {
            builder.setMessage("正在连接 App 点击分析...");
        } else {
            builder.setMessage("正在连接 App 点击分析，建议在 WiFi 环境下使用。");
        }
        builder.setCancelable(false);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SensorsDataDialogUtils.startLaunchActivity(context);
            }
        });
        builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
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
        builder.setTitle("提示");
        if (isWifi) {
            builder.setMessage("正在连接 App 可视化全埋点...");
        } else {
            builder.setMessage("正在连接 App 可视化全埋点，建议在 WiFi 环境下使用。");
        }
        builder.setCancelable(false);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SensorsDataDialogUtils.startLaunchActivity(context);
            }
        });
        builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
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
