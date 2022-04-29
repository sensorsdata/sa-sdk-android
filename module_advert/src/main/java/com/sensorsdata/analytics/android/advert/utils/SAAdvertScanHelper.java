/*
 * Created by chenru on 2022/4/25 下午5:05(format year/.
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

package com.sensorsdata.analytics.android.advert.utils;

import static com.sensorsdata.analytics.android.advert.SAAdvertConstants.TAG;
import static com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils.dialogShowDismissOld;
import static com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils.showDialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.advert.oaid.SAOaidHelper;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ServerUrl;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataLoadingDialog;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

public class SAAdvertScanHelper {
    public static boolean scanHandler(Activity activity, Uri uri) {
        String host = uri.getHost();
        if ("channeldebug".equals(host)) {
            if (ChannelUtils.hasUtmByMetaData(activity)) {
                showDialog(activity, "当前为渠道包，无法使用联调诊断工具");
                return true;
            }
            String monitorId = uri.getQueryParameter("monitor_id");
            if (TextUtils.isEmpty(monitorId)) {
                SensorsDataDialogUtils.startLaunchActivity(activity);
                return true;
            }
            String url = SensorsDataAPI.sharedInstance().getServerUrl();
            if (TextUtils.isEmpty(url)) {
                showDialog(activity, "数据接收地址错误，无法使用联调诊断工具");
                return true;
            }
            ServerUrl serverUrl = new ServerUrl(url);
            String projectName = uri.getQueryParameter("project_name");
            if (serverUrl.getProject().equals(projectName)) {
                String projectId = uri.getQueryParameter("project_id");
                String accountId = uri.getQueryParameter("account_id");
                String isReLink = uri.getQueryParameter("is_relink");
                if ("1".equals(isReLink)) {//续连标识 1 :续连
                    String deviceCode = uri.getQueryParameter("device_code");
                    if (ChannelUtils.checkDeviceInfo(activity, deviceCode)) {//比较设备信息是否匹配
                        showChannelDebugActiveDialog(activity);
                    } else {
                        showDialog(activity, "无法重连，请检查是否更换了联调手机");
                    }
                } else {
                    showChannelDebugDialog(activity, serverUrl.getBaseUrl(), monitorId, projectId, accountId);
                }
            } else {
                showDialog(activity, "App 集成的项目与电脑浏览器打开的项目不同，无法使用联调诊断工具");
            }
            return true;
        }
        return false;
    }

    public static void showChannelDebugActiveDialog(final Activity activity) {
        showDialog(activity, "成功开启调试模式",
                "此模式下不需要卸载 App，点击“激活”按钮可反复触发激活", "激活", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        trackChannelDebugInstallation(activity);
                        showChannelDebugActiveDialog(activity);
                    }
                }, "取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SensorsDataDialogUtils.startLaunchActivity(activity);
                    }
                });
    }

    private static void trackChannelDebugInstallation(final Activity activity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject _properties = new JSONObject();
                    _properties.put("$ios_install_source", ChannelUtils.getDeviceInfo(activity,
                            SAAdvertUtils.getAndroidId(activity), SAOaidHelper.getOAID(activity)));
                    // 先发送 track
                    SAEventManager.getInstance().trackEvent(EventType.TRACK, "$ChannelDebugInstall", _properties, null);
                    // 再发送 profile_set_once 或者 profile_set
                    JSONObject profileProperties = new JSONObject();
                    SensorsDataUtils.mergeJSONObject(_properties, profileProperties);
                    profileProperties.put("$first_visit_time", new java.util.Date());
                    SAEventManager.getInstance().trackEvent(EventType.PROFILE_SET_ONCE, null, profileProperties, null);
                    SensorsDataAPI.sharedInstance().flush();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }).start();

    }

    public static void showChannelDebugDialog(final Activity activity,
                                              final String baseUrl,
                                              final String monitorId,
                                              final String projectId,
                                              final String accountId) {
        showDialog(activity, "即将开启联调模式", "", "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                Context context = activity.getApplicationContext();
                boolean isTrackInstallation = ChannelUtils.isTrackInstallation();
                if (!isTrackInstallation || ChannelUtils.isCorrectTrackInstallation()) {
                    String androidId = SensorsDataUtils.getAndroidID(context);
                    String oaid = SAOaidHelper.getOAID(context);
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
                                        showChannelDebugActiveDialog(activity);
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
                SensorsDataDialogUtils.startLaunchActivity(activity);
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
}
