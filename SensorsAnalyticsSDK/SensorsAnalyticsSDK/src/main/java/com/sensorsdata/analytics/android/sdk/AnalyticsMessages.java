package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.exceptions.ConnectErrorException;
import com.sensorsdata.analytics.android.sdk.exceptions.DebugModeException;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.exceptions.ResponseErrorException;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Manage communication of events with the internal database and the SensorsData servers.
 * <p/>
 * <p>This class straddles the thread boundary between user threads and
 * a logical SensorsData thread.
 */
class AnalyticsMessages {

  /**
   * Do not call directly. You should call AnalyticsMessages.getInstance()
   */
    /* package */ AnalyticsMessages(final Context context, final String packageName) {
    mContext = context;
    mDbAdapter = new DbAdapter(mContext, packageName/*dbName*/);
    mWorker = new Worker();
  }

  /**
   * Use this to get an instance of AnalyticsMessages instead of creating one directly
   * for yourself.
   *
   * @param messageContext should be the Main Activity of the application
   *                       associated with these messages.
   */
  public static AnalyticsMessages getInstance(final Context messageContext, final String
      packageName) {
    synchronized (sInstances) {
      final Context appContext = messageContext.getApplicationContext();
      final AnalyticsMessages ret;
      if (!sInstances.containsKey(appContext)) {
        ret = new AnalyticsMessages(appContext, packageName);
        sInstances.put(appContext, ret);
      } else {
        ret = sInstances.get(appContext);
      }
      return ret;
    }
  }

  public void enqueueEventMessage(final String type, final JSONObject eventJson) {
    synchronized (mDbAdapter) {
      int ret = mDbAdapter.addJSON(eventJson, DbAdapter.Table.EVENTS);
      if (ret < 0) {
        String error = "Failed to enqueue the event: " + eventJson;
        if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
          throw new DebugModeException(error);
        } else {
          Log.w(LOGTAG, error);
        }
      }

      final Message m = Message.obtain();
      m.what = FLUSH_QUEUE;

      if (SensorsDataAPI.sharedInstance(mContext).isDebugMode() || ret ==
          DbAdapter.DB_OUT_OF_MEMORY_ERROR) {
        mWorker.runMessage(m);
      } else {
        String networkType = SensorsDataUtils.networkType(mContext);
        if (networkType.equals("WIFI") || networkType.equals("3G") || networkType.equals("4G")) {
          // track_signup 立即发送
          if (type.equals("track_signup") || ret > SensorsDataAPI.sharedInstance(mContext)
              .getFlushBulkSize()) {
            mWorker.runMessage(m);
          } else {
            final int interval = SensorsDataAPI.sharedInstance(mContext).getFlushInterval();
            mWorker.runMessageOnce(m, interval);
          }
        }
      }
    }
  }

  public void checkConfigure(final DecideMessages check) {
    final Message m = Message.obtain();
    m.what = CHECK_CONFIGURE;
    m.obj = check;

    mWorker.runMessage(m);
  }

  public void flush() {
    final Message m = Message.obtain();
    m.what = FLUSH_QUEUE;

    mWorker.runMessage(m);
  }

  public void sendData() {
    int count = 100;
    while (count > 0) {
      try {
        String[] eventsData;
        synchronized (mDbAdapter) {
          if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
            eventsData = mDbAdapter.generateDataString(DbAdapter.Table.EVENTS, 1);
          } else {
            eventsData = mDbAdapter.generateDataString(DbAdapter.Table.EVENTS, 100);
          }
        }
        if (eventsData == null) {
          return;
        }

        final String lastId = eventsData[0];
        final String rawMessage = eventsData[1];

        String data;
        try {
          data = encodeData(rawMessage);
        } catch (IOException e) {
          // 格式错误，直接将数据删除
          throw new InvalidDataException(e);
        }

        final List<NameValuePair> params = new ArrayList<NameValuePair>(1);
        params.add(new BasicNameValuePair("data_list", data));
        params.add(new BasicNameValuePair("gzip", "1"));

        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(SensorsDataAPI.sharedInstance(mContext).getServerUrl());

        try {
          httpPost.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
          // 格式错误，直接将数据删除
          throw new InvalidDataException(e);
        }

        httpPost.setHeader("User-Agent", "SensorsAnalytics Android SDK");
        if (SensorsDataAPI.sharedInstance(mContext).isDebugMode() && !SensorsDataAPI.sharedInstance
            (mContext).isDebugWriteData()) {
          httpPost.setHeader("Dry-Run", "true");
        }

        try {
          HttpResponse response = httpClient.execute(httpPost);

          int responseCode = response.getStatusLine().getStatusCode();
          String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

          if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
            if (responseCode == 200) {
              Log.i(LOGTAG, String.format("valid message: %s", rawMessage));
            } else {
              Log.i(LOGTAG, String.format("invalid message: %s", rawMessage));
              Log.i(LOGTAG, String.format("ret_code: %d", responseCode));
              Log.i(LOGTAG, String.format("ret_content: %s", responseBody));
            }
          }

          if (responseCode < 200 || responseCode >= 300) {
            // 校验错误，直接将数据删除
            throw new ResponseErrorException(String.format("flush failure with response '%s'",
                responseBody));
          }
        } catch (ClientProtocolException e) {
          throw new ConnectErrorException(e);
        } catch (IOException e) {
          throw new ConnectErrorException(e);
        } finally {
          count = mDbAdapter.cleanupEvents(lastId, DbAdapter.Table.EVENTS);
          Log.i(LOGTAG, String.format("Events flushed. [left = %d]", count));
        }

      } catch (ConnectErrorException e) {
        Log.w(LOGTAG, "Connection error: " + e.getMessage());
        break;
      } catch (InvalidDataException e) {
        if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
          throw new DebugModeException(e.getMessage());
        } else {
          Log.w(LOGTAG, "Invalid data: " + e.getMessage());
        }
      } catch (ResponseErrorException e) {
        if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
          throw new DebugModeException(e.getMessage());
        } else {
          Log.w(LOGTAG, "Unexpected response from Sensors Analytics: " + e.getMessage());
        }
      }
    }
  }

  private String encodeData(final String rawMessage) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes().length);
    GZIPOutputStream gos = new GZIPOutputStream(os);
    gos.write(rawMessage.getBytes());
    gos.close();
    byte[] compressed = os.toByteArray();
    os.close();
    return new String(Base64Coder.encode(compressed));
  }

  private String getCheckConfigure() throws ConnectErrorException {
    HttpClient httpClient = new DefaultHttpClient();
    HttpGet httpPost = new HttpGet(SensorsDataAPI.sharedInstance(mContext).getConfigureUrl());

    try {
      HttpResponse response = httpClient.execute(httpPost);

      int responseCode = response.getStatusLine().getStatusCode();
      String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

      if (responseCode != 200) {
        throw new ConnectErrorException("Response error.");
      }

      return responseBody;
    } catch (ClientProtocolException e) {
      throw new ConnectErrorException(e);
    } catch (IOException e) {
      throw new ConnectErrorException(e);
    }
  }

  // Worker will manage the (at most single) IO thread associated with
  // this AnalyticsMessages instance.
  // XXX: Worker class is unnecessary, should be just a subclass of HandlerThread
  private class Worker {

    public Worker() {
      final HandlerThread thread =
          new HandlerThread("com.sensorsdata.analytics.android.sdk.AnalyticsMessages.Worker",
              Thread.MIN_PRIORITY);
      thread.start();
      mHandler = new AnalyticsMessageHandler(thread.getLooper());
    }

    public void runMessage(Message msg) {
      synchronized (mHandlerLock) {
        if (mHandler == null) {
          // We died under suspicious circumstances. Don't try to send any more events.
          Log.v(LOGTAG, "Dead worker dropping a message: " + msg.what);
        } else {
          mHandler.sendMessage(msg);
        }
      }
    }

    public void runMessageOnce(Message msg, long delay) {
      synchronized (mHandlerLock) {
        if (mHandler == null) {
          // We died under suspicious circumstances. Don't try to send any more events.
          Log.v(LOGTAG, "Dead worker dropping a message: " + msg.what);
        } else {
          if (!mHandler.hasMessages(msg.what)) {
            Log.w(LOGTAG, "send delayed after " + delay);
            mHandler.sendMessageDelayed(msg, delay);
          }
        }
      }
    }

    private class AnalyticsMessageHandler extends Handler {

      public AnalyticsMessageHandler(Looper looper) {
        super(looper);
      }

      @Override
      public void handleMessage(Message msg) {
        try {
          if (msg.what == FLUSH_QUEUE) {
            sendData();
          } else if (msg.what == CHECK_CONFIGURE) {
            DecideMessages decideMessages = (DecideMessages) msg.obj;
            try {
              final String configureResult = getCheckConfigure();
              try {
                final JSONObject configureJson = new JSONObject(configureResult);
                final JSONObject eventBindings = configureJson.getJSONObject("event_bindings");
                if (eventBindings.has("events") && eventBindings.get("events") instanceof
                    JSONArray) {
                  decideMessages.reportResults(eventBindings.getJSONArray("events"));
                }
              } catch (JSONException e1) {
                Log.w(LOGTAG, "Unexpected vtrack configure from SensorsAnalytics: " +
                    configureResult);
              }
            } catch (ConnectErrorException e) {
              Log.e(LOGTAG, "Failed to get vtrack configure from SensorsAnalaytics.", e);
            }
          } else {
            Log.e(LOGTAG, "Unexpected message received by SensorsData worker: " + msg);
          }
        } catch (final RuntimeException e) {
          Log.e(LOGTAG, "Worker threw an unhandled exception", e);
          synchronized (mHandlerLock) {
            mHandler = null;
            try {
              Looper.myLooper().quit();
              Log.e(LOGTAG, "SensorsData will not process any more analytics messages", e);
            } catch (final Exception tooLate) {
              Log.e(LOGTAG, "Could not halt looper", tooLate);
            }
          }
        }
      }
    }

    private final Object mHandlerLock = new Object();
    private Handler mHandler;
  }

  // Used across thread boundaries
  private final Worker mWorker;
  private final Context mContext;
  private final DbAdapter mDbAdapter;

  // Messages for our thread
  private static final int FLUSH_QUEUE = 3;
  private static final int CHECK_CONFIGURE = 4; // 从SA获取配置信息

  private static final String LOGTAG = "SA.AnalyticsMessages";

  private static final Map<Context, AnalyticsMessages> sInstances =
      new HashMap<Context, AnalyticsMessages>();

}
