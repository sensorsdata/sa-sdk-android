package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.exceptions.ConnectErrorException;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * Sensors Analytics SDK
 */
public class SensorsDataAPI {

  /**
   * Debug 模式，用于检验数据导入是否正确。该模式下，事件会逐条实时发送到 Sensors Analytics，并根据返回值检查
   * 数据导入是否正确。
   *
   * Debug 模式的具体使用方式，请参考:
   *   http://www.sensorsdata.cn/manual/debug_mode.html
   *
   * Debug 模式有三种：
   *   DEBUG_OFF - 关闭DEBUG模式
   *   DEBUG_ONLY - 打开DEBUG模式，但该模式下发送的数据仅用于调试，不进行数据导入
   *   DEBUG_AND_TRACK - 打开DEBUG模式，并将数据导入到SensorsAnalytics中
   */
  public enum DebugMode {
    DEBUG_OFF(false, false),
    DEBUG_ONLY(true, false),
    DEBUG_AND_TRACK(true, true);

    private final boolean mDebugMode;
    private final boolean mDebugWriteData;

    DebugMode(boolean debugMode, boolean debugWriteData) {
      mDebugMode = debugMode;
      mDebugWriteData = debugWriteData;
    }

    boolean isDebugMode() {
      return mDebugMode;
    }

    boolean isDebugWriteData() {
      return mDebugWriteData;
    }
  }

  SensorsDataAPI(Context context, String serverURL, String configureURL, String vtrackServerURL,
      SensorsDataAPI.DebugMode debugMode) {
    mContext = context;

    final String packageName = context.getApplicationContext().getPackageName();
    try {
      final ApplicationInfo appInfo = context.getApplicationContext().getPackageManager()
          .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
      Bundle configBundle = appInfo.metaData;
      if (null == configBundle) {
        configBundle = new Bundle();
      }

      if (debugMode.isDebugMode()) {
        Uri serverURI = Uri.parse(serverURL);

        int pathPrefix = serverURI.getPath().lastIndexOf('/');
        String newPath = serverURI.getPath().substring(0, pathPrefix) + "/debug";

        // 将 URI Path 中末尾的部分替换成 '/debug'
        mServerUrl = serverURI.buildUpon().path(newPath).build().toString();
      } else {
        mServerUrl = serverURL;
      }

      // 若 Configure Url 为 'api/vtrack/config' ，则补齐 SDK 类型
      Uri configureURI = Uri.parse(configureURL);
      if (configureURI.getPath().equals("/api/vtrack/config")) {
        mConfigureUrl = configureURI.buildUpon().appendPath("Android.conf").build().toString();
      } else {
        mConfigureUrl = configureURL;
      }

      if (vtrackServerURL == null) {
        // 根据 Configure Url 自动配置 VTrack Server Url
        mVTrackServerUrl = configureURI.buildUpon().path("/api/ws").scheme("ws").build().toString();
      } else {
        mVTrackServerUrl = vtrackServerURL;
      }

      mDebugMode = debugMode;

      // 若程序在模拟器中运行，默认 FlushInterval 为1秒；否则为60秒
      if (SensorsDataUtils.isInEmulator()) {
        mFlushInterval = configBundle.getInt("com.sensorsdata.analytics.android.FlushInterval", 1000);
      } else {
        mFlushInterval = configBundle.getInt("com.sensorsdata.analytics.android.FlushInterval", 60000);
      }

      if (Build.VERSION.SDK_INT >= VTRACK_SUPPORTED_MIN_API
          && configBundle.getBoolean("com.sensorsdata.analytics.android.VTrack", true)) {
        String resourcePackageName =
            configBundle.getString("com.sensorsdata.analytics.android.ResourcePackageName");
        if (null == resourcePackageName) {
          resourcePackageName = context.getPackageName();
        }

        mVTrack = new ViewCrawler(mContext, resourcePackageName);
      } else {
        Log.i(LOGTAG, "VTrack is not supported on this Android OS Version");
        mVTrack = new VTrackUnsupported();
      }
    } catch (final PackageManager.NameNotFoundException e) {
      throw new RuntimeException("Can't configure SensorsDataAPI with package name " + packageName,
          e);
    }

    Log.v(LOGTAG, String.format("Initializing the instance of Sensors Analytics SDK with server"
        + " url '%s', configure url '%s', vtrack server url '%s', flush interval %d ms", mServerUrl,
        mConfigureUrl, mVTrackServerUrl, mFlushInterval));

    final Map<String, Object> deviceInfo = new HashMap<String, Object>();

    {
      deviceInfo.put("$lib", "Android");
      deviceInfo.put("$lib_version", VERSION);
      deviceInfo.put("$os", "Android");
      deviceInfo.put("$os_version",
          Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
      deviceInfo
          .put("$manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
      deviceInfo.put("$model", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
      try {
        final PackageManager manager = mContext.getPackageManager();
        final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
        deviceInfo.put("$app_version", info.versionName);
      } catch (final PackageManager.NameNotFoundException e) {
        Log.e(LOGTAG, "Exception getting app version name", e);
      }
      final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
      deviceInfo.put("$screen_height", displayMetrics.heightPixels);
      deviceInfo.put("$screen_width", displayMetrics.widthPixels);
    }

    mDeviceInfo = Collections.unmodifiableMap(deviceInfo);

    mPersistentIdentity = getPersistentIdentity(context);

    mMessages = AnalyticsMessages.getInstance(mContext);
  }

  /**
   * 获取SensorsDataAPI单例
   *
   * @param context App的Context
   *
   * @return SensorsDataAPI单例
   */
  public static SensorsDataAPI sharedInstance(Context context) {
    if (null == context) {
      return null;
    }
    synchronized (sInstanceMap) {
      final Context appContext = context.getApplicationContext();
      SensorsDataAPI instance = sInstanceMap.get(appContext);

      if (null == instance) {
        Log.w(LOGTAG, "The static method sharedInstance(context, serverURL, configureURL, "
            + "vtrackServerURL, debugMode) should be called before calling sharedInstance()");
      }
      return instance;
    }
  }

  /**
   * 初始化并获取SensorsDataAPI单例
   *
   * @param context App 的 Context
   * @param serverURL 用于收集事件的服务地址
   * @param configureUrl 用于获取SDK配置的服务地址
   * @param debugMode Debug模式,
   *                  {@link com.sensorsdata.analytics.android.sdk.SensorsDataAPI.DebugMode}
   *
   * @return SensorsDataAPI单例
   */
  public static SensorsDataAPI sharedInstance(Context context, String serverURL, String
      configureUrl, DebugMode debugMode) {
    if (null == context) {
      return null;
    }

    synchronized (sInstanceMap) {
      final Context appContext = context.getApplicationContext();

      SensorsDataAPI instance = sInstanceMap.get(appContext);
      if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
        instance = new SensorsDataAPI(appContext, serverURL, configureUrl, null, debugMode);
        sInstanceMap.put(appContext, instance);

        try {
          instance.track("$AppStart", null);
        } catch (InvalidDataException e) {
          Log.w("Unexpected exception", e);
        }
      }

      return instance;
    }
  }

  /**
   * 初始化并获取SensorsDataAPI单例（打开可视化埋点功能）
   *
   * @param context App的Context
   * @param serverURL 用于收集事件的服务地址
   * @param configureURL 用于获取SDK配置的服务地址
   * @param vtrackServerURL 可视化埋点的WebServer地址
   * @param debugMode Debug模式,
   *                  {@link com.sensorsdata.analytics.android.sdk.SensorsDataAPI.DebugMode}
   *
   * @return SensorsDataAPI单例
   */
  public static SensorsDataAPI sharedInstance(Context context, String serverURL,
      String configureURL, String vtrackServerURL, DebugMode debugMode) {
    if (null == context) {
      return null;
    }

    synchronized (sInstanceMap) {
      final Context appContext = context.getApplicationContext();

      SensorsDataAPI instance = sInstanceMap.get(appContext);
      if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
        instance = new SensorsDataAPI(appContext, serverURL, configureURL, vtrackServerURL,
            debugMode);
        sInstanceMap.put(appContext, instance);

        instance.mVTrack.startUpdates();
        instance.mMessages.checkConfigureMessage(new DecideMessages(instance.mVTrack));

        try {
          instance.track("$AppStart", null);
        } catch (InvalidDataException e) {
          Log.w("Unexpected exception", e);
        }
      }

      return instance;
    }
  }

  /**
   * 两次数据发送的最小时间间隔，单位毫秒
   *
   * 默认值为60 * 1000毫秒，DEBUG模式下为1 * 1000毫秒
   * 在每次调用track、signUp以及profileSet等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
   *
   *   1. 当前是否是WIFI/3G/4G网络条件
   *   2. 与上次发送的时间间隔是否大于flushInterval
   *
   * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
   * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存20MB数据。
   *
   * @return 返回时间间隔，单位毫秒
   */
  public int getFlushInterval() {
    return mFlushInterval;
  }

  /**
   * 设置两次数据发送的最小时间间隔
   *
   * @param flushInterval 时间间隔，单位毫秒
   */
  public void setFlushInterval(int flushInterval) {
    mFlushInterval = flushInterval;
  }

  /**
   * 获取当前用户的distinctId
   *
   * 若调用前未调用 {@link #identify(String)} 设置用户的 distinctId，SDK 会调用 {@link java.util.UUID} 随机生成
   * UUID，作为用户的 distinctId
   *
   * @return 当前用户的distinctId
   */
  public String getDistinctId() {
    return new String(mPersistentIdentity.getDistinctId());
  }

  /**
   * 设置当前用户的distinctId。一般情况下，如果是一个注册用户，则应该使用注册系统内
   * 的user_id，如果是个未注册用户，则可以选择一个不会重复的匿名ID，如设备ID等，如果
   * 客户没有设置indentify，则使用SDK自动生成的匿名ID
   *
   * @param distinctId 当前用户的distinctId，仅接受数字、下划线和大小写字母
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException 当 distinctId
   * 不符合规范时抛出异常
   */
  public void identify(String distinctId) throws InvalidDataException {
    assertDistinctId(distinctId);

    synchronized (mPersistentIdentity) {
      mPersistentIdentity.setDistinctId(distinctId);
    }
  }

  /**
   * 记录第一次登录行为
   *
   * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
   *   http://www.sensorsdata.cn/manual/track_signup.html
   * 并在必要时联系我们的技术支持人员。
   *
   * @param newDistinctId 用户完成注册后生成的注册ID
   * @param properties    事件的属性
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException 当 distinctId
   * 不符合规范或事件属性不符合规范时抛出异常
   */
  public void trackSignUp(String newDistinctId, JSONObject properties) throws InvalidDataException {
    String originalDistinctId = getDistinctId();
    identify(newDistinctId);

    trackEvent(EventType.TRACK_SIGNUP, "$SignUp", properties, originalDistinctId);
  }

  /**
   * 与 {@link #trackSignUp(String, org.json.JSONObject)} 类似，无事件属性
   *
   * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
   *   http://www.sensorsdata.cn/manual/track_signup.html，
   * 并在必要时联系我们的技术支持人员。
   *
   * @param newDistinctId 用户完成注册后生成的注册ID
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException 当 distinctId
   * 不符合规范时抛出异常
   */
  public void trackSignUp(String newDistinctId) throws InvalidDataException {
    trackSignUp(newDistinctId, null);
  }

  /**
   * 调用track接口，追踪一个带有属性的事件
   *
   * @param eventName  事件的名称
   * @param properties 事件的属性
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException 当事件名称或属性
   * 不符合规范时抛出异常
   */
  public void track(String eventName, JSONObject properties) throws InvalidDataException {
    trackEvent(EventType.TRACK, eventName, properties, null);
  }

  /**
   * 与 {@link #track(String, org.json.JSONObject)} 类似，无事件属性
   *
   * @param eventName 事件的名称
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException 当事件名称
   * 不符合规范时抛出异常
   */
  public void track(String eventName) throws InvalidDataException {
    trackEvent(EventType.TRACK, eventName, null, null);
  }

  /**
   * 将所有本地缓存的日志发送到 Sensors Analytics.
   */
  public void flush() {
    mMessages.flushMessage(0);
  }

  /**
   * 获取事件公共属性
   *
   * @return 当前所有Super属性
   */
  public JSONObject getSuperProperties() {
    JSONObject ret = new JSONObject();
    mPersistentIdentity.addSuperPropertiesToObject(ret);
    return ret;
  }

  /**
   * 注册所有事件都有的公共属性
   *
   * @param superProperties 事件公共属性
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException 当公共属性不符合规范时抛出异常
   */
  public void registerSuperProperties(JSONObject superProperties) throws InvalidDataException {
    assertPropertyTypes(EventType.REGISTER_SUPER_PROPERTIES, superProperties);
    synchronized (mPersistentIdentity) {
      mPersistentIdentity.registerSuperProperties(superProperties);
    }
  }

  /**
   * 删除事件公共属性
   *
   * @param superPropertyName 事件属性名称
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException 当属性名称不符合规范时抛出异常
   */
  public void unregisterSuperProperty(String superPropertyName) throws InvalidDataException {
    assertKey(superPropertyName);
    synchronized (mPersistentIdentity) {
      mPersistentIdentity.unregisterSuperProperty(superPropertyName);
    }
  }

  /**
   * 删除所有事件公共属性
   */
  public void clearSuperProperties() {
    synchronized (mPersistentIdentity) {
      mPersistentIdentity.clearSuperProperties();
    }
  }

  /**
   * 设置用户的一个或多个Profile。
   * Profile如果存在，则覆盖；否则，新创建。
   *
   * @param properties 属性列表
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称或属性值不符合规范时抛出异常
   */
  public void profileSet(JSONObject properties) throws InvalidDataException {
    trackEvent(EventType.PROFILE_SET, null, properties, null);
  }

  /**
   * 设置用户的一个Profile，如果之前存在，则覆盖，否则，新创建
   *
   * @param property 属性名称
   * @param value    属性的值，值的类型只允许为
   * {@link java.lang.String}, {@link java.lang.Number}, {@link java.util.Date}, {@link java.util.List}
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称或属性值不符合规范时抛出异常
   */
  public void profileSet(String property, Object value) throws InvalidDataException {
    try {
      profileSet(new JSONObject().put(property, value));
    } catch (JSONException e) {
      throw new InvalidDataException("Unexpected property name or value.");
    }
  }

  /**
   * 首次设置用户的一个或多个Profile。
   * 与profileSet接口不同的是，Profile如果存在，则覆盖；否则，新创建。
   *
   * @param properties 属性列表
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称或属性值不符合规范时抛出异常
   */
  public void profileSetOnce(JSONObject properties) throws InvalidDataException {
    trackEvent(EventType.PROFILE_SET_ONCE, null, properties, null);
  }

  /**
   * 首次设置用户的一个Profile
   * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
   *
   * @param property 属性名称
   * @param value    属性的值，值的类型只允许为
   * {@link java.lang.String}, {@link java.lang.Number}, {@link java.util.Date}, {@link java.util.List}
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称或属性值不符合规范时抛出异常
   */
  public void profileSetOnce(String property, Object value) throws InvalidDataException {
    try {
      profileSetOnce(new JSONObject().put(property, value));
    } catch (JSONException e) {
      throw new InvalidDataException("Unexpected property name or value.");
    }
  }

  /**
   * 给一个或多个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
   * 未设置，则添加属性并设置默认值为0
   *
   * @param properties 一个或多个属性集合
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称或属性值不符合规范时抛出异常
   */
  public void profileIncrement(Map<String, ? extends Number> properties)
      throws InvalidDataException {
    trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject(properties), null);
  }

  /**
   * 给一个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
   * 未设置，则添加属性并设置默认值为0
   *
   * @param property 属性名称
   * @param value    属性的值，值的类型只允许为 {@link java.lang.Number}
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称或属性值不符合规范时抛出异常
   */
  public void profileIncrement(String property, Number value) throws InvalidDataException {
    try {
      trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject().put(property, value), null);
    } catch (JSONException e) {
      throw new InvalidDataException("Unexpected property name or value.");
    }
  }

  /**
   * 给一个列表类型的Profile增加一个元素
   *
   * @param property 属性名称
   * @param value    新增的元素
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称或属性值不符合规范时抛出异常
   */
  public void profileAppend(String property, String value) throws InvalidDataException {
    Set<String> values = new HashSet<String>();
    values.add(value);
    profileAppend(property, values);
  }

  /**
   * 给一个列表类型的Profile增加一个或多个元素
   *
   * @param property 属性名称
   * @param values   新增的元素集合
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称或属性值不符合规范时抛出异常
   */
  public void profileAppend(String property, Set<String> values) throws InvalidDataException {
    try {
      final JSONArray append_values = new JSONArray();
      for (String value : values) {
        append_values.put(value);
      }
      final JSONObject properties = new JSONObject();
      properties.put(property, append_values);
      trackEvent(EventType.PROFILE_APPEND, null, properties, null);
    } catch (final JSONException e) {
      throw new InvalidDataException("Unexpected property name or value");
    }
  }

  /**
   * 删除用户的一个Profile
   *
   * @param property 属性名称
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称不符合规范时抛出异常
   */
  public void profileUnset(String property) throws InvalidDataException {
    try {
      trackEvent(EventType.PROFILE_UNSET, null, new JSONObject().put(property, true), null);
    } catch (final JSONException e) {
      throw new InvalidDataException("Unexpected property name");
    }
  }

  /**
   * 删除用户所有Profile
   *
   * @throws com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException
   * 当属性名称不符合规范时抛出异常
   */
  public void profileDelete() throws InvalidDataException {
    trackEvent(EventType.PROFILE_DELETE, null, null, null);
  }

  /**
   * 清除本地所有用户、事件相关信息
   */
  public void reset() {
    mPersistentIdentity.clearPreferences();
  }

  boolean isDebugMode() {
    return mDebugMode.isDebugMode();
  }

  boolean isDebugWriteData() {
    return mDebugMode.isDebugWriteData();
  }

  String getServerUrl() {
    return mServerUrl;
  }

  String getConfigureUrl() {
    return mConfigureUrl;
  }

  String getVTrackServerUrl() {
    return mVTrackServerUrl;
  }

  // Conveniences for testing.

  PersistentIdentity getPersistentIdentity(final Context context) {
    final SharedPreferencesLoader.OnPrefsLoadedListener listener =
        new SharedPreferencesLoader.OnPrefsLoadedListener() {
          @Override public void onPrefsLoaded(SharedPreferences preferences) {
          }
        };

    final String prefsName = "com.sensorsdata.analytics.android.sdk.SensorsDataAPI";
    final Future<SharedPreferences> storedPreferences =
        sPrefsLoader.loadPreferences(context, prefsName, listener);
    return new PersistentIdentity(storedPreferences);
  }

  private void trackEvent(EventType eventType, String eventName, JSONObject properties, String
      originalDistinctId) throws InvalidDataException {
    if (eventType.isTrack()) {
      assertKey(eventName);
    }
    assertPropertyTypes(eventType, properties);

    synchronized (mPersistentIdentity) {
      try {
        JSONObject sendProperties = null;

        if (eventType.isTrack()) {
          sendProperties = new JSONObject(mDeviceInfo);
          mPersistentIdentity.addSuperPropertiesToObject(sendProperties);
          // 当前网络状况
          sendProperties.put("$wifi", mMessages.isWifi());
        } else if (eventType.isProfile()) {
          sendProperties = new JSONObject();
        } else {
          return;
        }

        if (null != properties) {
          final Iterator<?> propIter = properties.keys();
          while (propIter.hasNext()) {
            final String key = (String) propIter.next();
            final Object value = properties.get(key);

            if (value instanceof Date) {
              sendProperties.put(key, mDateFormat.format((Date)value));
            } else {
              sendProperties.put(key, value);
            }
          }
        }

        final JSONObject dataObj = new JSONObject();

        if (eventType == EventType.TRACK) {
          dataObj.put("time", System.currentTimeMillis());
          dataObj.put("type", eventType.getEventType());
          dataObj.put("event", eventName);
          dataObj.put("properties", sendProperties);
          dataObj.put("distinct_id", mPersistentIdentity.getDistinctId());
        } else if (eventType == EventType.TRACK_SIGNUP) {
          dataObj.put("time", System.currentTimeMillis());
          dataObj.put("type", eventType.getEventType());
          dataObj.put("event", eventName);
          dataObj.put("properties", sendProperties);
          dataObj.put("distinct_id", mPersistentIdentity.getDistinctId());
          dataObj.put("original_id", originalDistinctId);
        } else {
          // is PROFILE_XXX
          dataObj.put("time", System.currentTimeMillis());
          dataObj.put("type", eventType.getEventType());
          dataObj.put("properties", sendProperties);
          dataObj.put("distinct_id", mPersistentIdentity.getDistinctId());
        }

        // $binding_depolyed为true或者无该属性时，isDepolyed为true
        final boolean isDepolyed = sendProperties.optBoolean("$binding_depolyed", true);

        // 若$binding_depolyed为true，则删除这些属性
        if (sendProperties.has("$binding_depolyed")) {
          // 可视化埋点的事件
          if (mVTrack instanceof  DebugTracking) {
            // Deep clone the event
            JSONObject debugDataObj = new JSONObject(dataObj.toString());
            ((DebugTracking) mVTrack).reportTrack(debugDataObj);
          }

          sendProperties.remove("$binding_path");
          sendProperties.remove("$binding_depolyed");
          sendProperties.remove("$binding_trigger_id");

          dataObj.put("properties", sendProperties);
        }

        if (isDepolyed) {
          mMessages.enqueueEventMessage(dataObj);

          if (mDebugMode.isDebugMode()) {
            // 同步发送
            mMessages.flushMessage(0);
          } else {
            // 异步延迟发送
            mMessages.flushMessage(mFlushInterval);
          }
        }
      } catch (JSONException e) {
        throw new InvalidDataException("Unexpteced property");
      }
    }
  }

  private void assertPropertyTypes(EventType eventType, JSONObject properties) throws
      InvalidDataException {
    if (properties == null) {
      return;
    }

    for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
      String key = (String) iterator.next();

      // Check Keys
      assertKey(key);

      try {
        Object value = properties.get(key);

        if (!(value instanceof String || value instanceof Number || value
            instanceof JSONArray || value instanceof Boolean || value instanceof Date)) {
          throw new InvalidDataException("The property value must be an instance of "
              + "String/Number/Boolean/JSONArray. [key='" + key + "', value='" + value.toString()
              + "']");
        }

        if (value instanceof String && !key.startsWith("$") && ((String) value).length() > 255) {
          throw new InvalidDataException("The property value is too long. [key='" + key
              + "', value='" + value.toString() + "']");
        }
      } catch (JSONException e) {
        throw new InvalidDataException("Unexpected property key. [key='" + key + "']");
      }
    }
  }

  private void assertKey(String key) throws InvalidDataException {
    if (null == key || key.length() < 1) {
      throw new InvalidDataException("The key is empty.");
    }
    if (!(KEY_PATTERN.matcher(key).matches())) {
      throw new InvalidDataException("The key '" + key + "' is invalid.");
    }
  }

  private void assertDistinctId(String key) throws InvalidDataException {
    if (key == null || key.length() < 1) {
      throw new InvalidDataException("The distinct_id or original_id is empty.");
    }
    if (key.length() > 255) {
      throw new InvalidDataException("The max length of distinct_id or original_id is 255.");
    }
  }

  private class VTrackUnsupported implements VTrack, DebugTracking {

    public VTrackUnsupported() {
    }

    @Override
    public void startUpdates() {
      // do NOTHING
    }

    @Override
    public void setEventBindings(JSONArray bindings) {
      // do NOTHING
    }

    @Override
    public void reportTrack(JSONObject eventJson) {
      // do NOTHING
    }
  }

  private enum EventType {
    TRACK("track", true, false),
    TRACK_SIGNUP("track_signup", true, false),
    PROFILE_SET("profile_set", false, true),
    PROFILE_SET_ONCE("profile_set_once", false, true),
    PROFILE_UNSET("profile_unset", false, true),
    PROFILE_INCREMENT("profile_increment", false, true),
    PROFILE_APPEND("profile_append", false, true),
    PROFILE_DELETE("profile_delete", false, true),
    REGISTER_SUPER_PROPERTIES("register_super_properties", false, false);

    EventType(String eventType, boolean isTrack, boolean isProfile) {
      this.eventType = eventType;
      this.track = isTrack;
      this.profile = isProfile;
    }

    public String getEventType() {
      return eventType;
    }

    public boolean isTrack() {
      return track;
    }

    public boolean isProfile() {
      return profile;
    }

    private String eventType;
    private boolean track;
    private boolean profile;
  }


  // 可视化埋点功能最低API版本
  static final int VTRACK_SUPPORTED_MIN_API = 16;

  // SDK版本
  static final String VERSION = "1.3.6";

  private static final Pattern KEY_PATTERN = Pattern.compile(
      "^((?!^distinct_id$|^original_id$|^time$|^properties$|^id$|^first_id$|^second_id$|^users$|^events$|^event$|^user_id$|^date$|^datetime$)[a-zA-Z_$][a-zA-Z\\d_$]{0,99})$",
      Pattern.CASE_INSENSITIVE);

  // Maps each token to a singleton SensorsDataAPI instance
  private static final Map<Context, SensorsDataAPI> sInstanceMap =
      new HashMap<Context, SensorsDataAPI>();
  private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();

  // Configures
  /* SensorsAnalytics 地址 */
  private final String mServerUrl;
  /* 可视化埋点配置地址 */
  private final String mConfigureUrl;
  /* 可视化埋点WebServer地址 */
  private final String mVTrackServerUrl;
  /* Debug模式选项 */
  private final DebugMode mDebugMode;
  /* Flush时间间隔 */
  private int mFlushInterval;

  private final Context mContext;
  private final AnalyticsMessages mMessages;
  private final PersistentIdentity mPersistentIdentity;
  private final Map<String, Object> mDeviceInfo;

  private final VTrack mVTrack;

  private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"
      + ".SSS");

  private static final String LOGTAG = "SA.SensorsDataAPI";
}
