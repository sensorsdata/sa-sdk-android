package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class SensorsDataAPI {

  SensorsDataAPI(Context context, Future<SharedPreferences> referrerPreferences)
      throws SensorsDataException {
    mContext = context;

    final Map<String, Object> deviceInfo = new HashMap<String, Object>();
    deviceInfo.put("$lib", "Android");
    deviceInfo.put("$lib_version", SSConfig.VERSION);
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
    deviceInfo.put("$screen_height", Integer.valueOf(displayMetrics.heightPixels));
    deviceInfo.put("$screen_width", Integer.valueOf(displayMetrics.widthPixels));
    mDeviceInfo = Collections.unmodifiableMap(deviceInfo);

    mPersistentIdentity = getPersistentIdentity(context, referrerPreferences);

    mMessages = AnalyticsMessages.getInstance(mContext);

    track("$AppStart", null);
  }

  /**
   * 根据传入的Token，获取SensorsDataAPI单例
   *
   * @param context The application context you are tracking
   *
   * @return an instance of SensorsDataAPI associated with your project
   */
  public static SensorsDataAPI getInstance(Context context)
      throws SensorsDataException {
    if (null == context) {
      return null;
    }
    synchronized (sInstanceMap) {
      final Context appContext = context.getApplicationContext();

      if (null == sReferrerPrefs) {
        sReferrerPrefs = sPrefsLoader.loadPreferences(context, SSConfig.REFERRER_PREFS_NAME, null);
      }

      SensorsDataAPI instance = sInstanceMap.get(appContext);
      if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
        instance = new SensorsDataAPI(appContext, sReferrerPrefs);
        sInstanceMap.put(appContext, instance);
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
    checkDistinctId(distinctId);

    synchronized (mPersistentIdentity) {
      mPersistentIdentity.setEventsDistinctId(distinctId);
    }
  }

  /**
   * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:http://www.sensorsdata.cn/manual/track_signup.html，并在必要时联系我们的技术支持人员。
   *
   * @param newDistinctId 用户完成注册后生成的注册ID
   * @param properties    事件的属性
   */
  public void trackSignUp(String newDistinctId, JSONObject properties) throws SensorsDataException {
    checkDistinctId(newDistinctId);
    checkKeyInProperties(properties);

    String oldDistinctId = getDistinctId();
    identify(newDistinctId);

    try {
      final JSONObject sendProperties = new JSONObject(mDeviceInfo);

      final Map<String, String> referrerProperties = mPersistentIdentity.getReferrerProperties();
      for (final Map.Entry<String, String> entry : referrerProperties.entrySet()) {
        final String key = entry.getKey();
        final String value = entry.getValue();
        sendProperties.put(key, value);
      }

      mPersistentIdentity.addSuperPropertiesToObject(sendProperties);

      if (null != properties) {
        final Iterator<?> propIter = properties.keys();
        while (propIter.hasNext()) {
          final String key = (String) propIter.next();
          sendProperties.put(key, properties.get(key));
        }
      }

      final JSONObject dataObj = new JSONObject();

      dataObj.put("time", System.currentTimeMillis());
      dataObj.put("type", "track_signup");
      dataObj.put("event", "$SignUp");
      dataObj.put("properties", sendProperties);
      dataObj.put("distinct_id", newDistinctId);
      dataObj.put("original_id", oldDistinctId);
      mMessages.eventsMessage(dataObj);

    } catch (final JSONException e) {
      Log.e(LOGTAG, "Exception tracking signing up", e);
      throw new SensorsDataException(e);
    }

    flush();
  }

  /**
   * 与 {@link #trackSignUp(String, org.json.JSONObject)} 类似，无事件属性
   * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:http://www.sensorsdata.cn/manual/track_signup.html，并在必要时联系我们的技术支持人员。
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

    try {
      final JSONObject sendProperties = new JSONObject(mDeviceInfo);

      final Map<String, String> referrerProperties = mPersistentIdentity.getReferrerProperties();
      for (final Map.Entry<String, String> entry : referrerProperties.entrySet()) {
        final String key = entry.getKey();
        final String value = entry.getValue();
        sendProperties.put(key, value);
      }

      mPersistentIdentity.addSuperPropertiesToObject(sendProperties);

      if (null != properties) {
        final Iterator<?> propIter = properties.keys();
        while (propIter.hasNext()) {
          final String key = (String) propIter.next();
          sendProperties.put(key, properties.get(key));
        }
      }

      final JSONObject dataObj = new JSONObject();
      final String distinctId = getDistinctId();

      dataObj.put("time", System.currentTimeMillis());
      dataObj.put("type", "track");
      dataObj.put("event", eventName);
      dataObj.put("properties", sendProperties);
      dataObj.put("distinct_id", distinctId);

      mMessages.eventsMessage(dataObj);
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
      final JSONObject message = stdPeopleMessage("profile_set", properties);
      mMessages.peopleMessage(message);
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
      final JSONObject message = stdPeopleMessage("profile_set_once", properties);
      mMessages.peopleMessage(message);
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
      mMessages.peopleMessage(message);
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
      mMessages.peopleMessage(message);
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
      final JSONObject names = new JSONObject();
      names.put(property, true);
      final JSONObject message = stdPeopleMessage("profile_unset", names);
      mMessages.peopleMessage(message);
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
      final JSONObject message = stdPeopleMessage("delete", null);
      mMessages.peopleMessage(message);
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
      for (final SensorsDataAPI instance : sInstanceMap.values()) {
        processor.process(instance);
      }
    }
  }

  // Conveniences for testing.

  PersistentIdentity getPersistentIdentity(final Context context,
      Future<SharedPreferences> referrerPreferences) {
    final SharedPreferencesLoader.OnPrefsLoadedListener listener =
        new SharedPreferencesLoader.OnPrefsLoadedListener() {
          @Override public void onPrefsLoaded(SharedPreferences preferences) {
          }
        };

    final String prefsName = "com.sensorsdata.analytics.android.sdk.SensorsDataAPI";
    final Future<SharedPreferences> storedPreferences =
        sPrefsLoader.loadPreferences(context, prefsName, listener);
    return new PersistentIdentity(referrerPreferences, storedPreferences);
  }

  private JSONObject stdPeopleMessage(String actionType, JSONObject properties) throws
      JSONException {
    final JSONObject dataObj = new JSONObject();
    dataObj.put("time", System.currentTimeMillis());
    dataObj.put("type", actionType);
    dataObj.put("properties", properties);
    dataObj.put("distinct_id", getDistinctId());

    return dataObj;
  }

  private void checkKey(String key) throws SensorsDataException {
    if (key == null || key.length() < 1) {
      throw new SensorsDataException("The key is empty.");
    }
    if (!(KEY_PATTERN.matcher(key).matches())) {
      throw new SensorsDataException("The key '" + key + "' is invalid.");
    }
  }

  private void checkDistinctId(String key) throws SensorsDataException {
    if (key == null || key.length() < 1) {
      throw new SensorsDataException("The distinct_id or original_id is empty.");
    }
    if (key.length() > 255) {
      throw new SensorsDataException("The max_length of distinct_id or original_id is 255.");
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
  private final PersistentIdentity mPersistentIdentity;
  private final Map<String, Object> mDeviceInfo;

  // Maps each token to a singleton SensorsDataAPI instance
  private static final Map<Context, SensorsDataAPI> sInstanceMap =
      new HashMap<Context, SensorsDataAPI>();
  private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
  private static Future<SharedPreferences> sReferrerPrefs;

  private static final Pattern KEY_PATTERN = Pattern.compile("^((?!^distinct_id$|^original_id$|^time$|^properties$|^id$|^first_id$|^second_id$|^users$|^events$|^event$|^user_id$|^date$|^datetime$)[a-zA-Z_$][a-zA-Z\\d_$]{0,99})$", Pattern.CASE_INSENSITIVE);

  private static final String LOGTAG = "SA.API";
}
