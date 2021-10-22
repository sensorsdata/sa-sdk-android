/*
 * Created by chenru on 2020/09/09.
 * Copyright 2015－2021 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.dialog;

import static com.sensorsdata.analytics.android.sdk.util.Base64Coder.CHARSET_UTF8;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAutoTrackHelper;
import com.sensorsdata.analytics.android.sdk.ThreadNameConstants;
import com.sensorsdata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.advert.utils.OaidHelper;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.visual.HeatMapService;
import com.sensorsdata.analytics.android.sdk.visual.VisualizedAutoTrackService;
import com.sensorsdata.analytics.android.sdk.visual.view.PairingCodeEditDialog;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class SensorsDataDialogUtils {
    private static final String TAG = "SA.SensorsDataDialogUtils";
    private static Dialog sDialog;

    public static void showDialog(Activity activity, String title, String content,
                                  final String positiveLabel, final DialogInterface.OnClickListener positiveOnClickListener,
                                  final String negativeLabel, final DialogInterface.OnClickListener negativeOnClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }

        if (!TextUtils.isEmpty(content)) {
            builder.setMessage(content);
        }

        builder.setCancelable(false);
        builder.setNegativeButton(negativeLabel, negativeOnClickListener);
        builder.setPositiveButton(positiveLabel, positiveOnClickListener);
        AlertDialog dialog = builder.create();
        dialogShowDismissOld(dialog);
    }

    public static void showChannelDebugDialog(final Activity activity,
                                              final String baseUrl,
                                              final String monitorId,
                                              final String projectId,
                                              final String accountId) {
        SensorsDataDialogUtils.showDialog(activity, "即将开启联调模式", "", "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                Context context = activity.getApplicationContext();
                boolean isTrackInstallation = ChannelUtils.isTrackInstallation(context);
                if (!isTrackInstallation || ChannelUtils.isCorrectTrackInstallation(context)) {
                    String androidId = SensorsDataUtils.getAndroidID(context);
                    String oaid = OaidHelper.getOAID(context);
                    if (isTrackInstallation && !ChannelUtils.isGetDeviceInfo(context, androidId, oaid)) {
                        showChannelDebugErrorDialog(activity);
                        return;
                    }
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        showDialog(activity, "当前网络不可用，请检查网络！");
                        return;
                    }
                    String deviceCode = ChannelUtils.getDeviceInfo(activity, androidId, oaid);
                    final SensorsDataLoadingDialog loadingDialog = new SensorsDataLoadingDialog(activity);
                    dialogShowDismissOld(loadingDialog);
                    requestActiveChannel(baseUrl,
                            monitorId, projectId, accountId,
                            deviceCode, isTrackInstallation,
                            new HttpCallback.JsonCallback() {
                                @Override
                                public void onFailure(int code, String errorMessage) {
                                    loadingDialog.dismiss();
                                    SALog.i(TAG, "ChannelDebug request error:" + errorMessage);
                                    showDialog(activity, "网络异常,请求失败!");
                                }

                                @Override
                                public void onResponse(JSONObject response) {
                                    loadingDialog.dismiss();
                                    if (response == null) {
                                        SALog.i(TAG, "ChannelDebug response error msg: response is null");
                                        showDialog(activity, "添加白名单请求失败，请联系神策技术支持人员排查问题!");
                                        return;
                                    }
                                    int code = response.optInt("code", 0);
                                    if (code == 1) {// 请求成功
                                        SensorsDataAutoTrackHelper.showChannelDebugActiveDialog(activity);
                                    } else {//请求失败
                                        SALog.i(TAG, "ChannelDebug response error msg:" + response.optString("message"));
                                        showDialog(activity, "添加白名单请求失败，请联系神策技术支持人员排查问题!");
                                    }
                                }
                            });
                } else {
                    showChannelDebugErrorDialog(activity);
                }
            }
        }, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startLaunchActivity(activity);
            }
        });
    }


    private static void showChannelDebugErrorDialog(final Activity activity) {
        SensorsDataDialogUtils.showDialog(activity, "检测到 “设备码为空”，可能原因如下，请排查：",
                "1. 开启 App 时拒绝“电话”授权；\n" +
                        "2. 手机系统权限设置中是否关闭“电话”授权；\n" +
                        "3. 请联系研发人员确认是否“调用 trackInstallation 接口在获取“电话”授权之后。\n\n " +
                        "排查修复后，请先卸载应用并重新安装，再扫码进行联调。", "确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SensorsDataDialogUtils.startLaunchActivity(activity);
                    }
                }, null, null);
    }

    public static void showPopupWindowDialog(final Activity activity, Uri uri) {
        try {
            Class<?> clazz = Class.forName("com.sensorsdata.sf.ui.utils.PreviewUtil");
            String sfPopupTest = uri.getQueryParameter("sf_popup_test");
            String popupWindowId = uri.getQueryParameter("popup_window_id");
            boolean isSfPopupTest = false;
            if (!TextUtils.isEmpty(sfPopupTest)) {
                isSfPopupTest = Boolean.parseBoolean(sfPopupTest);
            }
            Method[] methods = clazz.getDeclaredMethods();
            Method currentMethod = null;
            Runnable failCallback = null;
            for (Method method : methods) {
                if (method.getName().equals("showPreview")) {
                    currentMethod = method;
                    if (method.getParameterTypes().length == 4) {
                        failCallback = new Runnable() {
                            @Override
                            public void run() {
                                if (activity == null) return;
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showDialog(activity, "测试弹窗加载失败，请确认网络或项目环境是否正常！");
                                    }
                                });
                            }
                        };
                        break;
                    }
                }
            }
            if (currentMethod != null) {
                if (failCallback != null) {
                    currentMethod.invoke(null, activity, isSfPopupTest, popupWindowId, failCallback);
                } else {
                    currentMethod.invoke(null, activity, isSfPopupTest, popupWindowId);
                }
                SchemeActivity.isPopWindow = true;
            } else {
                //没有找到弹窗方法的时候，打开应用
                startLaunchActivity(activity);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
            //异常可能是没有集成 SF SDK，此时需要打开应用
            startLaunchActivity(activity);
        }
    }

    public static void showDebugModeSelectDialog(final Activity activity, final String infoId, final String locationHref, final String project) {
        try {
            DebugModeSelectDialog dialog = new DebugModeSelectDialog(activity, SensorsDataAPI.sharedInstance().getDebugMode());
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnDebugModeDialogClickListener(new DebugModeSelectDialog.OnDebugModeViewClickListener() {
                @Override
                public void onCancel(Dialog dialog) {
                    dialog.cancel();
                }

                @Override
                public void setDebugMode(Dialog dialog, SensorsDataAPI.DebugMode debugMode) {
                    SensorsDataAPI.sharedInstance().setDebugMode(debugMode);
                    dialog.cancel();
                }
            });
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //如果当前的调试模式不是 DebugOff ,则发送匿名或登录 ID 给服务端
                    String serverUrl = SensorsDataAPI.sharedInstance().getServerUrl();
                    SensorsDataAPI.DebugMode mCurrentDebugMode = SensorsDataAPI.sharedInstance().getDebugMode();
                    if (SensorsDataAPI.sharedInstance().isNetworkRequestEnable() && !TextUtils.isEmpty(serverUrl) && !TextUtils.isEmpty(infoId) && mCurrentDebugMode != SensorsDataAPI.DebugMode.DEBUG_OFF) {
                        if (TextUtils.isEmpty(locationHref)) {
                            new SendDebugIdThread(serverUrl, SensorsDataAPI.sharedInstance().getDistinctId(), infoId, ThreadNameConstants.THREAD_SEND_DISTINCT_ID).start();
                        } else {
                            try {
                                if (!TextUtils.isEmpty(project)) {
                                    String url = locationHref + "?project=" + project;
                                    SALog.i(TAG, "sf url:" + url);
                                    new SendDebugIdThread(url, SensorsDataAPI.sharedInstance().getDistinctId(), infoId, ThreadNameConstants.THREAD_SEND_DISTINCT_ID).start();
                                }
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }
                        }
                    }
                    String currentDebugToastMsg = "";
                    if (mCurrentDebugMode == SensorsDataAPI.DebugMode.DEBUG_OFF) {
                        currentDebugToastMsg = "已关闭调试模式，请重新扫描二维码进行开启";
                    } else if (mCurrentDebugMode == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
                        currentDebugToastMsg = "开启调试模式，校验数据，但不进行数据导入；关闭 App 进程后，将自动关闭调试模式";
                    } else if (mCurrentDebugMode == SensorsDataAPI.DebugMode.DEBUG_AND_TRACK) {
                        currentDebugToastMsg = "开启调试模式，校验数据，并将数据导入到神策分析中；关闭 App 进程后，将自动关闭调试模式";
                    }
                    Toast.makeText(activity, currentDebugToastMsg, Toast.LENGTH_LONG).show();
                    SALog.info(TAG, "您当前的调试模式是：" + mCurrentDebugMode, null);
                    startLaunchActivity(activity);
                }
            });
            dialogShowDismissOld(dialog);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void showOpenHeatMapDialog(final Activity context, final String featureCode,
                                             final String postUrl) {
        try {
            if (!SensorsDataAPI.sharedInstance().isNetworkRequestEnable()) {
                showDialog(context, "已关闭网络请求（NetworkRequest），无法使用 App 点击分析，请开启后再试！");
                return;
            }

            if (!SensorsDataAPI.sharedInstance().isHeatMapEnabled()) {
                showDialog(context, "SDK 没有被正确集成，请联系贵方技术人员开启点击分析。");
                return;
            }

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
                    startLaunchActivity(context);
                }
            });
            builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    HeatMapService.getInstance().start(context, featureCode, postUrl);
                    startLaunchActivity(context);
                }
            });
            AlertDialog dialog = builder.create();
            dialogShowDismissOld(dialog);
            try {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackground(getDrawable());
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackground(getDrawable());
                } else {
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundDrawable(getDrawable());
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundDrawable(getDrawable());
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    static StateListDrawable getDrawable() {
        GradientDrawable pressDrawable = new GradientDrawable();
        pressDrawable.setShape(GradientDrawable.RECTANGLE);
        pressDrawable.setColor(Color.parseColor("#dddddd"));

        GradientDrawable normalDrawable = new GradientDrawable();
        normalDrawable.setShape(GradientDrawable.RECTANGLE);
        normalDrawable.setColor(Color.WHITE);

        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_focused}, pressDrawable);
        stateListDrawable.addState(new int[]{}, normalDrawable);
        return stateListDrawable;
    }

    public static void showOpenVisualizedAutoTrackDialog(final Activity context, final String featureCode, final String postUrl) {
        try {
            if (!SensorsDataAPI.sharedInstance().isNetworkRequestEnable()) {
                showDialog(context, "已关闭网络请求（NetworkRequest），无法使用 App 可视化全埋点，请开启后再试！");
                return;
            }
            if (!SensorsDataAPI.sharedInstance().isVisualizedAutoTrackEnabled()) {
                showDialog(context, "SDK 没有被正确集成，请联系贵方技术人员开启可视化全埋点。");
                return;
            }

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
                    startLaunchActivity(context);
                }
            });
            builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    VisualizedAutoTrackService.getInstance().start(context, featureCode, postUrl);
                    startLaunchActivity(context);
                }
            });
            AlertDialog dialog = builder.create();
            dialogShowDismissOld(dialog);
            try {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackground(getDrawable());
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackground(getDrawable());
                } else {
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundDrawable(getDrawable());
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundDrawable(getDrawable());
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void showDialog(final Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("提示")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startLaunchActivity(context);
                    }
                });
        AlertDialog dialog = builder.create();
        dialogShowDismissOld(dialog);
        try {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void showPairingCodeInputDialog(final Context context) {
        if (context == null) {
            SALog.i(TAG, "The argument context can't be null");
            return;
        }
        if (!(context instanceof Activity)) {
            SALog.i(TAG, "The static method showPairingCodeEditDialog(Context context) only accepts Activity as a parameter");
            return;
        }
        Activity activity = (Activity) context;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final PairingCodeEditDialog dialog = new PairingCodeEditDialog(context);
                dialog.show();
            }
        });
    }

    public static void startLaunchActivity(Context context) {
        try {
            if (isSchemeActivity(context)) {
                PackageManager packageManager = context.getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
                context.startActivity(intent);
                ((SchemeActivity) context).finish();
                SALog.i(TAG, "startLaunchActivity");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static boolean isSchemeActivity(Context context) {
        if (context == null) {
            return false;
        }
        return context instanceof SchemeActivity;
    }


    public static void dialogShowDismissOld(Dialog dialog) {
        //全局唯一 Dialog，展现下一个 Dialog 时应该取消上一个 Dialog
        try {
            if (sDialog != null && sDialog.isShowing()) {
                try {
                    sDialog.dismiss();
                    SALog.i(TAG, "Dialog dismiss");
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            sDialog = dialog;
            dialog.show();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private static void requestActiveChannel(String baseUrl, String monitorId,
                                             String projectId, String accountId,
                                             String deviceCode, boolean isActive,
                                             HttpCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("monitor_id", monitorId);
            json.put("distinct_id", SensorsDataAPI.sharedInstance().getDistinctId());
            json.put("project_id", projectId);
            json.put("account_id", accountId);
            json.put("has_active", isActive ? "true" : "false");
            json.put("device_code", deviceCode);
            new RequestHelper.Builder(HttpMethod.POST, baseUrl + "/api/sdk/channel_tool/url")
                    .jsonData(json.toString()).callback(callback).execute();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private static class SendDebugIdThread extends Thread {
        private String distinctId;
        private String infoId;
        private String serverUrl;

        SendDebugIdThread(String serverUrl, String distinctId, String infoId, String name) {
            super(name);
            this.distinctId = distinctId;
            this.infoId = infoId;
            this.serverUrl = serverUrl;
        }

        @Override
        public void run() {
            super.run();
            sendHttpRequest(serverUrl, false);
        }

        private void sendHttpRequest(String serverUrl, boolean isRedirects) {
            ByteArrayOutputStream out = null;
            OutputStream out2 = null;
            BufferedOutputStream bout = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(String.format(serverUrl + "&info_id=%s", infoId));
                SALog.info(TAG, String.format("DebugMode URL:%s", url), null);
                connection = (HttpURLConnection) url.openConnection();
                if (connection == null) {
                    SALog.info(TAG, String.format("can not connect %s,shouldn't happen", url.toString()), null);
                    return;
                }
                SAConfigOptions configOptions = SensorsDataAPI.getConfigOptions();
                if (configOptions != null && configOptions.mSSLSocketFactory != null
                        && connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(configOptions.mSSLSocketFactory);
                }
                connection.setInstanceFollowRedirects(false);
                out = new ByteArrayOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(out);
                String requestBody = "{\"distinct_id\": \"" + distinctId + "\"}";
                writer.write(requestBody);
                writer.flush();
                SALog.info(TAG, String.format("DebugMode request body : %s", requestBody), null);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-type", "text/plain");

                out2 = connection.getOutputStream();
                bout = new BufferedOutputStream(out2);
                bout.write(out.toString().getBytes(CHARSET_UTF8));
                bout.flush();
                out.close();
                int responseCode = connection.getResponseCode();
                SALog.info(TAG, String.format(Locale.CHINA, "DebugMode 后端的响应码是:%d", responseCode), null);
                if (!isRedirects && NetworkUtils.needRedirects(responseCode)) {
                    String location = NetworkUtils.getLocation(connection, serverUrl);
                    if (!TextUtils.isEmpty(location)) {
                        closeStream(out, out2, bout, connection);
                        sendHttpRequest(location, true);
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            } finally {
                closeStream(out, out2, bout, connection);
            }
        }

        private void closeStream(ByteArrayOutputStream out, OutputStream out2, BufferedOutputStream bout, HttpURLConnection connection) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            if (out2 != null) {
                try {
                    out2.close();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            if (bout != null) {
                try {
                    bout.close();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }
}
