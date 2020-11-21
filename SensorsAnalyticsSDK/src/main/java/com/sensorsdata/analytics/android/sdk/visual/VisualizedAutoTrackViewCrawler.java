/*
 * Created by renqingyou on 2019/04/13.
 * Copyright 2015－2020 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.visual;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.AppStateManager;
import com.sensorsdata.analytics.android.sdk.BuildConfig;
import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.visual.model.SnapInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.WebNodeInfo;
import com.sensorsdata.analytics.android.sdk.visual.snap.EditProtocol;
import com.sensorsdata.analytics.android.sdk.visual.snap.EditState;
import com.sensorsdata.analytics.android.sdk.visual.snap.ResourceIds;
import com.sensorsdata.analytics.android.sdk.visual.snap.ResourceReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import static com.sensorsdata.analytics.android.sdk.util.Base64Coder.CHARSET_UTF8;


@TargetApi(16)
class VisualizedAutoTrackViewCrawler implements VTrack {

    private static final int MESSAGE_SEND_STATE_FOR_EDITING = 1;
    private static final String TAG = "SA.VisualizedAutoTrackViewCrawler";
    private final Activity mActivity;
    private final LifecycleCallbacks mLifecycleCallbacks;
    private final EditState mEditState;
    private final ViewCrawlerHandler mMessageThreadHandler;
    private JSONObject mMessageObject;
    private String mFeatureCode;
    private String mPostUrl;
    private String mAppVersion;
    private boolean mVisualizedAutoTrackRunning = false;

    VisualizedAutoTrackViewCrawler(Activity activity, String resourcePackageName, String featureCode, String postUrl) {
        mActivity = activity;
        mFeatureCode = featureCode;
        mEditState = new EditState();
        mEditState.add(activity);
        mLifecycleCallbacks = new LifecycleCallbacks();
        try {
            mPostUrl = URLDecoder.decode(postUrl, CHARSET_UTF8);
            mMessageObject = new JSONObject("{\"type\":\"snapshot_request\",\"payload\":{\"config\":{\"classes\":[{\"name\":\"android.view.View\",\"properties\":[{\"name\":\"importantForAccessibility\",\"get\":{\"selector\":\"isImportantForAccessibility\",\"parameters\":[],\"result\":{\"type\":\"java.lang.Boolean\"}}},{\"name\":\"clickable\",\"get\":{\"selector\":\"isClickable\",\"parameters\":[],\"result\":{\"type\":\"java.lang.Boolean\"}}}]},{\"name\":\"android.widget.TextView\",\"properties\":[{\"name\":\"importantForAccessibility\",\"get\":{\"selector\":\"isImportantForAccessibility\",\"parameters\":[],\"result\":{\"type\":\"java.lang.Boolean\"}}},{\"name\":\"clickable\",\"get\":{\"selector\":\"isClickable\",\"parameters\":[],\"result\":{\"type\":\"java.lang.Boolean\"}}}]},{\"name\":\"android.widget.ImageView\",\"properties\":[{\"name\":\"importantForAccessibility\",\"get\":{\"selector\":\"isImportantForAccessibility\",\"parameters\":[],\"result\":{\"type\":\"java.lang.Boolean\"}}},{\"name\":\"clickable\",\"get\":{\"selector\":\"isClickable\",\"parameters\":[],\"result\":{\"type\":\"java.lang.Boolean\"}}}]}]}}}");
        } catch (Exception e) {
            SALog.printStackTrace(e);
            mMessageObject = null;
        }
        final Application app = (Application) mActivity.getApplicationContext();
        app.registerActivityLifecycleCallbacks(mLifecycleCallbacks);

        try {
            final PackageManager manager = activity.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(activity.getPackageName(), 0);
            mAppVersion = info.versionName;
        } catch (Exception e) {
            mAppVersion = "";
        }

        final HandlerThread thread =
                new HandlerThread(VisualizedAutoTrackViewCrawler.class.getCanonicalName(), Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mMessageThreadHandler = new ViewCrawlerHandler(mActivity, thread.getLooper(), resourcePackageName);
    }

    @Override
    public void startUpdates() {
        try {
            if (!TextUtils.isEmpty(mFeatureCode) && !TextUtils.isEmpty(mPostUrl)) {
                final Application app = (Application) mActivity.getApplicationContext();
                app.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
                mMessageThreadHandler.start();
                mMessageThreadHandler
                        .sendMessage(mMessageThreadHandler.obtainMessage(MESSAGE_SEND_STATE_FOR_EDITING));
                mVisualizedAutoTrackRunning = true;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void stopUpdates(boolean clear) {
        try {
            if (clear) {
                mFeatureCode = null;
                mPostUrl = null;
            }
            mMessageThreadHandler.removeMessages(MESSAGE_SEND_STATE_FOR_EDITING);
            final Application app = (Application) mActivity.getApplicationContext();
            app.unregisterActivityLifecycleCallbacks(mLifecycleCallbacks);
            mVisualizedAutoTrackRunning = false;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    boolean isVisualizedAutoTrackRunning() {
        return mVisualizedAutoTrackRunning;
    }

    private class LifecycleCallbacks
            implements Application.ActivityLifecycleCallbacks {

        private LifecycleCallbacks() {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            mEditState.add(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            mEditState.remove(activity);
        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }

    private class ViewCrawlerHandler extends Handler {

        private final EditProtocol mProtocol;
        private ViewSnapshot mSnapshot;
        private boolean mUseGzip;
        private StringBuilder mLastImageHash;

        private ViewCrawlerHandler(Context context, Looper looper, String resourcePackageName) {
            super(looper);
            mSnapshot = null;
            final ResourceIds resourceIds = new ResourceReader.Ids(resourcePackageName, context);
            mProtocol = new EditProtocol(resourceIds);
            mLastImageHash = new StringBuilder();
            mUseGzip = true;
        }

        public void start() {

        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SEND_STATE_FOR_EDITING:
                    sendSnapshot(mMessageObject);
                    break;
            }
        }

        /**
         * Send a snapshot response, with crawled views and screenshot image, to the connected web UI.
         */
        private void sendSnapshot(JSONObject message) {
            final long startSnapshot = System.currentTimeMillis();
            try {
                final JSONObject payload = message.getJSONObject("payload");
                if (payload.has("config")) {
                    mSnapshot = mProtocol.readSnapshotConfig(payload);
                }

                if (null == mSnapshot) {
                    SALog.i(TAG, "Snapshot should be initialize at first calling.");
                    return;
                }
            } catch (final JSONException e) {
                SALog.i(TAG, "Payload with snapshot config required with snapshot request", e);
                return;
            } catch (final EditProtocol.BadInstructionsException e) {
                SALog.i(TAG, "VisualizedAutoTrack server sent malformed message with snapshot request", e);
                return;
            }

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final OutputStreamWriter writer = new OutputStreamWriter(out);
            SnapInfo info = null;
            try {
                writer.write("{");
                writer.write("\"type\": \"snapshot_response\",");
                writer.write("\"feature_code\": \"" + mFeatureCode + "\",");
                writer.write("\"app_version\": \"" + mAppVersion + "\",");
                writer.write("\"lib_version\": \"" + BuildConfig.SDK_VERSION + "\",");
                writer.write("\"os\": \"Android\",");
                writer.write("\"lib\": \"Android\",");

                if (mUseGzip) {
                    final ByteArrayOutputStream payload_out = new ByteArrayOutputStream();
                    final OutputStreamWriter payload_writer = new OutputStreamWriter(payload_out);

                    payload_writer.write("{\"activities\":");
                    payload_writer.flush();
                    info = mSnapshot.snapshots(mEditState, payload_out, mLastImageHash);
                    final long snapshotTime = System.currentTimeMillis() - startSnapshot;
                    payload_writer.write(",\"snapshot_time_millis\": ");
                    payload_writer.write(Long.toString(snapshotTime));
                    payload_writer.write("}");
                    payload_writer.flush();

                    payload_out.close();
                    byte[] payloadData = payload_out.toString().getBytes();
                    ByteArrayOutputStream os = new ByteArrayOutputStream(payloadData.length);
                    GZIPOutputStream gos = new GZIPOutputStream(os);
                    gos.write(payloadData);
                    gos.close();
                    byte[] compressed = os.toByteArray();
                    os.close();

                    writer.write("\"gzip_payload\": \"" + new String(Base64Coder.encode(compressed)) + "\"");
                } else {
                    writer.write("\"payload\": {");

                    {
                        writer.write("\"activities\":");
                        writer.flush();
                        info = mSnapshot.snapshots(mEditState, out, mLastImageHash);
                    }

                    final long snapshotTime = System.currentTimeMillis() - startSnapshot;
                    writer.write(",\"snapshot_time_millis\": ");
                    writer.write(Long.toString(snapshotTime));

                    writer.write("}");
                }

                String pageName = null;
                if (!TextUtils.isEmpty(info.screenName)) {
                    writer.write(",\"screen_name\": \"" + info.screenName + "\"");
                    pageName = info.screenName;
                }

                // 页面浏览事件中，如果存在 fragment ，则优先取 fragment screenName
                if (info.hasFragment) {
                    String fragmentScreenName = AppStateManager.getInstance().getFragmentScreenName();
                    if (!TextUtils.isEmpty(fragmentScreenName)) {
                        pageName = fragmentScreenName;
                    }
                }

                SALog.i(TAG, "page_name： " + pageName);
                if (!TextUtils.isEmpty(pageName)) {
                    writer.write(",\"page_name\": \"" + pageName + "\"");
                }

                if (!TextUtils.isEmpty(info.activityTitle)) {
                    writer.write(",\"title\": \"" + info.activityTitle + "\"");
                }

                writer.write(",\"is_webview\": " + info.isWebView);

                if (info.isWebView && !TextUtils.isEmpty(info.webViewUrl)) {
                    WebNodeInfo pageInfo = WebNodesManager.getInstance().getWebPageInfo(info.webViewUrl);
                    if (pageInfo != null) {
                        if (!TextUtils.isEmpty(pageInfo.getUrl())) {
                            writer.write(",\"h5_url\": \"" + pageInfo.getUrl() + "\"");
                        }
                        if (!TextUtils.isEmpty(pageInfo.getTitle())) {
                            writer.write(",\"h5_title\": \"" + pageInfo.getTitle() + "\"");
                        }
                    }
                    List<WebNodeInfo.AlertInfo> list = info.alertInfos;
                    if (list != null && list.size() > 0) {
                        writer.write(",\"app_alert_infos\":");
                        writer.flush();
                        writer.write("[");
                        for (int i = 0; i < list.size(); i++) {
                            if (i > 0) {
                                writer.write(",");
                            }
                            WebNodeInfo.AlertInfo alertInfo = list.get(i);
                            if (alertInfo != null) {
                                writer.write("{");
                                writer.write("\"title\":");
                                writer.write("\"" + alertInfo.title + "\"");
                                writer.write(",");
                                writer.write("\"message\":");
                                writer.write("\"" + alertInfo.message + "\"");
                                writer.write(",");
                                writer.write("\"link_text\":");
                                writer.write("\"" + alertInfo.linkText + "\"");
                                writer.write(",");
                                writer.write("\"link_url\":");
                                writer.write("\"" + alertInfo.linkUrl + "\"");
                                writer.write("}");
                            }
                        }
                        writer.write("]");
                        writer.flush();
                    }
                }
                writer.write("}");
                writer.flush();
            } catch (final IOException e) {
                SALog.i(TAG, "Can't write snapshot request to server", e);
            } finally {
                try {
                    writer.close();
                } catch (final IOException e) {
                    SALog.i(TAG, "Can't close writer.", e);
                }
            }
            onSnapFinished(info);
            postSnapshot(out);
        }

        private void onSnapFinished(SnapInfo info) {
            if (info != null && !info.isWebView) {
                WebNodesManager.getInstance().clear();
            }
        }

        private void postSnapshot(ByteArrayOutputStream out) {
            boolean rePostSnapshot = true;
            if (TextUtils.isEmpty(mFeatureCode) || TextUtils.isEmpty(mPostUrl)) {
                return;
            }

            try {
                InputStream in;
                OutputStream out2;
                BufferedOutputStream bout;
                HttpURLConnection connection;
                final URL url = new URL(mPostUrl);
                connection = (HttpURLConnection) url.openConnection();
                SAConfigOptions configOptions = SensorsDataAPI.getConfigOptions();
                if (configOptions != null && configOptions.mSSLSocketFactory != null
                        && connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(configOptions.mSSLSocketFactory);
                }
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-type", "text/plain");

                out2 = connection.getOutputStream();
                bout = new BufferedOutputStream(out2);
                bout.write(out.toString().getBytes(CHARSET_UTF8));
                bout.flush();
                bout.close();
                out.close();

                int responseCode = connection.getResponseCode();
                try {
                    in = connection.getInputStream();
                } catch (FileNotFoundException e) {
                    in = connection.getErrorStream();
                }
                byte[] responseBody = slurp(in);
                in.close();
                out2.close();

                String response = new String(responseBody, CHARSET_UTF8);
                SALog.i(TAG, "responseCode=" + responseCode);
                SALog.i(TAG, "response=" + response);
                JSONObject responseJson = new JSONObject(response);
                if (responseCode == 200) {
                    int delay = responseJson.getInt("delay");
                    if (delay < 0) {
                        rePostSnapshot = false;
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            if (rePostSnapshot) {
                mMessageThreadHandler.sendMessageDelayed(mMessageThreadHandler.obtainMessage(MESSAGE_SEND_STATE_FOR_EDITING), 1000);
            } else {
                stopUpdates(true);
            }
        }

        private byte[] slurp(final InputStream inputStream)
                throws IOException {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[8192];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        }
    }
}
