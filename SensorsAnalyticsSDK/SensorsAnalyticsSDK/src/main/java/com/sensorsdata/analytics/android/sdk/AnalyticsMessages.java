package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.HttpService;
import com.sensorsdata.analytics.android.sdk.util.RemoteService;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manage communication of events with the internal database and the SensorsData servers.
 * <p>
 * <p>This class straddles the thread boundary between user threads and
 * a logical SensorsData thread.
 */
/* package */ class AnalyticsMessages {

  /**
   * Do not call directly. You should call AnalyticsMessages.getInstance()
   */
    /* package */ AnalyticsMessages(final Context context) {
    mContext = context;
    mConfig = getConfig(context);
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

  public void eventsMessage(final JSONObject eventJson) {
    final Message m = Message.obtain();
    m.what = ENQUEUE_EVENTS;
    m.obj = eventJson;
    mWorker.runMessage(m);
  }

  public void peopleMessage(final JSONObject peopleJson) {
    final Message m = Message.obtain();
    m.what = ENQUEUE_PEOPLE;
    m.obj = peopleJson;

    mWorker.runMessage(m);
  }

  public void postToServer() {
    final Message m = Message.obtain();
    m.what = FLUSH_QUEUE;

    mWorker.runMessage(m);
  }

  public void hardKill() {
    final Message m = Message.obtain();
    m.what = KILL_WORKER;

    mWorker.runMessage(m);
  }

  /////////////////////////////////////////////////////////
  // For testing, to allow for Mocking.

  /* package */ boolean isDead() {
    return mWorker.isDead();
  }

  protected DbAdapter makeDbAdapter(Context context) {
    return new DbAdapter(context);
  }

  protected SSConfig getConfig(Context context) {
    return SSConfig.getInstance(context);
  }

  protected RemoteService getPoster() {
    return new HttpService();
  }

  // Sends a message if and only if we are running with SensorsData Message log enabled.
  // Will be called from the SensorsData thread.
  private void logAboutMessageToSensorsData(String message) {
    if (SSConfig.DEBUG) {
      Log.v(LOGTAG, message + " (Thread " + Thread.currentThread().getId() + ")");
    }
  }

  private void logAboutMessageToSensorsData(String message, Throwable e) {
    if (SSConfig.DEBUG) {
      Log.v(LOGTAG, message + " (Thread " + Thread.currentThread().getId() + ")", e);
    }
  }

  public boolean isWifiOr3G() {
    // Wifi
    ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(
        mContext.CONNECTIVITY_SERVICE);
    if (manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting()) {
      return true;
    }

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

  // Worker will manage the (at most single) IO thread associated with
  // this AnalyticsMessages instance.
  // XXX: Worker class is unnecessary, should be just a subclass of HandlerThread
  private class Worker {
    public Worker() {
      mHandler = restartWorkerThread();
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
          logAboutMessageToSensorsData("Dead worker dropping a message: " + msg.what);
        } else {
          mHandler.sendMessage(msg);
        }
      }
    }

    // NOTE that the returned worker will run FOREVER, unless you send a hard kill
    // (which you really shouldn't)
    private Handler restartWorkerThread() {
      final HandlerThread thread =
          new HandlerThread("com.sensorsdata.analytics.android.sdk.AnalyticsMessages.Worker",
              Thread.MIN_PRIORITY);
      thread.start();
      final Handler ret = new AnalyticsMessageHandler(thread.getLooper());
      return ret;
    }

    private class AnalyticsMessageHandler extends Handler {

      public AnalyticsMessageHandler(Looper looper) {
        super(looper);
        mDbAdapter = null;
        mFlushInterval = mConfig.getFlushInterval();
        mSystemInformation = new SystemInformation(mContext);
        mRetryAfter = -1;
      }

      @Override public void handleMessage(Message msg) {
        if (mDbAdapter == null) {
          mDbAdapter = makeDbAdapter(mContext);
        }

        try {
          int returnCode = DbAdapter.DB_UNDEFINED_CODE;

          if (msg.what == ENQUEUE_PEOPLE) {
            final JSONObject message = (JSONObject) msg.obj;

            logAboutMessageToSensorsData("Queuing people record for sending later");
            logAboutMessageToSensorsData("    " + message.toString());

            returnCode = mDbAdapter.addJSON(message, DbAdapter.Table.PEOPLE);
          } else if (msg.what == ENQUEUE_EVENTS) {
            final JSONObject message = (JSONObject) msg.obj;

            logAboutMessageToSensorsData("Queuing event for sending later");
            logAboutMessageToSensorsData("    " + message.toString());

            returnCode = mDbAdapter.addJSON(message, DbAdapter.Table.EVENTS);
          } else if (msg.what == FLUSH_QUEUE) {
            logAboutMessageToSensorsData("Flushing queue due to scheduled or forced flush");
            updateFlushFrequency();
            if (SystemClock.elapsedRealtime() >= mRetryAfter) {
              try {
                sendAllData(mDbAdapter);
              } catch (RemoteService.ServiceUnavailableException e) {
                mRetryAfter = SystemClock.elapsedRealtime() + e.getRetryAfter() * 1000;
              }
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

          ///////////////////////////

          // Flush the queue due to the limit while the network type is 3G/4G/wifi.
          if (isWifiOr3G()) {
            if ((returnCode >= mConfig.getBulkSize() || returnCode == DbAdapter.DB_OUT_OF_MEMORY_ERROR)
                && SystemClock.elapsedRealtime() >= mRetryAfter) {
              logAboutMessageToSensorsData("Flushing queue due to bulk upload limit");
              updateFlushFrequency();
              try {
                sendAllData(mDbAdapter);
              } catch (RemoteService.ServiceUnavailableException e) {
                mRetryAfter = SystemClock.elapsedRealtime() + e.getRetryAfter() * 1000;
              }
            } else if (returnCode > 0 && !hasMessages(FLUSH_QUEUE)) {
              logAboutMessageToSensorsData("Queue depth " + returnCode + " - Adding flush in " + mFlushInterval);
              if (mFlushInterval >= 0) {
                sendEmptyMessageDelayed(FLUSH_QUEUE, mFlushInterval);
              }
            }
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
      }// handleMessage

      private void sendAllData(DbAdapter dbAdapter)
          throws RemoteService.ServiceUnavailableException {
        final RemoteService poster = getPoster();
        if (!poster.isOnline(mContext)) {
          logAboutMessageToSensorsData(
              "Not flushing data to SensorsData because the device is not connected to the internet.");
          return;
        }

        logAboutMessageToSensorsData("Sending records to SensorsData");

        sendData(dbAdapter, DbAdapter.Table.EVENTS, new String[] {mConfig.getServerUrl()});
        sendData(dbAdapter, DbAdapter.Table.PEOPLE, new String[] {mConfig.getServerUrl()});
      }

      private void sendData(DbAdapter dbAdapter, DbAdapter.Table table, String[] urls)
          throws RemoteService.ServiceUnavailableException {
        final RemoteService poster = getPoster();
        final String[] eventsData = dbAdapter.generateDataString(table);

        if (eventsData != null) {
          final String lastId = eventsData[0];
          final String rawMessage = eventsData[1];

          final String encodedData = Base64Coder.encodeString(rawMessage);
          final List<NameValuePair> params = new ArrayList<NameValuePair>(1);
          params.add(new BasicNameValuePair("data_list", encodedData));
          params.add(new BasicNameValuePair("gzip", "0"));

          boolean deleteEvents = true;
          byte[] response;
          for (String url : urls) {
            try {
              response = poster.performRequest(url, params);
              deleteEvents =
                  true; // Delete events on any successful post, regardless of 1 or 0 response
              if (null == response) {
                logAboutMessageToSensorsData(
                    "Response was null, unexpected failure posting to " + url + ".");
              } else {
                String parsedResponse;
                try {
                  parsedResponse = new String(response, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                  throw new RuntimeException("UTF not supported on this platform?", e);
                }

                logAboutMessageToSensorsData("Successfully posted to " + url + ": \n" + rawMessage);
                logAboutMessageToSensorsData("Response was " + parsedResponse);
              }
              break;
            } catch (final OutOfMemoryError e) {
              Log.e(LOGTAG, "Out of memory when posting to " + url + ".", e);
              break;
            } catch (final MalformedURLException e) {
              Log.e(LOGTAG, "Cannot interpret " + url + " as a URL.", e);
              break;
            } catch (final IOException e) {
              logAboutMessageToSensorsData("Cannot post message to " + url + ".", e);
              deleteEvents = false;
            }
          }

          if (deleteEvents) {
            logAboutMessageToSensorsData(
                "Not retrying this batch of events, deleting them from DB.");
            dbAdapter.cleanupEvents(lastId, table);
          } else {
            logAboutMessageToSensorsData("Retrying this batch of events.");
            if (!hasMessages(FLUSH_QUEUE)) {
              sendEmptyMessageDelayed(FLUSH_QUEUE, mFlushInterval);
            }
          }
        }
      }

      private DbAdapter mDbAdapter;
      private final long mFlushInterval;
      private long mRetryAfter;
    }// AnalyticsMessageHandler

    private void updateFlushFrequency() {
      final long now = System.currentTimeMillis();
      final long newFlushCount = mFlushCount + 1;

      if (mLastFlushTime > 0) {
        final long flushInterval = now - mLastFlushTime;
        final long totalFlushTime = flushInterval + (mAveFlushFrequency * mFlushCount);
        mAveFlushFrequency = totalFlushTime / newFlushCount;

        final long seconds = mAveFlushFrequency / 1000;
        logAboutMessageToSensorsData(
            "Average send frequency approximately " + seconds + " seconds.");
      }

      mLastFlushTime = now;
      mFlushCount = newFlushCount;
    }

    private final Object mHandlerLock = new Object();
    private Handler mHandler;
    private long mFlushCount = 0;
    private long mAveFlushFrequency = 0;
    private long mLastFlushTime = -1;
    private SystemInformation mSystemInformation;
  }

  /////////////////////////////////////////////////////////

  // Used across thread boundaries
  private final Worker mWorker;
  private final Context mContext;
  private final SSConfig mConfig;

  // Messages for our thread
  private static final int ENQUEUE_PEOPLE = 0; // submit events and people data
  private static final int ENQUEUE_EVENTS = 1; // push given JSON message to people DB
  private static final int FLUSH_QUEUE = 2; // push given JSON message to events DB
  private static final int KILL_WORKER = 5;
  // Hard-kill the worker thread, discarding all events on the event queue. This is for testing, or disasters.

  private static final String LOGTAG = "SA.AnalyticsMessages";

  private static final Map<Context, AnalyticsMessages> sInstances =
      new HashMap<Context, AnalyticsMessages>();

}
