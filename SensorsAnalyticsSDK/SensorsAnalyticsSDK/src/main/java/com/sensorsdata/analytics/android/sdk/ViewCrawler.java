package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.JsonWriter;
import android.util.Log;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(SensorsDataAPI.VTRACK_SUPPORTED_MIN_API)
public class ViewCrawler implements VTrack, DebugTracking {

  public ViewCrawler(Context context, String resourcePackageName) {
    mContext = context;

    mEditState = new EditState();

    final Application app = (Application) context.getApplicationContext();
    app.registerActivityLifecycleCallbacks(new LifecycleCallbacks());

    final HandlerThread thread =
        new HandlerThread(ViewCrawler.class.getCanonicalName(), Process.THREAD_PRIORITY_BACKGROUND);
    thread.start();

    mMessageThreadHandler = new ViewCrawlerHandler(context, thread.getLooper(), resourcePackageName);

    mDynamicEventTracker = new DynamicEventTracker(context, mMessageThreadHandler);
  }

  @Override
  public void startUpdates() {
    mMessageThreadHandler.start();
    mMessageThreadHandler
        .sendMessage(mMessageThreadHandler.obtainMessage(MESSAGE_INITIALIZE_CHANGES));
  }

  @Override
  public void setEventBindings(JSONArray bindings) {
    final Message msg =
        mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_EVENT_BINDINGS_RECEIVED);
    msg.obj = bindings;
    mMessageThreadHandler.sendMessage(msg);
  }

  @Override
  public void reportTrack(JSONObject eventJson) {
    final Message m = mMessageThreadHandler.obtainMessage();
    m.what = MESSAGE_SEND_EVENT_TRACKED;
    m.obj = eventJson;

    mMessageThreadHandler.sendMessage(m);
  }

  private class EmulatorConnector implements Runnable {
    public EmulatorConnector() {
      mStopped = true;
    }

    @Override
    public void run() {
      if (! mStopped) {
        final Message message = mMessageThreadHandler.obtainMessage(MESSAGE_CONNECT_TO_EDITOR);
        mMessageThreadHandler.sendMessage(message);
      }

      mMessageThreadHandler.postDelayed(this, EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS);
    }

    public void start() {
      mStopped = false;
      mMessageThreadHandler.post(this);
    }

    public void stop() {
      mStopped = true;
      mMessageThreadHandler.removeCallbacks(this);
    }

    private volatile boolean mStopped;
  }

  private class LifecycleCallbacks
      implements Application.ActivityLifecycleCallbacks, FlipGesture.OnFlipGestureListener {

    public LifecycleCallbacks() {
      mFlipGesture = new FlipGesture(this);
      mEmulatorConnector = new EmulatorConnector();
    }

    @Override public void onFlipGesture() {
      final Message message = mMessageThreadHandler.obtainMessage(MESSAGE_CONNECT_TO_EDITOR);
      mMessageThreadHandler.sendMessage(message);
    }

    @Override public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override public void onActivityStarted(Activity activity) {
    }

    @Override public void onActivityResumed(Activity activity) {
      installConnectionSensor(activity);
      mEditState.add(activity);
    }

    @Override public void onActivityPaused(Activity activity) {
      mEditState.remove(activity);
      if (mEditState.isEmpty()) {
        uninstallConnectionSensor(activity);
      }
    }

    @Override public void onActivityStopped(Activity activity) {
    }

    @Override public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override public void onActivityDestroyed(Activity activity) {
    }

    private void installConnectionSensor(final Activity activity) {
      if (SensorsDataUtils.isInEmulator()) {
        mEmulatorConnector.start();
      } else {
        final SensorManager sensorManager =
            (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        final Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager
            .registerListener(mFlipGesture, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
      }
    }

    private void uninstallConnectionSensor(final Activity activity) {
      if (SensorsDataUtils.isInEmulator()) {
        mEmulatorConnector.stop();
      } else {
        final SensorManager sensorManager =
            (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mFlipGesture);
      }
    }

    private final FlipGesture mFlipGesture;
    private final EmulatorConnector mEmulatorConnector;
  }


  private class ViewCrawlerHandler extends Handler {

    public ViewCrawlerHandler(Context context, Looper looper, String resourcePackageName) {
      super(looper);
      mContext = context;
      mSnapshot = null;

      final ResourceIds resourceIds = new ResourceReader.Ids(resourcePackageName, context);

      mProtocol = new EditProtocol(resourceIds);
      mEditorEventBindings = new ArrayList<Pair<String, JSONObject>>();
      mPersistentEventBindings = new ArrayList<Pair<String, JSONObject>>();

      mStartLock = new ReentrantLock();
      mStartLock.lock();
    }

    public void start() {
      mStartLock.unlock();
    }

    @Override public void handleMessage(Message msg) {
      mStartLock.lock();//pay close attention to this

      try {
        switch (msg.what) {
          case MESSAGE_INITIALIZE_CHANGES:
            initializeBindings();
            break;
          case MESSAGE_CONNECT_TO_EDITOR:
            connectToEditor();
            break;
          case MESSAGE_SEND_DEVICE_INFO:
            sendDeviceInfo();
            break;
          case MESSAGE_SEND_STATE_FOR_EDITING:
            sendSnapshot((JSONObject) msg.obj);
            break;
          case MESSAGE_SEND_EVENT_TRACKED:
            sendReportTrackToEditor((JSONObject) msg.obj);
            break;
          case MESSAGE_EVENT_BINDINGS_RECEIVED:
            handleEventBindingsReceived((JSONArray) msg.obj);
            break;
          case MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED:
            handleEditorBindingsReceived((JSONObject) msg.obj);
            break;
          case MESSAGE_HANDLE_EDITOR_CLOSED:
            handleEditorClosed();
            break;
          case MESSAGE_HANDLE_DISCONNECT:
            handleDisconnect();
            break;
        }
      } finally {
        mStartLock.unlock();
      }
    }

    /**
     * Load stored changes from persistent storage and apply them to the application.
     */
    private void initializeBindings() {
      final SharedPreferences preferences = getSharedPreferences();
      final String storedBindings = preferences.getString(SHARED_PREF_BINDINGS_KEY, null);
      try {
        if (null != storedBindings) {
          if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
            Log.v(LOGTAG, "Initialize event bindings: " + storedBindings);
          }

          final JSONArray bindings = new JSONArray(storedBindings);

          mPersistentEventBindings.clear();
          for (int i = 0; i < bindings.length(); i++) {
            final JSONObject event = bindings.getJSONObject(i);
            final String targetActivity = JSONUtils.optionalStringKey(event, "target_activity");
            mPersistentEventBindings.add(new Pair<String, JSONObject>(targetActivity, event));
          }
        }
      } catch (final JSONException e) {
        Log.w(LOGTAG, "JSON error when initializing saved changes, clearing persistent memory", e);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.remove(SHARED_PREF_BINDINGS_KEY);
        editor.apply();
      }

      applyVariantsAndEventBindings();
    }

    /**
     * Try to connect to the remote interactive editor, if a connection does not already exist.
     */
    private void connectToEditor() {
      if (mEditorConnection != null && mEditorConnection.isValid()) {
        Log.d(LOGTAG, "The Editor has been connected.");
        return;
      }

      Log.d(LOGTAG, "Connecting to the Editor...");

      final String url = SensorsDataAPI.sharedInstance(mContext).getVTrackServerUrl();

      try {
        mEditorConnection = new EditorConnection(new URI(url), new Editor());
      } catch (final URISyntaxException e) {
        Log.e(LOGTAG, "Error parsing URI " + url + " for editor websocket", e);
      } catch (final EditorConnection.EditorConnectionException e) {
        Log.e(LOGTAG, "Error connecting to URI " + url, e);
      } catch (final IOException e) {
        Log.i(LOGTAG, "Can't create SSL Socket to connect to editor service", e);
      }
    }

    /**
     * Report on device info to the connected web UI.
     */
    private void sendDeviceInfo() {
      if (mEditorConnection == null || !mEditorConnection.isValid()) {
        return;
      }

      final PackageManager manager = mContext.getPackageManager();
      final DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();

      //final OutputStream out = mEditorConnection.getBufferedOutputStream();
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final JsonWriter j = new JsonWriter(new OutputStreamWriter(out));

      try {
        j.beginObject();
        j.name("type").value("device_info_response");
        j.name("payload");
        {
          j.beginObject();
          try {
            j.name("$lib").value("Android");
            j.name("$lib_version").value(SensorsDataAPI.VERSION);
            j.name("$os").value("Android");
            j.name("$os_version").value(
                Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            j.name("$screen_height").value(String.valueOf(displayMetrics.heightPixels));
            j.name("$screen_width").value(String.valueOf(displayMetrics.widthPixels));
            try {
              final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
              j.name("$main_bundle_identifier").value(info.packageName);
              j.name("$app_version").value(info.versionName);
            } catch (PackageManager.NameNotFoundException e) {
              j.name("$main_bundle_identifier").value("");
              j.name("$app_version").value("");
            }
            j.name("$device_name").value(Build.BRAND + "/" + Build.MODEL);
            j.name("$device_model").value(Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
            j.name("$device_id").value(SensorsDataUtils.getDeviceID(mContext));
          } catch (Exception e) {
            Log.e(LOGTAG, "sendDeviceInfo;fill sendInfo error:e=", e);
          }
          j.endObject();
        }
        j.endObject();
      } catch (final IOException e) {
        Log.e(LOGTAG, "Can't write device_info to server", e);
      } finally {
        try {
          j.close();
        } catch (final IOException e) {
          Log.e(LOGTAG, "Can't close websocket writer", e);
        }
      }

      if (mEditorConnection != null && mEditorConnection.isValid()) {
        mEditorConnection.sendMessage(out.toString());
      }
    }

    /**
     * Send a snapshot response, with crawled views and screenshot image, to the connected web UI.
     */
    private void sendSnapshot(JSONObject message) {
      if (mEditorConnection == null || !mEditorConnection.isValid()) {
        return;
      }

      final long startSnapshot = System.currentTimeMillis();

      try {
        final JSONObject payload = message.getJSONObject("payload");
        if (payload.has("config")) {
          mSnapshot = mProtocol.readSnapshotConfig(payload);
        }

        if (null == mSnapshot) {
          Log.w(LOGTAG, "Snapshot should be initialize at first callsing.");
          return;
        }

        if (payload.has("last_image_hash")) {
          final String lastImageHash = payload.getString("last_image_hash");
          mSnapshot.updateLastImageHashArray(lastImageHash);
        }
      } catch (final JSONException e) {
        Log.e(LOGTAG, "Payload with snapshot config required with snapshot request", e);
        return;
      } catch (final EditProtocol.BadInstructionsException e) {
        Log.e(LOGTAG, "Editor sent malformed message with snapshot request", e);
        return;
      }

      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final OutputStreamWriter writer = new OutputStreamWriter(out);

      try {
        writer.write("{");
        writer.write("\"type\": \"snapshot_response\",");
        writer.write("\"payload\": {");
        {
          writer.write("\"activities\":");
          writer.flush();
          mSnapshot.snapshots(mEditState, out);
        }

        final long snapshotTime = System.currentTimeMillis() - startSnapshot;
        writer.write(",\"snapshot_time_millis\": ");
        writer.write(Long.toString(snapshotTime));

        writer.write("}");
        writer.write("}");
      } catch (final IOException e) {
        Log.e(LOGTAG, "Can't write snapshot request to server", e);
      } finally {
        try {
          writer.close();
        } catch (final IOException e) {
          Log.e(LOGTAG, "Can't close writer.", e);
        }
      }

      if (mEditorConnection != null && mEditorConnection.isValid()) {
        mEditorConnection.sendMessage(out.toString());
      }
    }

    /**
     * Report the eventbinding response to the connected web UI.
     */
    private void sendEventBindingResponse(boolean result) {
      if (mEditorConnection == null || !mEditorConnection.isValid()) {
        return;
      }

      //final OutputStream out = mEditorConnection.getBufferedOutputStream();
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final JsonWriter j = new JsonWriter(new OutputStreamWriter(out));

      try {
        j.beginObject();
        j.name("type").value("event_binding_response");
        j.name("payload").beginObject();
        j.name("status").value("OK");
        j.endObject();
        j.endObject();
      } catch (final IOException e) {
        Log.e(LOGTAG, "Can't write event_binding_response to server", e);
      } finally {
        try {
          j.close();
        } catch (final IOException e) {
          Log.e(LOGTAG, "Can't close websocket writer", e);
        }
      }

      if (mEditorConnection != null && mEditorConnection.isValid()) {
        mEditorConnection.sendMessage(out.toString());
      }
    }

    /**
     * Report that a track has occurred to the connected web UI.
     */
    private void sendReportTrackToEditor(JSONObject eventJson) {
      if (mEditorConnection == null || !mEditorConnection.isValid() || eventJson == null) {
        return;
      }

      final JSONObject sendProperties = eventJson.optJSONObject("properties");
      if (sendProperties == null) {
        return;
      }

      Log.d(LOGTAG, "Sending debug track to editor. original event: " + eventJson.toString());

      final String fromVTrack = sendProperties.optString("$from_vtrack", "");
      if (fromVTrack.length() < 1) {
        return;
      }

      final OutputStream out = mEditorConnection.getBufferedOutputStream();
      final OutputStreamWriter writer = new OutputStreamWriter(out);

      try {
        JSONObject payload = new JSONObject();
        payload.put("depolyed", sendProperties.getBoolean("$binding_depolyed"));
        payload.put("trigger_id", sendProperties.getString("$binding_trigger_id"));
        payload.put("path", sendProperties.getString("$binding_path"));

        sendProperties.remove("$binding_path");
        sendProperties.remove("$binding_depolyed");
        sendProperties.remove("$binding_trigger_id");
        eventJson.put("properties", sendProperties);

        payload.put("event", eventJson);

        JSONObject result = new JSONObject();
        result.put("type", "debug_track");
        result.put("payload", payload);

        writer.write(result.toString());
        writer.flush();
      } catch (JSONException e) {
        Log.e(LOGTAG, "Invalied proprties", e);
      } catch (final IOException e) {
        Log.e(LOGTAG, "Can't write track_message to server", e);
      } finally {
        try {
          writer.close();
        } catch (final IOException e) {
          Log.e(LOGTAG, "Can't close writer.", e);
        }
      }
    }

    /**
     * Accept and apply a persistent event binding from a non-interactive source.
     */
    private void handleEventBindingsReceived(JSONArray eventBindings) {
      final SharedPreferences preferences = getSharedPreferences();
      final SharedPreferences.Editor editor = preferences.edit();
      editor.putString(SHARED_PREF_BINDINGS_KEY, eventBindings.toString());
      editor.apply();
      initializeBindings();
    }

    /**
     * Accept and apply a temporary event binding from the connected UI.
     */
    private void handleEditorBindingsReceived(JSONObject message) {
      Log.d(LOGTAG, String.format("获取模拟器事件配置: %s", message.toString()));

      final JSONArray eventBindings;

      sendEventBindingResponse(true);

      try {
        final JSONObject payload = message.getJSONObject("payload");
        eventBindings = payload.getJSONArray("events");
      } catch (final JSONException e) {
        Log.e(LOGTAG, "Bad event bindings received", e);
        return;
      }

      final int eventCount = eventBindings.length();

      mEditorEventBindings.clear();
      for (int i = 0; i < eventCount; i++) {
        try {
          final JSONObject event = eventBindings.getJSONObject(i);
          final String targetActivity = JSONUtils.optionalStringKey(event, "target_activity");
          mEditorEventBindings.add(new Pair<String, JSONObject>(targetActivity, event));
        } catch (final JSONException e) {
          Log.e(LOGTAG, "Bad event binding received from editor in " + eventBindings.toString(), e);
        }
      }

      applyVariantsAndEventBindings();
    }

    /**
     * Clear state associated with the editor now that the editor is gone.
     */
    private void handleEditorClosed() {
      Log.d(LOGTAG, "Editor closed.");

      mSnapshot = null;

      mEditorEventBindings.clear();
      applyVariantsAndEventBindings();
    }

    /**
     * disconnect websocket server;.
     */
    private void handleDisconnect() {
      if (mEditorConnection == null) {
        return;
      }

      mEditorConnection.close(true);
    }

    /**
     * Reads our JSON-stored edits from memory and submits them to our EditState. Overwrites
     * any existing edits at the time that it is run.
     * <p/>
     * applyVariantsAndEventBindings should be called any time we load new event bindings from
     * disk or when we receive new edits from the interactive UI editor.
     * Changes and event bindings from our persistent storage and temporary changes
     * received from interactive editing will all be submitted to our EditState, tweaks
     * will be updated, and experiment statuses will be tracked.
     */
    private void applyVariantsAndEventBindings() {
      final List<Pair<String, ViewVisitor>> newVisitors =
          new ArrayList<Pair<String, ViewVisitor>>();

      Log.d(LOGTAG, String.format("加载事件配置。 模拟器事件配置 %d ，正式事件配置 %d", mEditorEventBindings.size(),
          mPersistentEventBindings.size()));

      if (mEditorEventBindings.size() > 0) {
        // 如果mEditorEventBindings.size() > 0，说明连接了VTrack模拟器，只是用模拟器下发的事件配置
        for (Pair<String, JSONObject> changeInfo : mEditorEventBindings) {
          try {
            final ViewVisitor visitor =
                mProtocol.readEventBinding(changeInfo.second, mDynamicEventTracker);
            newVisitors.add(new Pair<String, ViewVisitor>(changeInfo.first, visitor));
          } catch (final EditProtocol.InapplicableInstructionsException e) {
            Log.i(LOGTAG, e.getMessage());
          } catch (final EditProtocol.BadInstructionsException e) {
            Log.e(LOGTAG, "Bad editor event binding cannot be applied.", e);
          }
        }
      } else {
        for (final Pair<String, JSONObject> changeInfo : mPersistentEventBindings) {
          try {
            final ViewVisitor visitor =
                mProtocol.readEventBinding(changeInfo.second, mDynamicEventTracker);
            newVisitors.add(new Pair<String, ViewVisitor>(changeInfo.first, visitor));
          } catch (final EditProtocol.InapplicableInstructionsException e) {
            Log.i(LOGTAG, e.getMessage());
          } catch (final EditProtocol.BadInstructionsException e) {
            Log.e(LOGTAG, "Bad persistent event binding cannot be applied.", e);
          }
        }
      }

      final Map<String, List<ViewVisitor>> editMap = new HashMap<String, List<ViewVisitor>>();
      final int totalEdits = newVisitors.size();
      for (int i = 0; i < totalEdits; i++) {
        final Pair<String, ViewVisitor> next = newVisitors.get(i);
        final List<ViewVisitor> mapElement;
        if (editMap.containsKey(next.first)) {
          mapElement = editMap.get(next.first);
        } else {
          mapElement = new ArrayList<ViewVisitor>();
          editMap.put(next.first, mapElement);
        }
        mapElement.add(next.second);
      }

      mEditState.setEdits(editMap);
    }

    private SharedPreferences getSharedPreferences() {
      final String sharedPrefsName = SHARED_PREF_EDITS_FILE;
      return mContext.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
    }

    private EditorConnection mEditorConnection;
    private ViewSnapshot mSnapshot;
    private final Context mContext;
    private final Lock mStartLock;
    private final EditProtocol mProtocol;

    private final List<Pair<String, JSONObject>> mEditorEventBindings;
    private final List<Pair<String, JSONObject>> mPersistentEventBindings;
  }


  private class Editor implements EditorConnection.Editor {
    @Override public void sendSnapshot(JSONObject message) {
      final Message msg =
          mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_SEND_STATE_FOR_EDITING);
      msg.obj = message;
      mMessageThreadHandler.sendMessage(msg);
    }

    @Override public void bindEvents(JSONObject message) {
      final Message msg =
          mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED);
      msg.obj = message;
      mMessageThreadHandler.sendMessage(msg);
    }

    @Override public void sendDeviceInfo() {
      final Message msg = mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_SEND_DEVICE_INFO);
      mMessageThreadHandler.sendMessage(msg);
    }

    @Override public void cleanup() {
      final Message msg =
          mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_EDITOR_CLOSED);
      mMessageThreadHandler.sendMessage(msg);
    }

    @Override public void disconnect() {
      mIsRetryConnect = false;
      final Message msg =
          mMessageThreadHandler.obtainMessage(ViewCrawler.MESSAGE_HANDLE_DISCONNECT);
      mMessageThreadHandler.sendMessage(msg);
    }

    @Override public void onWebSocketOpen() {
      if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
        Log.v(LOGTAG, "onWebSocketOpen");
      }
      mCurrentRetryTimes = 0;
      mIsRetryConnect = true;
    }

    @Override
    public void onWebSocketClose(int code) {
      Log.d(LOGTAG, "onWebSocketClose; mIsRetryConnect=" + mIsRetryConnect + ";"
            + "mCurrentRetryTimes="
            + mCurrentRetryTimes);

      if (code != CLOSE_CODE_NOCODE) {
        mIsRetryConnect = false;
        mCurrentRetryTimes = 0;
        return;
      }

      if (mCurrentRetryTimes >= CONNECT_RETRY_TIMES) {
        mIsRetryConnect = false;
      }

      if (mIsRetryConnect) {
        final Message message = mMessageThreadHandler.obtainMessage(MESSAGE_CONNECT_TO_EDITOR);
        mMessageThreadHandler.sendMessageDelayed(message, RETRY_TIME_INTERVAL);
        mCurrentRetryTimes++;
      }
    }

  }

  private static boolean mIsRetryConnect = true;
  ;
  private static int mCurrentRetryTimes = 0;
  private static final int CONNECT_RETRY_TIMES = 40;
  private static final long RETRY_TIME_INTERVAL = 30 * 1000;
  /*
      WebSocket close Code:
      check com.sensorsdata.analytics.android.sdk.java_websocket.framing.CloseFrame.NORMAL
  */
  private static final int CLOSE_CODE_NORMAL = 1000;
  private static final int CLOSE_CODE_GOING_AWAY = 1001;
  private static final int CLOSE_CODE_NOCODE = 1005;

  private final Context mContext;
  private final DynamicEventTracker mDynamicEventTracker;
  private final EditState mEditState;
  private final ViewCrawlerHandler mMessageThreadHandler;

  private static final String SHARED_PREF_EDITS_FILE = "sensorsdata";
  private static final String SHARED_PREF_BINDINGS_KEY = "sensorsdata.viewcrawler.bindings";

  private static final int MESSAGE_INITIALIZE_CHANGES = 0;
  private static final int MESSAGE_CONNECT_TO_EDITOR = 1;
  private static final int MESSAGE_SEND_STATE_FOR_EDITING = 2;
  private static final int MESSAGE_SEND_DEVICE_INFO = 4;
  private static final int MESSAGE_EVENT_BINDINGS_RECEIVED = 5;
  private static final int MESSAGE_HANDLE_EDITOR_BINDINGS_RECEIVED = 6;
  private static final int MESSAGE_SEND_EVENT_TRACKED = 7;
  private static final int MESSAGE_HANDLE_EDITOR_CLOSED = 8;
  private static final int MESSAGE_HANDLE_DISCONNECT = 13;

  private static final int EMULATOR_CONNECT_ATTEMPT_INTERVAL_MILLIS = 1000 * 30;

  private static final String LOGTAG = "SA.ViewCrawler";
}
