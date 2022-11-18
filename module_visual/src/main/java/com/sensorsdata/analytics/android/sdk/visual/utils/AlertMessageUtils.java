package com.sensorsdata.analytics.android.sdk.visual.utils;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;
import com.sensorsdata.analytics.android.sdk.visual.AbstractViewCrawler;
import com.sensorsdata.analytics.android.sdk.visual.NodesProcess;
import com.sensorsdata.analytics.android.sdk.visual.R;
import com.sensorsdata.analytics.android.sdk.visual.model.NodeInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.SnapInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.WebNodeInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class AlertMessageUtils {

    private static final String TAG = "SA.AlertMessageUtils";

    public static class AlertRunnable implements Runnable {

        public enum AlertType {
            H5,
            FLUTTER
        }

        private final String unique;
        private final AlertType alertType;

        public AlertRunnable(AlertType alertType, String unique) {
            this.unique = unique;
            this.alertType = alertType;
        }

        @Override
        public void run() {
            switch (alertType) {
                case H5:
                    h5AlertHandlerFailure(unique);
                    break;
                case FLUTTER:
                    flutterAlertHandlerFailure(unique);
                    break;
            }
        }
    }

    private static void flutterAlertHandlerFailure(String mActivityName) {
        SALog.i(TAG, "Flutter page is not integrated SDK");
        if (!TextUtils.isEmpty(mActivityName)) {
            Context context = SensorsDataAPI.sharedInstance().getSAContextManager().getContext();
            String title = SADisplayUtil.getStringResource(context, R.string.sensors_analytics_visual_sa_h5);
            String message = SADisplayUtil.getStringResource(context, R.string.sensors_analytics_visual_sa_flutter_error);
            String link_text = SADisplayUtil.getStringResource(context, R.string.sensors_analytics_visual_sa_h5_error_link);
            String msg = "{\"callType\":\"app_alert\",\"data\":[{\"title\":\"" + title + "\",\"message\":\"" + message + "\",\"link_text\":\"" + link_text + "\",\"link_url\":\"https://manual.sensorsdata.cn/sa/latest/flutter-22257963.html\"}]}";
            NodesProcess.getInstance().getFlutterNodesManager().handlerFailure(mActivityName, msg);
        }
    }

    private static void h5AlertHandlerFailure(String url) {
        if (!TextUtils.isEmpty(url)) {
            WebNodeInfo webNodeInfo = (WebNodeInfo) NodesProcess.getInstance().getWebNodesManager().getNodes(url);
            if (webNodeInfo == null) {
                SALog.i(TAG, "H5 page is not integrated Web JS SDK");
                Context context = SensorsDataAPI.sharedInstance().getSAContextManager().getContext();
                String title = SADisplayUtil.getStringResource(context, R.string.sensors_analytics_visual_sa_h5);
                String message = SADisplayUtil.getStringResource(context, R.string.sensors_analytics_visual_sa_h5_error);
                String link_text = SADisplayUtil.getStringResource(context, R.string.sensors_analytics_visual_sa_h5_error_link);
                String msg = "{\"callType\":\"app_alert\",\"data\":[{\"title\":\"" + title + "\",\"message\":\"" + message + "\",\"link_text\":\"" + link_text + "\",\"link_url\":\"https://manual.sensorsdata.cn/sa/latest/tech_sdk_client_web_use-7545346.html\"}]}";
                NodesProcess.getInstance().getWebNodesManager().handlerFailure(url, msg);
            }
        }
    }

    public static void buildH5AlertInfo(OutputStream writer, String mType, SnapInfo info, Context context) throws IOException {
        WebNodeInfo pageInfo = (WebNodeInfo) NodesProcess.getInstance().getWebNodesManager().getPageInfo(info.webViewUrl);
        if (pageInfo != null) {
            if (!TextUtils.isEmpty(pageInfo.getUrl())) {
                writer.write((",\"h5_url\": \"" + pageInfo.getUrl() + "\"").getBytes());
            }
            if (!TextUtils.isEmpty(pageInfo.getTitle())) {
                writer.write((",\"h5_title\": \"" + pageInfo.getTitle() + "\"").getBytes());
            }
        }
        List<NodeInfo.AlertInfo> list = info.alertInfos;
        buildAlertInfo(writer, mType, list, context);
    }

    public static void buildFlutterAlertInfo(OutputStream writer, String mType, SnapInfo info, Context context) throws IOException {
        List<NodeInfo.AlertInfo> list = info.flutter_alertInfos;
        buildAlertInfo(writer, mType, list, context);
    }

    private static void buildAlertInfo(OutputStream writer, String mType, List<NodeInfo.AlertInfo> list, Context context) throws IOException {
        if (list != null && list.size() > 0) {
            writer.write((",\"app_alert_infos\":").getBytes());
            writer.flush();
            writer.write("[".getBytes());
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    writer.write(",".getBytes());
                }
                NodeInfo.AlertInfo alertInfo = list.get(i);
                if (alertInfo != null) {
                    if (TextUtils.equals(AbstractViewCrawler.TYPE_HEAT_MAP, mType)) {
                        alertInfo.title = alertInfo.title.replace(SADisplayUtil.getStringResource(context, R.string.sensors_analytics_visual),
                                SADisplayUtil.getStringResource(context, R.string.sensors_analytics_heatmap));
                    }
                    writer.write("{".getBytes());
                    writer.write(("\"title\":").getBytes());
                    writer.write(("\"" + alertInfo.title + "\"").getBytes());
                    writer.write(",".getBytes());
                    writer.write(("\"message\":").getBytes());
                    writer.write(("\"" + alertInfo.message + "\"").getBytes());
                    writer.write(",".getBytes());
                    writer.write(("\"link_text\":").getBytes());
                    writer.write(("\"" + alertInfo.linkText + "\"").getBytes());
                    writer.write(",".getBytes());
                    writer.write(("\"link_url\":").getBytes());
                    writer.write(("\"" + alertInfo.linkUrl + "\"").getBytes());
                    writer.write("}".getBytes());
                }
            }
            writer.write("]".getBytes());
            writer.flush();
        }
    }
}