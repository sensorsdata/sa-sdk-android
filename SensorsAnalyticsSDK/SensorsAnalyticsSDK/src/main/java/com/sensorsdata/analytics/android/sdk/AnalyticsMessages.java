package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.exceptions.ConnectErrorException;
import com.sensorsdata.analytics.android.sdk.exceptions.DebugModeException;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;

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
    /* package */ AnalyticsMessages(final Context context) {
    mContext = context;
    mDbAdapter = new DbAdapter(mContext);
    mWorker = new Worker();
  }

  /**
   * Use this to get an instance of AnalyticsMessages instead of creating one directly
   * for yourself.
   *
   * @param messageContext should be the Main Activity of the application
   *                       associated with these messages.
   */
  public static AnalyticsMessages getInstance(final Context messageContext) {
    synchronized (sInstances) {
      final Context appContext = messageContext.getApplicationContext();
      AnalyticsMessages ret;
      if (!sInstances.containsKey(appContext)) {
        ret = new AnalyticsMessages(appContext);
        sInstances.put(appContext, ret);
      } else {
        ret = sInstances.get(appContext);
      }
      return ret;
    }
  }

  public void enqueueEventMessage(final JSONObject eventJson) {
    final Message m = Message.obtain();
    m.what = ENQUEUE_EVENTS;
    m.obj = eventJson;

    mWorker.runMessage(m);
  }

  public void checkConfigureMessage(final DecideMessages check) {
    final Message m = Message.obtain();
    m.what = CHECK_CONFIGURE;
    m.obj = check;

    mWorker.runMessage(m);
  }

  public void hardKill() {
    final Message m = Message.obtain();
    m.what = KILL_WORKER;

    mWorker.runMessage(m);
  }

  public boolean isWifi() {
    // Wifi
    ConnectivityManager manager = (ConnectivityManager)
        mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);
    if (manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting()) {
      return true;
    }

    return false;
  }

  public boolean is3G() {
    // Mobile network
    TelephonyManager telephonyManager = (TelephonyManager)
        mContext.getSystemService(Context.TELEPHONY_SERVICE);

    int networkType = telephonyManager.getNetworkType();
    switch (networkType) {
      case TelephonyManager.NETWORK_TYPE_UMTS:
      case TelephonyManager.NETWORK_TYPE_EVDO_0:
      case TelephonyManager.NETWORK_TYPE_EVDO_A:
      case TelephonyManager.NETWORK_TYPE_HSDPA:
      case TelephonyManager.NETWORK_TYPE_HSUPA:
      case TelephonyManager.NETWORK_TYPE_HSPA:
      case TelephonyManager.NETWORK_TYPE_EVDO_B:
      case TelephonyManager.NETWORK_TYPE_EHRPD:
      case TelephonyManager.NETWORK_TYPE_HSPAP:
      case TelephonyManager.NETWORK_TYPE_LTE:
        return true;
    }

    // 2G or disconnected to the internet
    return false;
  }

  /////////////////////////////////////////////////////////
  // For testing, to allow for Mocking.

  boolean isDead() {
    return mWorker.isDead();
  }

  public void sendData() {
    try {
      int count = 100;
      while (count > 0) {
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
          mDbAdapter.cleanupEvents(lastId, DbAdapter.Table.EVENTS);
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
          throw new InvalidDataException(e);
        }

        httpPost.setHeader("User-Agent", "SensorsAnalytics Android SDK");
        if (SensorsDataAPI.sharedInstance(mContext).isDebugMode() && !SensorsDataAPI.sharedInstance
            (mContext).isDebugWriteData()) {
          httpPost.setHeader("Dry-Run", "true");
        }

        try {
          HttpResponse response = httpClient.execute(httpPost);

          int response_code = response.getStatusLine().getStatusCode();
          String response_body = EntityUtils.toString(response.getEntity(), "UTF-8");

          if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
            if (response_code == 200) {
              Log.v(LOGTAG, String.format("valid message: %s", rawMessage));
            } else {
              Log.v(LOGTAG, String.format("invalid message: %s", rawMessage));
              Log.v(LOGTAG, String.format("ret_code: %d", response_code));
              Log.v(LOGTAG, String.format("ret_content: %s", response_body));

              // 校验错误，直接将数据删除
              count = mDbAdapter.cleanupEvents(lastId, DbAdapter.Table.EVENTS);
              continue;
            }
          }

          if (response_code != 200) {
            throw new ConnectErrorException("Response error.");
          }

        } catch (ClientProtocolException e) {
          throw new ConnectErrorException(e);
        } catch (IOException e) {
          throw new ConnectErrorException(e);
        }

        count = mDbAdapter.cleanupEvents(lastId, DbAdapter.Table.EVENTS);
      }
    } catch (ConnectErrorException e) {
      if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
        throw new DebugModeException(e.getMessage());
      } else {
        Log.w("Failed to flush events.", e);
      }
    } catch (InvalidDataException e) {
      if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
        throw new DebugModeException(e.getMessage());
      } else {
        Log.w("Failed to flush events.", e);
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

    public boolean isDead() {
      synchronized (mHandlerLock) {
        return mHandler == null;
      }
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
          if (msg.what == ENQUEUE_EVENTS) {
            JSONObject event = (JSONObject) msg.obj;
            synchronized (mDbAdapter) {
              int ret = mDbAdapter.addJSON(event, DbAdapter.Table.EVENTS);

              if (ret < 0) {
                String error = "Failed to enqueue the event: " + event;
                if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
                  throw new DebugModeException(error);
                } else {
                  Log.w(LOGTAG, error);
                }
              }

              if (ret > SensorsDataAPI.sharedInstance(mContext).getFlushBulkSize() || ret ==
                  DbAdapter.DB_OUT_OF_MEMORY_ERROR || SensorsDataAPI.sharedInstance(mContext)
                  .isDebugMode()) {
                sendData();
              } else {
                final Message m = Message.obtain();
                m.what = FLUSH_QUEUE;
                mWorker.runMessageOnce(m, SensorsDataAPI.sharedInstance(mContext)
                    .getFlushInterval());
              }
            }
          } else if (msg.what == FLUSH_QUEUE) {
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
          } else if (msg.what == KILL_WORKER) {
            Log.w(LOGTAG,
                "Worker received a hard kill. Dumping all events and force-killing. Thread id "
                    + Thread.currentThread().getId());
            synchronized (mHandlerLock) {
              mDbAdapter.deleteDB();
              mHandler = null;
              Looper.myLooper().quit();
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
  private static final int ENQUEUE_EVENTS = 1;
  private static final int FLUSH_QUEUE = 3;
  private static final int CHECK_CONFIGURE = 4; // 从SA获取配置信息
  private static final int KILL_WORKER = 5;
  // Hard-kill the worker thread, discarding all events on the event queue. This is for testing, or disasters.

  private static final String LOGTAG = "SA.AnalyticsMessages";

  private static final Map<Context, AnalyticsMessages> sInstances =
      new HashMap<Context, AnalyticsMessages>();

}
