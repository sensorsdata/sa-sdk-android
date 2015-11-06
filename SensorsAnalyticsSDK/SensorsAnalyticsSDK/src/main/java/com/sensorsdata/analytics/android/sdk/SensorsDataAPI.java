package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class SensorsDataAPI {

  SensorsDataAPI(Context context, Future<SharedPreferences> referrerPreferences, String token)
      throws SensorsDataException {
    mContext = context;
    mToken = token;
    mEventTimings = new HashMap<String, Long>();

    final Map<String, String> deviceInfo = new HashMap<String, String>();
    deviceInfo.put("$android_lib_version", SSConfig.VERSION);
    deviceInfo.put("$android_os", "Android");
    deviceInfo.put("$android_os_version",
        Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
    deviceInfo
        .put("$android_manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
    deviceInfo.put("$android_brand", Build.BRAND == null ? "UNKNOWN" : Build.BRAND);
    deviceInfo.put("$android_model", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
    try {
      final PackageManager manager = mContext.getPackageManager();
      final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
      deviceInfo.put("$android_app_version", info.versionName);
      deviceInfo.put("$android_app_version_code", Integer.toString(info.versionCode));
    } catch (final PackageManager.NameNotFoundException e) {
      Log.e(LOGTAG, "Exception getting app version name", e);
    }
    mDeviceInfo = Collections.unmodifiableMap(deviceInfo);

    mPersistentIdentity = getPersistentIdentity(context, referrerPreferences, token);

    mMessages = AnalyticsMessages.getInstance(mContext);

    track("$AppStart", null);
  }

  /**
   * 根据传入的Token，获取SensorsDataAPI单例
   *
   * @param context The application context you are tracking
   * @param token   项目Token，请在SensorsDataAnalytics管理界面申请
   *
   * @return an instance of SensorsDataAPI associated with your project
   */
  public static SensorsDataAPI getInstance(Context context, String token)
      throws SensorsDataException {
    if (null == token || null == context) {
      return null;
    }
    synchronized (sInstanceMap) {
      final Context appContext = context.getApplicationContext();

      if (null == sReferrerPrefs) {
        sReferrerPrefs = sPrefsLoader.loadPreferences(context, SSConfig.REFERRER_PREFS_NAME, null);
      }

      Map<Context, SensorsDataAPI> instances = sInstanceMap.get(token);
      if (null == instances) {
        instances = new HashMap<Context, SensorsDataAPI>();
        sInstanceMap.put(token, instances);
      }

      SensorsDataAPI instance = instances.get(appContext);
      if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
        instance = new SensorsDataAPI(appContext, sReferrerPrefs, token);
        instances.put(appContext, instance);
      }

      return instance;
    }
  }

  /**
   * 设置当前用户的distinctId。一般情况下，如果是一个注册用户，则应该使用注册系统内
   * 的user_id，如果是个未注册用户，则可以选择一个不会重复的匿名ID，如设备ID等，如果
   * 客户没有设置indentify，则使用SDK自动生成的匿名ID
   *
   * @param distinctId 当前用户的distinctId，仅接受数字、下划线和大小写字母
   */
  public void identify(String distinctId) throws SensorsDataException {
    checkKey(distinctId);

    synchronized (mPersistentIdentity) {
      mPersistentIdentity.setEventsDistinctId(distinctId);
      mPersistentIdentity.setPeopleDistinctId(distinctId);
    }

    pushWaitingPeopleRecord();
  }

  /**
   * 提供一个接口，用来在用户注册的时候，用注册ID来替换用户以前的匿名ID。此接口当且仅当用户正式注册时使用。
   *
   * @param newDistinctId 用户完成注册后生成的注册ID
   * @param properties    事件的属性
   */
  public void trackSignUp(String newDistinctId, JSONObject properties) throws SensorsDataException {
    checkKey(newDistinctId);
    checkKeyInProperties(properties);

    String oldDistinctId = getDistinctId();
    identify(newDistinctId);

    try {
      final JSONObject messageProps = new JSONObject();

      final Map<String, String> referrerProperties = mPersistentIdentity.getReferrerProperties();
      for (final Map.Entry<String, String> entry : referrerProperties.entrySet()) {
        final String key = entry.getKey();
        final String value = entry.getValue();
        messageProps.put(key, value);
      }

      mPersistentIdentity.addSuperPropertiesToObject(messageProps);

      if (null != properties) {
        final Iterator<?> propIter = properties.keys();
        while (propIter.hasNext()) {
          final String key = (String) propIter.next();
          messageProps.put(key, properties.get(key));
        }
      }

      final AnalyticsMessages.EventDescription eventDescription =
          new AnalyticsMessages.EventDescription(
              "track_signup",
              null,
              (long) System.currentTimeMillis(),
              newDistinctId,
              oldDistinctId,
              messageProps,
              mToken);
      mMessages.eventsMessage(eventDescription);

    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception tracking signing up", e);
      throw new SensorsDataException(e);
    }

    flush();
  }

  /**
   * 与 {@link #trackSignUp(String, org.json.JSONObject)} 类似，无事件属性
   *
   * @param newDistinctId 用户完成注册后生成的注册ID
   */
  public void trackSignUp(String newDistinctId) throws SensorsDataException {
    trackSignUp(newDistinctId, null);
  }

  /**
   * 调用track接口，追踪一个带有属性的事件
   *
   * @param eventName  事件的名称
   * @param properties 事件的属性
   */
  public void track(String eventName, JSONObject properties) throws SensorsDataException {
    checkKey(eventName);
    checkKeyInProperties(properties);

    final Long eventBegin;
    synchronized (mEventTimings) {
      eventBegin = mEventTimings.get(eventName);
      mEventTimings.remove(eventName);
    }

    try {
      final JSONObject messageProps = new JSONObject();

      final Map<String, String> referrerProperties = mPersistentIdentity.getReferrerProperties();
      for (final Map.Entry<String, String> entry : referrerProperties.entrySet()) {
        final String key = entry.getKey();
        final String value = entry.getValue();
        messageProps.put(key, value);
      }

      mPersistentIdentity.addSuperPropertiesToObject(messageProps);

      final double timeSecondsDouble = System.currentTimeMillis();

      if (null != eventBegin) {
        final double eventBeginDouble = ((double) eventBegin) / 1000.0;
        final double secondsElapsed = timeSecondsDouble - eventBeginDouble;
        messageProps.put("$duration", secondsElapsed);
      }

      if (null != properties) {
        final Iterator<?> propIter = properties.keys();
        while (propIter.hasNext()) {
          final String key = (String) propIter.next();
          if (key.length() > 0) {
            messageProps.put(key, properties.get(key));
          }
        }
      }

      final AnalyticsMessages.EventDescription eventDescription =
          new AnalyticsMessages.EventDescription(
              "track",
              eventName,
              (long) timeSecondsDouble,
              getDistinctId(),
              null,
              messageProps,
              mToken);
      mMessages.eventsMessage(eventDescription);
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception tracking event " + eventName, e);
      throw new SensorsDataException(e);
    }
  }

  /**
   * 与 {@link #track(String, org.json.JSONObject)} 类似，无事件属性
   *
   * @param eventName 事件的名称
   */
  public void track(String eventName) throws SensorsDataException {
    track(eventName, null);
  }

  /**
   * 将所有本地缓存的日志发送到SensorsData Analytics.
   */
  public void flush() {
    mMessages.postToServer();
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
   * 获取当前用户的distinctId
   *
   * @return 当前用户的distinctId
   */
  public String getDistinctId() {
    return mPersistentIdentity.getEventsDistinctId();
  }

  /**
   * 注册所有事件都有的公共属性
   *
   * @param superProperties 事件公共属性
   */
  public void registerSuperProperties(JSONObject superProperties) throws SensorsDataException {
    checkKeyInProperties(superProperties);
    mPersistentIdentity.registerSuperProperties(superProperties);
  }

  /**
   * 删除事件公共属性
   *
   * @param superPropertyName 事件属性名称
   */
  public void unregisterSuperProperty(String superPropertyName) throws SensorsDataException {
    checkKey(superPropertyName);
    mPersistentIdentity.unregisterSuperProperty(superPropertyName);
  }

  /**
   * 删除所有事件公共属性
   */
  public void clearSuperProperties() {
    mPersistentIdentity.clearSuperProperties();
  }

  /**
   * 设置用户的一个或多个Profile。
   * Profile如果存在，则覆盖；否则，新创建。
   *
   * @param properties   属性列表
   */
  public void profileSet(JSONObject properties) throws SensorsDataException {
    checkKeyInProperties(properties);

    try {
      final JSONObject sendProperties = new JSONObject(mDeviceInfo);
      for (final Iterator<?> iter = properties.keys(); iter.hasNext(); ) {
        final String key = (String) iter.next();
        sendProperties.put(key, properties.get(key));
      }

      final JSONObject message = stdPeopleMessage("profile_set", sendProperties);
      recordPeopleMessage(message);
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception setting people properties", e);
      throw new SensorsDataException(e);
    }
  }

  /**
   * 首次设置用户的一个或多个Profile。
   * 与profileSet接口不同的是，Profile如果存在，则覆盖；否则，新创建。
   *
   * @param properties   属性列表
   */
  public void profileSetOnce(JSONObject properties) throws SensorsDataException {
    checkKeyInProperties(properties);

    try {
      final JSONObject sendProperties = new JSONObject(mDeviceInfo);
      for (final Iterator<?> iter = properties.keys(); iter.hasNext(); ) {
        final String key = (String) iter.next();
        sendProperties.put(key, properties.get(key));
      }

      final JSONObject message = stdPeopleMessage("profile_set_once", sendProperties);
      recordPeopleMessage(message);
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception setting people properties", e);
      throw new SensorsDataException(e);
    }
  }

  /**
   * 设置用户的一个Profile，如果之前存在，则覆盖，否则，新创建
   *
   * @param property     属性名称
   * @param value        属性的值，值的类型只允许为String, Number, Date或List<?>
   */
  public void profileSet(String property, Object value) throws SensorsDataException {
    try {
      profileSet(new JSONObject().put(property, value));
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception set properties", e);
      throw new SensorsDataException(e);
    }
  }

  /**
   * 首次设置用户的一个Profile
   * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
   *
   * @param property     属性名称
   * @param value        属性的值，值的类型只允许为String, Number, Date或List<?>
   */
  public void profileSetOnce(String property, Object value) throws SensorsDataException {
    try {
      profileSetOnce(new JSONObject().put(property, value));
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception set properties", e);
      throw new SensorsDataException(e);
    }
  }


  /**
   * 给一个或多个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
   * 未设置，则添加属性并设置默认值为0
   *
   * @param properties 一个或多个属性集合
   */
  public void profileIncrement(Map<String, ? extends Number> properties)
      throws SensorsDataException {
    final JSONObject json = new JSONObject(properties);
    checkKeyInProperties(json);
    try {
      final JSONObject message = stdPeopleMessage("profile_increment", json);
      recordPeopleMessage(message);
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception incrementing properties", e);
      throw new SensorsDataException(e);
    }
  }

  /**
   * 给一个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
   * 未设置，则添加属性并设置默认值为0
   *
   * @param property  属性名称
   * @param value     属性的值
   */
  public void profileIncrement(String property, Number value) throws SensorsDataException {
    final Map<String, Number> map = new HashMap<String, Number>();
    map.put(property, value);
    profileIncrement(map);
  }

  /**
   * 给一个列表类型的Profile增加一个元素
   *
   * @param property  属性名称
   * @param value     新增的元素
   */
  public void profileAppend(String property, Object value) throws SensorsDataException {
    checkKey(property);
    try {
      final JSONObject properties = new JSONObject();
      properties.put(property, value);
      final JSONObject message = stdPeopleMessage("profile_append", properties);
      recordPeopleMessage(message);
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception appending a property", e);
      throw new SensorsDataException(e);
    }
  }

  /**
   * 删除用户的一个Profile
   *
   * @param property  属性名称
   */
  public void profileUnset(String property) throws SensorsDataException {
    checkKey(property);
    try {
      final JSONArray names = new JSONArray();
      names.put(property);
      final JSONObject message = stdPeopleMessage("profile_unset", names);
      recordPeopleMessage(message);
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception unsetting a property", e);
      throw new SensorsDataException(e);
    }
  }

  /**
   * 删除用户所有Profile
   */
  public void delete() throws SensorsDataException {
    try {
      final JSONObject message = stdPeopleMessage("delete", JSONObject.NULL);
      recordPeopleMessage(message);
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception deleting a user");
      throw new SensorsDataException(e);
    }
  }

  /**
   * 清除本地所有用户、事件相关信息
   */
  public void reset() {
    mPersistentIdentity.clearPreferences();
  }

  interface InstanceProcessor {
    public void process(SensorsDataAPI m);
  }

  static void allInstances(InstanceProcessor processor) {
    synchronized (sInstanceMap) {
      for (final Map<Context, SensorsDataAPI> contextInstances : sInstanceMap.values()) {
        for (final SensorsDataAPI instance : contextInstances.values()) {
          processor.process(instance);
        }
      }
    }
  }

  // Conveniences for testing.

  PersistentIdentity getPersistentIdentity(final Context context,
      Future<SharedPreferences> referrerPreferences, final String token) {
    final SharedPreferencesLoader.OnPrefsLoadedListener listener =
        new SharedPreferencesLoader.OnPrefsLoadedListener() {
          @Override public void onPrefsLoaded(SharedPreferences preferences) {
            final JSONArray records =
                PersistentIdentity.waitingPeopleRecordsForSending(preferences);
            if (null != records) {
              sendAllPeopleRecords(records);
            }
          }
        };

    final String prefsName = "com.sensorsdata.analytics.android.sdk.SensorsDataAPI_" + token;
    final Future<SharedPreferences> storedPreferences =
        sPrefsLoader.loadPreferences(context, prefsName, listener);
    return new PersistentIdentity(referrerPreferences, storedPreferences);
  }

  ///////////////////////


    private JSONObject stdPeopleMessage(String actionType, Object properties) throws JSONException {
      final JSONObject dataObj = new JSONObject();
      final String distinctId = getDistinctId(); // TODO ensure getDistinctId is thread safe

      dataObj.put("token", mToken);
      dataObj.put("time", System.currentTimeMillis());
      dataObj.put("type", actionType);
      dataObj.put("properties", properties);

      if (null != distinctId) {
        dataObj.put("distinct_id", distinctId);
      }

      return dataObj;
    }

  ////////////////////////////////////////////////////

  private void recordPeopleMessage(JSONObject message) {
    if (message.has("distinct_id")) {
      mMessages.peopleMessage(message);
    } else {
      mPersistentIdentity.storeWaitingPeopleRecord(message);
    }
  }

  private void pushWaitingPeopleRecord() throws SensorsDataException {
    final JSONArray records = mPersistentIdentity.waitingPeopleRecordsForSending();
    if (null != records) {
      sendAllPeopleRecords(records);
    }
  }

  private void sendAllPeopleRecords(JSONArray records) {
    for (int i = 0; i < records.length(); i++) {
      try {
        final JSONObject message = records.getJSONObject(i);
        mMessages.peopleMessage(message);
      } catch (final JSONException e) {
        Log.e(LOGTAG, "Malformed people record stored pending identity, will not send it.", e);
      }
    }
  }

  private void checkKey(String key) throws SensorsDataException {
    if (key == null || key.length() < 1) {
      throw new SensorsDataException("The key is empty.");
    }

    if (!(KEY_PATTERN.matcher(key).matches())) {
      throw new SensorsDataException("The key '" + key + "' is invalid.");
    }
  }

  private void checkKeyInProperties(JSONObject properties) throws SensorsDataException {
    if (properties == null) {
      return;
    }
    for (Iterator<String> iter = properties.keys(); iter.hasNext();) {
      String key = iter.next();
      checkKey(key);
    }
  }

  private final Context mContext;
  private final AnalyticsMessages mMessages;
  private final String mToken;
  private final PersistentIdentity mPersistentIdentity;
  private final Map<String, String> mDeviceInfo;
  private final Map<String, Long> mEventTimings;

  // Maps each token to a singleton SensorsDataAPI instance
  private static final Map<String, Map<Context, SensorsDataAPI>> sInstanceMap =
      new HashMap<String, Map<Context, SensorsDataAPI>>();
  private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
  private static Future<SharedPreferences> sReferrerPrefs;

  private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

  private static final String LOGTAG = "SA.API";
}
