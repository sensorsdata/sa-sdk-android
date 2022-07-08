/*
 * Created by chenru on 2020/09/09.
 * Copyright 2015－2022 Sensors Data Inc.
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
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ThreadNameConstants;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;
import com.sensorsdata.analytics.android.sdk.util.ToastUtil;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.sdk.util.ToastUtil;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class SensorsDataDialogUtils {
    private static final String TAG = "SA.SensorsDataDialogUtils";
    private static Dialog sDialog;
    private static boolean isShowHttpErrorDialog = true;

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
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
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
                                        showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_popwindow_fail));
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
                        currentDebugToastMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_debug_tip_off);
                    } else if (mCurrentDebugMode == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
                        currentDebugToastMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_debug_tip_only);
                    } else if (mCurrentDebugMode == SensorsDataAPI.DebugMode.DEBUG_AND_TRACK) {
                        currentDebugToastMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_debug_tip_track);
                    }
                    ToastUtil.showLong(activity, currentDebugToastMsg);
                    SALog.info(TAG, "The App current DebugMode is ：" + mCurrentDebugMode, null);
                    startLaunchActivity(activity);
                }
            });
            dialogShowDismissOld(dialog);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void showHttpErrorDialog(final Activity context, final String msg) {
        try {
            if (TextUtils.isEmpty(msg) || !isShowHttpErrorDialog || context == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(SADisplayUtil.getStringResource(context, R.string.sensors_analytics_common_title));
            builder.setMessage(msg);
            builder.setCancelable(false);
            builder.setNegativeButton(SADisplayUtil.getStringResource(context, R.string.sensors_analytics_common_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setPositiveButton(SADisplayUtil.getStringResource(context, R.string.sensors_analytics_common_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowHttpErrorDialog = false;
                }
            });
            AlertDialog dialog = builder.create();
            dialogShowDismissOld(dialog);
            try {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
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

    public static StateListDrawable getDrawable() {
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

    public static void showDialog(final Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(SADisplayUtil.getStringResource(context, R.string.sensors_analytics_common_title))
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(SADisplayUtil.getStringResource(context, R.string.sensors_analytics_common_ok), new DialogInterface.OnClickListener() {
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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
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
        String name = context.getClass().getName();
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        if (name.endsWith(SchemeActivity.class.getSimpleName())) {
            Object scheme_activity_sign = ReflectUtil.findFieldRecur(context, "SCHEME_ACTIVITY_SIGN");
            if (scheme_activity_sign instanceof String) {
                return TextUtils.equals((String) scheme_activity_sign, SchemeActivity.SCHEME_ACTIVITY_SIGN);
            }
        }
        return false;
    }


    public static void dialogShowDismissOld(Dialog dialog) {
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
                SALog.info(TAG, String.format(TimeUtils.SDK_LOCALE, "DebugMode ResponseCode: %d", responseCode), null);
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
